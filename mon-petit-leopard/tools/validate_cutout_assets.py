#!/usr/bin/env python3
"""Regression checks for sprite cutouts and preservation of natural edge colours."""

from __future__ import annotations

import importlib.util
import io
import hashlib
import json
from collections import deque
from pathlib import Path
import sys

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
FRAME = 256
ANNOTATIONS = ROOT / "source-assets/cutout-v087"
MANIFEST = ROOT / "cutout-audit-manifest.json"


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def load_sprite_importer(filename="prepare_v061_sprite_assets.py", name="cutout_sprite_importer"):
    # Import production helpers without running the asset-generation entrypoint
    # or creating bytecode files in the project during a read-only validation.
    path = ROOT / "tools" / filename
    spec = importlib.util.spec_from_file_location(name, path)
    check(spec is not None and spec.loader is not None, f"Cannot import {path}")
    module = importlib.util.module_from_spec(spec)
    previous = sys.dont_write_bytecode
    try:
        sys.dont_write_bytecode = True
        spec.loader.exec_module(module)
    finally:
        sys.dont_write_bytecode = previous
    return module


def verify_alpha_cleanup(cleanup, original, label):
    """Check the colour/opacity contract against every original RGBA pixel."""
    before = np.array(original.convert("RGBA"))
    source_bytes = original.tobytes()
    cleaned = cleanup(original)
    check(cleaned.mode == "RGBA" and cleaned.size == original.size,
          f"{label}: alpha cleanup must preserve RGBA mode and canvas")
    check(original.tobytes() == source_bytes, f"{label}: cleanup mutated its source image")
    after = np.array(cleaned)
    alpha = before[:, :, 3]
    visible = alpha > 2
    intermediate = (alpha >= 3) & (alpha <= 249)
    negligible = alpha <= 2
    near_opaque = alpha >= 250

    check(np.array_equal(after[:, :, :3][visible], before[:, :, :3][visible]),
          f"{label}: RGB changed on a visible pixel; black/white edges must not be repainted")
    check(np.array_equal(after[:, :, 3][intermediate], alpha[intermediate]),
          f"{label}: genuine intermediate opacity was changed")
    check(np.all(after[negligible] == 0),
          f"{label}: nearly transparent pixels must have zero RGB and alpha")
    check(np.all(after[:, :, 3][near_opaque] == 255),
          f"{label}: nearly opaque pixels must be normalized to alpha 255")
    check(np.array_equal(after[:, :, 3] > 0, visible),
          f"{label}: cleanup expanded the silhouette or erased a visible detail")
    check(np.array_equal(np.array(cleanup(cleaned)), after),
          f"{label}: repeated cleanup must not progressively change the drawing")
    return int(visible.sum()), int(intermediate.sum())


def synthetic_edges():
    """Put genuine dark/white/fur edges next to opaque contrasting colours."""
    alphas = (0, 1, 2, 3, 20, 64, 127, 128, 200, 234, 235, 249, 250, 251, 254, 255)
    colours = ((9, 9, 9), (246, 245, 244), (231, 166, 51),
               (191, 73, 99), (110, 111, 110), (0, 0, 0))
    arr = np.zeros((len(colours) * 3, len(alphas) * 3, 4), dtype=np.uint8)
    for row, colour in enumerate(colours):
        # The old algorithm replaced partially transparent black with the light
        # neighbours, and natural white with the dark neighbours.
        neighbour = (215, 216, 215) if max(colour) < 100 else (22, 23, 22)
        for column, alpha in enumerate(alphas):
            y, x = row * 3, column * 3
            arr[y:y + 3, x:x + 3] = (*neighbour, 255)
            arr[y + 1, x + 1] = (*colour, alpha)
    return Image.fromarray(arr, "RGBA")


def validate_alpha_cleanup():
    importer = load_sprite_importer()
    cleanup = importer._decontaminate_alpha_edges
    verify_alpha_cleanup(cleanup, synthetic_edges(), "contrasting natural edge colours")

    visible_total = 0
    intermediate_total = 0
    with importer.load_bundle() as bundle:
        with Image.open(io.BytesIO(bundle.read("old/leopard_old_atlas.webp"))) as source:
            atlas = source.convert("RGBA")
        check(atlas.size == (importer.FRAME * importer.COLS, importer.FRAME * importer.ROWS),
              "Original old leopard atlas has unexpected dimensions")
        for action in ("idle_down", "idle_left", "idle_right", "idle_up"):
            index = importer.OFFSETS[action]
            x = index % importer.COLS * importer.FRAME
            y = index // importer.COLS * importer.FRAME
            original = atlas.crop((x, y, x + importer.FRAME, y + importer.FRAME))
            before = np.array(original)
            partial = (before[:, :, 3] > 2) & (before[:, :, 3] < 250)
            check(np.any(partial & (before[:, :, :3].max(axis=2) < 90)),
                  f"old/{action}: source must exercise real dark partial edges")
            check(np.any(partial & (before[:, :, :3].min(axis=2) >= 200)),
                  f"old/{action}: source must exercise real white partial details")
            visible, intermediate = verify_alpha_cleanup(cleanup, original, f"old/{action}")
            visible_total += visible
            intermediate_total += intermediate

    print("Alpha edge regression: PASS — contrasting black/white/fur samples and four original old leopard idles; "
          f"{visible_total} visible RGB pixels and {intermediate_total} intermediate alpha values preserved")


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def integer(value, label, low=0, high=FRAME * FRAME):
    check(type(value) is int and low <= value <= high,
          f"{label}: expected an integer from {low} to {high}, got {value}")
    return value


def point(value, label):
    check(isinstance(value, list) and len(value) == 2, f"{label}: expected [x, y]")
    return (integer(value[0], label + " x", high=FRAME - 1),
            integer(value[1], label + " y", high=FRAME - 1))


def region_bounds(region, label):
    sx, sy = point(region["seed"], label + " seed")
    box = region["box"]
    check(isinstance(box, list) and len(box) == 4,
          f"{label}: expected [left, top, right, bottom]")
    left, top, right, bottom = [integer(value, label + " bound", high=FRAME) for value in box]
    check(left <= sx < right and top <= sy < bottom, f"{label}: seed must lie inside its bounded region")
    return sx, sy, (right - left) * (bottom - top)


def protected_polygon_mask(polygons, label):
    """Rasterize reviewed polygons independently of the preparation helpers."""
    check(isinstance(polygons, list), f"{label}: protected polygons must be a list")
    image = Image.new("L", (FRAME, FRAME), 0)
    painter = ImageDraw.Draw(image)
    for index, polygon in enumerate(polygons, 1):
        check(isinstance(polygon, list) and len(polygon) >= 3,
              f"{label} polygon {index}: at least three vertices are required")
        vertices = [point(vertex, f"{label} polygon {index}") for vertex in polygon]
        painter.polygon(vertices, fill=255)
    return np.array(image) > 0


def sprite_resource_path(species, age, name):
    check(species in {"leopard", "wolf"} and age in {"cub", "teen", "adult", "old"},
          f"Unknown sprite species/age: {species}/{age}")
    check(isinstance(name, str) and Path(name).name == name
          and name.startswith(f"{species}_{age}_") and Path(name).suffix in {".png", ".webp"},
          f"Sprite resource does not belong to its declared species/age: {name}")
    directory = f"res-wolf-{age}" if species == "wolf" else f"res-{age}"
    return ROOT / "app/src/main" / directory / "drawable-nodpi" / name


def load_probe_frame(path, number, images):
    if path not in images:
        with Image.open(path) as source:
            source.load()
            images[path] = source.convert("RGBA")
    image = images[path]
    check(image.height == FRAME and image.width > 0 and image.width % FRAME == 0,
          f"{path.name}: probe resource has incompatible frame dimensions")
    number = integer(number, f"{path.name}: probe frame", low=1, high=image.width // FRAME)
    x = (number - 1) * FRAME
    return image.crop((x, 0, x + FRAME, FRAME))


def validate_generated_edge_probes():
    document = json.loads((ANNOTATIONS / "generated-edge-probes.json").read_bytes())
    check(document["schema_version"] == 1 and document["frame_size"] == FRAME,
          "Generated edge probes use an unsupported schema or frame size")
    probes = document["probes"]
    check(isinstance(probes, list) and probes, "Generated edge regression probes are missing")
    images = {}
    checked = set()
    for probe in probes:
        path = sprite_resource_path(probe["species"], probe["age"], probe["resource"])
        frame = load_probe_frame(path, probe["frame"], images)
        x, y = point(probe["point"], f"{path.name}: generated edge probe")
        key = (path, probe["frame"], x, y)
        check(key not in checked, f"{path.name}: duplicate generated edge probe")
        checked.add(key)
        expected = probe["rgba"]
        check(isinstance(expected, list) and len(expected) == 4,
              f"{path.name}: edge probe must provide all RGBA channels")
        for channel in expected:
            integer(channel, f"{path.name}: expected RGBA", high=255)
        check(expected[3] > 2, f"{path.name}: edge regression must target a visible detail")
        tolerance = integer(probe["tolerance"], f"{path.name}: channel tolerance", high=3)
        actual = np.array(frame.getpixel((x, y)), dtype=np.int16)
        delta = np.abs(actual - np.array(expected, dtype=np.int16))
        check(np.all(delta <= tolerance),
              f"{path.name} #{probe['frame']} at {(x, y)}: generated edge is stale or recoloured; "
              f"RGBA {actual.tolist()}, expected {expected} within {tolerance} per channel")
    print(f"Generated edge regression: PASS — {len(checked)} original-colour pixel probes "
          f"in {len(images)} runtime resources, maximum permitted RGBA channel tolerance 3")


def exterior_ring(alpha):
    """Find the one-pixel exterior ring without using production mask helpers."""
    occupied = alpha > 0
    height, width = occupied.shape
    exterior = np.zeros_like(occupied)
    queue = deque()
    for y, x in ([(0, x) for x in range(width)] + [(height - 1, x) for x in range(width)]
                 + [(y, 0) for y in range(height)] + [(y, width - 1) for y in range(height)]):
        if not occupied[y, x] and not exterior[y, x]:
            exterior[y, x] = True
            queue.append((y, x))
    while queue:
        y, x = queue.popleft()
        for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
            if (0 <= ny < height and 0 <= nx < width
                    and not occupied[ny, nx] and not exterior[ny, nx]):
                exterior[ny, nx] = True
                queue.append((ny, nx))
    adjacent = np.zeros_like(occupied)
    padded = np.pad(exterior, 1)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dy != 0 or dx != 0:
                adjacent |= padded[1 + dy:1 + dy + height, 1 + dx:1 + dx + width]
    return occupied & adjacent


def verify_wolf_unmatte_scope(unmatte, original, original_rgb, protected, label):
    before = np.array(original.convert("RGBA"))
    rgb_before = np.array(original_rgb).copy()
    protected_before = np.array(protected).copy()
    check(protected_before.shape == before.shape[:2] and protected_before.dtype == np.bool_,
          f"{label}: expected a boolean source protection mask")
    ring = exterior_ring(before[:, :, 3])
    permitted = ring & ~protected_before
    result = unmatte(original, original_rgb, protected)
    check(result.mode == "RGBA" and result.size == original.size, f"{label}: exterior cleanup changed the canvas")
    after = np.array(result)
    check(np.array_equal(after[~permitted], before[~permitted]),
          f"{label}: exterior cleanup modified interior fur, an enclosed edge or protected detail")
    check(np.array_equal(np.array(original), before)
          and np.array_equal(original_rgb, rgb_before) and np.array_equal(protected, protected_before),
          f"{label}: exterior cleanup mutated its source or protection mask")
    check(not np.any((before[:, :, 3] == 0) & (after[:, :, 3] > 0)),
          f"{label}: exterior cleanup painted new pixels outside the source silhouette")
    changed = int(np.any(before != after, axis=2).sum())
    preserved = int(((before[:, :, 3] > 0) & ~permitted).sum())
    return result, changed, preserved


def validate_wolf_edge_preservation():
    importer = load_sprite_importer("prepare_v086_wolf_assets.py", "cutout_wolf_importer")
    # A matted outer edge surrounds real black, white and grey interior details.
    # The enclosed transparent hole must not be confused with the outer border.
    height = width = 40
    yy, xx = np.indices((height, width))
    checker = np.where((xx // 3 + yy // 3) % 2 == 0, 225, 215).astype(np.uint8)
    rgb = np.repeat(checker[:, :, None], 3, axis=2)
    occupied = np.zeros((height, width), dtype=bool)
    occupied[7:33, 7:33] = True
    rgb[7:33, 7:33] = (170, 170, 170)
    rgb[8:32, 8:32] = (38, 39, 38)
    rgb[12:17, 12:17] = (250, 249, 248)
    rgb[22:27, 12:17] = (166, 168, 167)
    rgb[17:21, 23:27] = (181, 113, 61)
    occupied[23:25, 23:25] = False
    rgba = np.dstack((rgb, occupied.astype(np.uint8) * 255))
    rgba[~occupied] = 0
    protected = np.zeros_like(occupied)
    protected[7, 13:20] = True
    _, changed, preserved = verify_wolf_unmatte_scope(
        importer.unmatte_exterior, Image.fromarray(rgba, "RGBA"), rgb, protected,
        "wolf one-pixel exterior correction")
    check(changed > 0, "Wolf exterior regression fixture must exercise an actual matte correction")
    print(f"Wolf exterior edge regression: PASS — {changed} outer-edge pixels corrected; "
          f"{preserved} interior/protected pixels retain exact RGBA, including natural white and grey")


def validate_wolf_probes(probe_path=None):
    path = Path(probe_path) if probe_path is not None else ANNOTATIONS / "wolf-probes.json"
    document = json.loads(path.read_bytes())
    check(document["schema_version"] == 1, "Wolf cutout probes use an unsupported schema")
    probes = document["probes"]
    check(isinstance(probes, list) and probes, "Reviewed wolf cutout probes are missing")
    importer = load_sprite_importer("prepare_v086_wolf_assets.py", "cutout_wolf_probe_importer")
    original_unmatte = importer.unmatte_exterior
    context = ["wolf source probe"]
    preservation = []

    def inspected_unmatte(image, original_rgb, protected):
        output, changed, preserved = verify_wolf_unmatte_scope(
            original_unmatte, image, original_rgb, protected, context[0])
        preservation.append((changed, preserved))
        return output

    importer.unmatte_exterior = inspected_unmatte
    source_metadata = json.loads((ROOT / "source-assets/wolf-v086/sources.json").read_bytes())
    images = {}
    sources = {}
    extracted = {}
    ids = set()
    coverage = {}
    source_checks = 0

    def source_sheet(age, sheet_kind, ident):
        check(age in {"cub", "teen", "adult", "old"} and sheet_kind in {"main", "objects"},
              f"{ident}: unknown original source age or sheet")
        key = (age, sheet_kind)
        if key not in sources:
            metadata = source_metadata[age][sheet_kind]
            source_path = ROOT / "source-assets/wolf-v086" / metadata["file"]
            check(source_path.name == f"wolf_{age}_{sheet_kind}_source.png",
                  f"{ident}: original source belongs to a different age or sheet")
            check(sha256(source_path.read_bytes()) == metadata["sha256"],
                  f"{ident}: original source sheet changed")
            with Image.open(source_path) as original:
                sources[key] = original.convert("RGBA")
        return sources[key]

    def source_cutout(age, sheet_kind, action, number, ident):
        sheet = source_sheet(age, sheet_kind, ident)
        number = integer(number, f"{ident}: source frame", low=1)
        key = (age, sheet_kind, action, number)
        if key not in extracted:
            context[0] = f"wolf {age}/{sheet_kind}/{action} #{number}"
            if sheet_kind == "main":
                check(action in importer.MAIN[age], f"{ident}: unknown original main action {action}")
                check(number <= len(importer.MAIN[age][action]), f"{ident}: source frame out of range")
                extracted[key] = importer.extract_main_pose(sheet, age, action, number - 1)
            else:
                check(action in importer.OBJECT_ACTIONS and number == 1,
                      f"{ident}: object interaction must reference its one source pose")
                extracted[key] = importer.extract_object_pose(sheet, age, action)
        return extracted[key]

    try:
        for probe in probes:
            ident = probe["id"]
            check(isinstance(ident, str) and ident and ident not in ids,
                  f"Wolf probes require unique nonempty identifiers: {ident}")
            ids.add(ident)
            age = probe["age"]
            runtime = sprite_resource_path("wolf", age, probe["resource"])
            frame = load_probe_frame(runtime, probe["frame"], images)
            x, y = point([probe["x"], probe["y"]], f"{ident}: runtime point")
            kind = probe["kind"]
            check(kind in {"background", "protected"}, f"{ident}: unknown probe kind {kind}")
            expected_alpha = probe["expected_alpha"]
            check(type(expected_alpha) is int and expected_alpha == (0 if kind == "background" else 255),
                  f"{ident}: background must be transparent and protected detail opaque")
            actual_alpha = frame.getpixel((x, y))[3]
            check(actual_alpha == expected_alpha,
                  f"{ident}: runtime alpha {actual_alpha}, expected {expected_alpha} at {(x, y)}")
            coverage.setdefault(age, set()).add(kind)

            source = probe["source"]
            sheet_kind = source["kind"]
            sheet = source_sheet(age, sheet_kind, ident)
            sx = integer(source["x"], f"{ident}: original source x", high=sheet.width - 1)
            sy = integer(source["y"], f"{ident}: original source y", high=sheet.height - 1)
            expected_rgba = source["rgba"]
            check(isinstance(expected_rgba, list) and len(expected_rgba) == 4,
                  f"{ident}: original source RGBA must have four channels")
            for value in expected_rgba:
                integer(value, f"{ident}: original source channel", high=255)
            check(list(sheet.getpixel((sx, sy))) == expected_rgba,
                  f"{ident}: reviewed original pixel no longer matches its source sheet")

            if "source_alpha_probe" in probe:
                source_probe = probe["source_alpha_probe"]
                action = source_probe["cell_action"]
                number = integer(source_probe["cell_frame"], f"{ident}: source frame", low=1)
                cutout = source_cutout(age, sheet_kind, action, number, ident)
                cx = integer(source_probe["x"], f"{ident}: extracted source x", high=cutout.width - 1)
                cy = integer(source_probe["y"], f"{ident}: extracted source y", high=cutout.height - 1)
                expected = integer(source_probe["expected_alpha"], f"{ident}: extracted source alpha", high=255)
                actual = cutout.getpixel((cx, cy))[3]
                check(actual == expected,
                      f"{ident}: original-scale cutout alpha {actual}, expected {expected} at {(cx, cy)}")
                source_checks += 1

        edge_samples = document["edge_samples"]
        check(isinstance(edge_samples, list) and edge_samples,
              "Wolf cutout probes must include reviewed semi-transparent edge colours")
        for probe in edge_samples:
            ident = probe["id"]
            check(isinstance(ident, str) and ident and ident not in ids,
                  f"Wolf edge samples require unique nonempty identifiers: {ident}")
            ids.add(ident)
            cutout = source_cutout(probe["age"], probe["source_kind"], probe["cell_action"],
                                   probe["cell_frame"], ident)
            x = integer(probe["x"], f"{ident}: extracted edge x", high=cutout.width - 1)
            y = integer(probe["y"], f"{ident}: extracted edge y", high=cutout.height - 1)
            expected = probe["rgba"]
            check(isinstance(expected, list) and len(expected) == 4,
                  f"{ident}: semi-transparent edge sample requires all RGBA channels")
            for channel in expected:
                integer(channel, f"{ident}: expected edge channel", high=255)
            check(0 < expected[3] < 255, f"{ident}: edge sample must target a real partial opacity")
            tolerance = integer(probe["tolerance"], f"{ident}: edge channel tolerance", high=3)
            actual = np.array(cutout.getpixel((x, y)), dtype=np.int16)
            check(np.all(np.abs(actual - np.array(expected, dtype=np.int16)) <= tolerance),
                  f"{ident}: extracted edge RGBA {actual.tolist()}, expected {expected} within {tolerance}")
    finally:
        importer.unmatte_exterior = original_unmatte
    check(set(coverage) == {"cub", "teen", "adult", "old"}
          and all(kinds == {"background", "protected"} for kinds in coverage.values()),
          "Wolf cutout probes must cover both removed background and protected detail at all four ages")
    check(source_checks > 0 and preservation,
          "Wolf probes must exercise real source extraction and interior-colour preservation")
    preserved = sum(count for _, count in preservation)
    print(f"Reviewed wolf cutouts: PASS — {len(probes)} runtime probes across four ages, "
          f"{source_checks} original-scale alpha probes, {len(edge_samples)} semi-transparent edge samples, "
          f"{len(extracted)} source cutouts; "
          f"{preserved} interior/protected RGBA pixels unchanged by exterior cleanup")


def validate_reviewed_cutouts():
    geometry = load_sprite_importer("cutout_geometry.py", "cutout_geometry_validator")
    annotation_files = sorted(ANNOTATIONS.glob("leopard-*.json"))
    check(annotation_files, "Reviewed leopard cutout annotation files are missing")
    expected_hashes = {}
    expected_findings = {}
    for path in annotation_files:
        payload = path.read_bytes()
        document = json.loads(payload)
        expected_hashes[path.name] = sha256(payload)
        check(document["species"] == "leopard" and document["frame_size"] == [FRAME, FRAME],
              f"{path.name}: wrong species or frame dimensions")
        check(document["frame_numbering"] == "1-based", f"{path.name}: ambiguous frame numbering")
        findings = document["findings"]
        check(isinstance(findings, list) and findings, f"{path.name}: reviewed findings must not be empty")
        for finding in findings:
            age = finding.get("age", document.get("age"))
            name = finding["resource"]
            check(age in {"cub", "teen", "adult", "old"}, f"{path.name}: unknown age {age}")
            check(isinstance(name, str) and Path(name).name == name
                  and name.startswith(f"leopard_{age}_") and Path(name).suffix in {".png", ".webp"},
                  f"{path.name}: resource does not belong to the declared age: {name}")
            resource = f"app/src/main/res-{age}/drawable-nodpi/{name}"
            number = integer(finding["frame"], f"{name}: frame", low=1)
            frames = expected_findings.setdefault(resource, {})
            check(number not in frames, f"{name} #{number}: duplicate annotation")
            check(isinstance(finding["regions"], list) and finding["regions"],
                  f"{name} #{number}: at least one reviewed region is required")
            for index, region in enumerate(finding["regions"], 1):
                region_bounds(region, f"{name} #{number} region {index}")
            for preserved in finding.get("keep_points", []) + finding.get("keep_effect_points", []):
                point(preserved, f"{name} #{number}: protected detail")
            frames[number] = finding

    report = json.loads(MANIFEST.read_bytes())
    check(report["frame_size"] == FRAME, "Cutout manifest uses the wrong frame size")
    check(report["annotations_sha256"] == expected_hashes,
          "Cutout manifest is stale: annotation coverage or SHA-256 differs")
    files = report["files"]
    check(isinstance(files, list) and files, "Cutout manifest contains no audited resources")
    listed_paths = [record["path"] for record in files]
    check(len(listed_paths) == len(set(listed_paths)), "Cutout manifest contains duplicate resources")
    check(set(listed_paths) == set(expected_findings),
          "Cutout manifest does not cover exactly the annotated resources")

    frame_total = 0
    seed_total = 0
    protected_total = 0
    protected_area_total = 0
    removed_total = 0
    for record in files:
        relative = record["path"]
        path = ROOT / relative
        check(sha256(path.read_bytes()) == record["sha256"],
              f"{path.name}: file changed after the cutout audit")
        with Image.open(path) as source:
            source.load()
            image = source.convert("RGBA")
        check(list(image.size) == record["size"] and image.height == FRAME
              and image.width > 0 and image.width % FRAME == 0,
              f"{path.name}: recorded and actual frame dimensions disagree")
        check(sha256(image.tobytes()) == record["rgba_sha256"],
              f"{path.name}: decoded RGBA pixels differ from the audited image")
        baseline = geometry.reconstruct_cutout_source(record, image)
        check(baseline.mode == "RGBA" and baseline.size == image.size,
              f"{path.name}: reconstructed source uses different frame dimensions")
        check(sha256(image.tobytes()) == record["rgba_sha256"],
              f"{path.name}: source reconstruction mutated the final sprite")
        details = record["frames"]
        check(isinstance(details, list) and details, f"{path.name}: no frame audit")
        numbers = [entry["frame"] for entry in details]
        check(len(numbers) == len(set(numbers)), f"{path.name}: duplicate audited frame")
        check(set(numbers) == set(expected_findings[relative]),
              f"{path.name}: manifest misses or adds reviewed frames")

        for frame in details:
            number = integer(frame["frame"], f"{path.name}: frame", low=1, high=image.width // FRAME)
            finding = expected_findings[relative][number]
            label = f"{path.name} #{number}"
            x = (number - 1) * FRAME
            rgba = np.array(image.crop((x, 0, x + FRAME, FRAME)))
            baseline_rgba = np.array(baseline.crop((x, 0, x + FRAME, FRAME)))
            removed_rgba = geometry.decode_removed_rgba(frame, label)
            removed_mask = removed_rgba[:, :, 3] > 0
            expected_polygons = finding.get("keep_polygons", [])
            check(frame["keep_polygons"] == expected_polygons,
                  f"{label}: protected cheek/fur polygons differ from the annotations")
            protected_area = protected_polygon_mask(expected_polygons, label)
            protected_pixels = integer(frame["protected_area_pixels"], label + " protected area pixels")
            check(protected_pixels == int(protected_area.sum()),
                  f"{label}: protected area coverage differs from the independently rasterized mask")
            check(sha256(rgba[protected_area].tobytes()) == frame["protected_area_rgba_sha256"],
                  f"{label}: protected cheek/fur area changed after its original RGBA audit")
            check(not np.any(removed_mask & protected_area),
                  f"{label}: removed pixels overlap a protected cheek/fur polygon")
            protected_area_total += protected_pixels
            check(len(frame["regions"]) == len(finding["regions"]),
                  f"{label}: region audit coverage differs from the annotations")
            region_removed = 0
            reviewed_area = np.zeros((FRAME, FRAME), dtype=bool)
            for index, (audited, annotated) in enumerate(zip(frame["regions"], finding["regions"]), 1):
                check(audited["seed"] == annotated["seed"] and audited["box"] == annotated["box"],
                      f"{label} region {index}: reviewed seed or bounds changed")
                sx, sy, area = region_bounds(audited, f"{label} region {index}")
                left, top, right, bottom = annotated["box"]
                reviewed_area[top:bottom, left:right] = True
                check(int(rgba[sy, sx, 3]) == 0,
                      f"{label} region {index}: reviewed background seed remains opaque at {(sx, sy)}")
                maximum = annotated.get("max_pixels", area)
                integer(maximum, f"{label} region {index}: removal limit", high=area)
                region_removed += integer(audited["removed_pixels"],
                                          f"{label} region {index}: removed pixels", high=maximum)
                seed_total += 1

            check(not np.any(removed_mask & ~reviewed_area),
                  f"{label}: removed pixels extend outside the reviewed region bounds")
            check(np.array_equal(removed_rgba[removed_mask], baseline_rgba[removed_mask]),
                  f"{label}: removed RGBA pixels disagree with the hash-verified reconstructed source")

            expected_points = finding.get("keep_points", []) + finding.get("keep_effect_points", [])
            preserved = frame["protected"]
            check(isinstance(preserved, list)
                  and [item["point"] for item in preserved] == expected_points,
                  f"{label}: protected fur/effect points differ from the reviewed annotations")
            for item in preserved:
                px, py = point(item["point"], label + " protected point")
                expected_rgba = item["rgba"]
                check(isinstance(expected_rgba, list) and len(expected_rgba) == 4,
                      f"{label}: protected colour must contain four RGBA channels")
                for channel in expected_rgba:
                    integer(channel, label + " protected channel", high=255)
                check(expected_rgba[3] > 20 and rgba[py, px].tolist() == expected_rgba,
                      f"{label}: protected fur/effect pixel changed or was erased at {(px, py)}")
                protected_total += 1

            before = integer(frame["visible_before"], label + " visible before", low=1)
            after = integer(frame["visible_after"], label + " visible after", low=1)
            removed = integer(frame["removed_pixels"], label + " total removed pixels")
            check(before == int((baseline_rgba[:, :, 3] > 20).sum()),
                  f"{label}: original visible count differs from the reconstructed source")
            check(after == int((rgba[:, :, 3] > 20).sum()), f"{label}: visible pixel count is stale")
            check(before >= after >= before * .65, f"{label}: correction removed too much of the drawing")
            check(removed == region_removed and before - after <= removed,
                  f"{label}: removal statistics contradict the per-region audit")
            check(removed == int(removed_mask.sum()),
                  f"{label}: removal statistics disagree with the reconstructed pixel mask")
            removed_total += removed
            frame_total += 1

    check(report["annotated_frames"] == frame_total, "Cutout manifest total reviewed frames is incorrect")
    check(report["removed_pixels"] == removed_total, "Cutout manifest total removed pixels is incorrect")
    print(f"Reviewed leopard cutouts: PASS — {len(files)} resources, {frame_total} frames, "
          f"{seed_total} transparent background seeds and {protected_total} protected fur/effect pixels; "
          f"{protected_area_total} polygon-protected pixels; original RGBA sources reconstruct exactly, "
          "and removed pixels stay inside reviewed regions")


def main():
    validate_alpha_cleanup()
    validate_generated_edge_probes()
    validate_wolf_edge_preservation()
    validate_reviewed_cutouts()
    validate_wolf_probes()


if __name__ == "__main__":
    main()
