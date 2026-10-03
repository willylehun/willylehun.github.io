#!/usr/bin/env python3
"""Extract sixteen supplied lion sheets into eight sex- and age-specific packs.

Only the user's source pixels are used. The two geometry modules document each
sheet's real panel positions and audited cutouts, including the different
1448x1086 female cub/adult layouts. No sprite from another animal is imported.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ProcessPoolExecutor
import hashlib
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageOps

from prepare_v086_wolf_assets import components, extract, normalize
import lion_male_source_geometry as male_geometry
import lion_female_source_geometry as female_geometry

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source-assets" / "lion-v0810"
RUNTIME = ROOT / "app" / "src" / "main"
SEXES = ("male", "female")
AGES = ("cub", "teen", "adult", "old")
FRAME = 256
SAFE = 16
GEOMETRY = {"male": male_geometry, "female": female_geometry}
MAIN = {sex: module.MAIN for sex, module in GEOMETRY.items()}
OBJECT_ACTIONS = ("bottle", "comb", "bowl", "fetch_mouse", "fetch_plush",
                  "fetch_ball", "fetch_tennis", "fetch_yarn", "groom_foam",
                  "rope_play", "scratcher_play", "soap", "towel")
OBJECTS = {sex: module.OBJECTS for sex, module in GEOMETRY.items()}
TOY_ACTIONS = ("fetch_mouse", "fetch_ball", "fetch_tennis", "fetch_yarn", "rope_play")
# These two source pairs use a light blue printed checker, including below
# the feet. It must remain background when classifying a closed white region.
BLUE_CHECKER_PACKS = {("female", "teen"), ("female", "adult")}


def settings(sex, name):
    return getattr(GEOMETRY[sex], name, {})


def extract_main_pose(sheet, sex, age, action, frame_index):
    box = MAIN[sex][age][action][frame_index]
    blue = ((0, 0, box[2] - box[0], box[3] - box[1]), ()) if (sex, age) in BLUE_CHECKER_PACKS else None
    return extract(sheet, box, action in ("sleep", "moods"),
                   settings(sex, "MAIN_GROUND").get(age, {}).get(action),
                   settings(sex, "LABEL_MASKS").get((age, action, frame_index)),
                   care_blue=blue,
                   protected_polygons=settings(sex, "FUR_PROTECT").get((age, action, frame_index), ()),
                   protect_closed_floor=((sex, age) not in BLUE_CHECKER_PACKS
                       and (age, action, frame_index) not in settings(sex, "MAIN_NO_CLOSED_FLOOR")))


def object_ground(sex, age, action):
    overrides = settings(sex, "OBJECT_GROUND")
    if (age, action) in overrides:
        return overrides[(age, action)]
    box = OBJECTS[sex][age][OBJECT_ACTIONS.index(action)]
    height = box[3] - box[1]
    if action in TOY_ACTIONS:
        return round(height * .75)
    if action in ("bottle", "comb", "fetch_plush", "groom_foam", "soap", "towel"):
        return height - 40
    return None


def without_scratcher(im, sex, age):
    outlines = settings(sex, "SCRATCHER_OUTLINES")
    if age not in outlines:
        raise RuntimeError(f"Missing audited lion scratcher silhouette: {sex}/{age}")
    mask = Image.new("L", im.size)
    ImageDraw.Draw(mask).polygon(outlines[age], fill=255)
    arr = np.array(im)
    arr[np.array(mask) == 0] = 0
    labels, pieces = components(arr[:, :, 3] > 20)
    if not pieces:
        raise RuntimeError(f"Empty lion scratcher silhouette: {sex}/{age}")
    arr[labels != pieces[0][1]] = 0
    return Image.fromarray(arr)


def extract_object_pose(sheet, sex, age, action):
    box = OBJECTS[sex][age][OBJECT_ACTIONS.index(action)]
    if action == "scratcher_play":
        cropped = np.array(sheet.crop(box).convert("RGBA"))
        header = settings(sex, "SCRATCHER_HEADERS").get(age, 0)
        if header:
            rgb = cropped[:header, :, :3].astype("int16")
            green = (rgb[:, :, 1] > rgb[:, :, 0] + 4) & (rgb[:, :, 1] > rgb[:, :, 2] + 8)
            cropped[:header][green] = 255
        local = Image.fromarray(cropped)
        value = extract(local, (0, 0, local.width, local.height),
                        floor_band=local.height - 45, protect_closed_floor=False,
                        care_blue=((0, 0, local.width, local.height), ()) if (sex, age) in BLUE_CHECKER_PACKS else None,
                        hole_seeds=settings(sex, "SCRATCHER_HOLES").get(age, ()),
                        protected_polygons=settings(sex, "SCRATCHER_PROTECT").get(age, ()))
        return without_scratcher(value, sex, age)
    care_blue = settings(sex, "CARE_BLUE").get((age, action))
    if care_blue is None and (sex, age) in BLUE_CHECKER_PACKS:
        care_blue = ((0, 0, box[2] - box[0], box[3] - box[1]), ())
    header = settings(sex, "OBJECT_HEADERS").get((age, action), 0)
    source, source_box = sheet, box
    if header:
        cropped = np.array(sheet.crop(box).convert("RGBA"))
        rgb = cropped[:header, :, :3].astype("int16")
        green = (rgb[:, :, 1] > rgb[:, :, 0] + 4) & (rgb[:, :, 1] > rgb[:, :, 2] + 8)
        cropped[:header][green] = 255
        source = Image.fromarray(cropped)
        source_box = (0, 0, source.width, source.height)
    return extract(source, source_box, action in ("groom_foam", "soap"),
                   object_ground(sex, age, action),
                   erase_polygon=settings(sex, "OBJECT_ERASE").get((age, action)),
                   hole_seeds=settings(sex, "TOY_HOLES").get((age, action), ()),
                   care_blue=care_blue,
                   protected_polygons=settings(sex, "CARE_PROTECT").get((age, action), ()),
                   protect_closed_floor=False)


def head_token(sheet, sex, age):
    full_box = MAIN[sex][age]["idle_down"][0]
    full = extract_main_pose(sheet, sex, age, "idle_down", 0)
    box = settings(sex, "HEAD_BOXES")[age]
    head = full.crop((box[0] - full_box[0], box[1] - full_box[1],
                      box[2] - full_box[0], box[3] - full_box[1]))
    outlines = settings(sex, "HEAD_OUTLINES")
    if age in outlines:
        outline = [(x - box[0], y - box[1]) for x, y in outlines[age]]
        mask = Image.new("L", head.size)
        ImageDraw.Draw(mask).polygon(outline, fill=255)
        arr = np.array(head)
        arr[np.array(mask) == 0] = 0
        head = Image.fromarray(arr)
    # A face crop can intersect the curled tail beside the mane. The portrait
    # is the connected head silhouette, so detached tail pixels are excluded.
    arr = np.array(head)
    labels, parts = components(arr[:, :, 3] > 20)
    if not parts:
        raise RuntimeError(f"Empty lion portrait: {sex}/{age}")
    arr[labels != parts[0][1]] = 0
    head = Image.fromarray(arr)
    return normalize(head, 30000), box


def save(im, path, check=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    staging = ROOT / "build" / "lion-assets-staging"
    staging.mkdir(parents=True, exist_ok=True)
    temporary = staging / (path.name + ".tmp")
    try:
        if path.suffix == ".webp":
            im.save(temporary, "WEBP", lossless=True, method=6, exact=True)
        else:
            im.save(temporary, "PNG", optimize=False)
        with Image.open(temporary) as encoded:
            encoded.load()
            if encoded.size != im.size or "A" not in encoded.getbands():
                raise RuntimeError(f"Invalid encoded lion resource: {path}")
            rebuilt_rgba = pixel_digest(encoded) if check else None
        if check:
            if not path.is_file():
                raise RuntimeError(f"Missing lion source rebuild target: {path}")
            # Lossless codecs may produce different compressed bytes across
            # Pillow versions. Every visible RGBA byte must still be identical;
            # meaningless RGB hidden under alpha=0 is canonically transparent.
            with Image.open(path) as existing:
                identical = (existing.size == im.size
                             and pixel_digest(existing) == rebuilt_rgba)
            if not identical:
                raise RuntimeError(f"Lion resource differs from its source rebuild: {path}")
        else:
            temporary.replace(path)
    finally:
        if temporary.exists():
            temporary.unlink()


def pixel_digest(im):
    rgba = im.convert("RGBA")
    rgba = Image.alpha_composite(Image.new("RGBA", rgba.size), rgba)
    return hashlib.sha256(str(rgba.size).encode() + rgba.tobytes()).hexdigest()


def prepare_pack(task):
    sex, age, metadata, check, qa_dir = task
    sheets = {}
    for kind in ("main", "objects"):
        source = SOURCE / metadata[kind]["file"]
        if hashlib.sha256(source.read_bytes()).hexdigest() != metadata[kind]["sha256"]:
            raise RuntimeError(f"Modified original lion source: {source}")
        sheets[kind] = Image.open(source).convert("RGBA")
        metadata[kind]["path"] = str(source.relative_to(ROOT))
        metadata[kind]["dimensions"] = list(sheets[kind].size)
    frames = {}; records = {}
    ground = settings(sex, "MAIN_GROUND").get(age, {})
    for action, boxes in MAIN[sex][age].items():
        values = [extract_main_pose(sheets["main"], sex, age, action, i) for i in range(len(boxes))]
        if action == "idle_left":
            values = [ImageOps.mirror(values[0])]
        frames[action] = values
        records[action] = {"source": "main", "source_cells": boxes,
            "derivation": "horizontal mirror of mislabeled source idle" if action == "idle_left" else "source poses"}
        if (sex, age) in BLUE_CHECKER_PACKS:
            records[action]["blue_checker_cleanup"] = "edge-connected blue-tinted source checker; white interiors stay closed"
        if action in ground:
            records[action]["checker_shadow_cleanup_bands"] = [[0, ground[action], b[2] - b[0], b[3] - b[1]] for b in boxes]
        no_close = [i + 1 for i in range(len(boxes))
                    if (age, action, i) in settings(sex, "MAIN_NO_CLOSED_FLOOR")]
        if no_close:
            records[action]["source_checker_kept_open_in_frames"] = no_close
        masks = {str(i + 1): settings(sex, "LABEL_MASKS")[(age, action, i)]
                 for i in range(len(boxes)) if (age, action, i) in settings(sex, "LABEL_MASKS")}
        if masks:
            records[action]["printed_label_masks"] = masks
        protection = {str(i + 1): settings(sex, "FUR_PROTECT")[(age, action, i)]
                      for i in range(len(boxes)) if (age, action, i) in settings(sex, "FUR_PROTECT")}
        if protection:
            records[action]["protected_source_fur_polygons"] = protection
    for direction in ("down", "left", "right", "up"):
        frames["run_" + direction] = frames["walk_" + direction]
        records["run_" + direction] = {**records["walk_" + direction],
            "derivation": "supplied walk sequence played at run cadence"}
    for action, box in zip(OBJECT_ACTIONS, OBJECTS[sex][age]):
        if action == "bowl" or (action == "bottle" and age != "cub"):
            continue
        frames[action] = [extract_object_pose(sheets["objects"], sex, age, action)]
        records[action] = {"source": "objects", "source_cells": [box],
            "derivation": "fixed scratcher removed; one supplied lion pose" if action == "scratcher_play"
                          else "one supplied object interaction pose"}
        if (sex, age) in BLUE_CHECKER_PACKS:
            records[action]["blue_checker_cleanup"] = "edge-connected blue-tinted source checker; white interiors stay closed"
        header = settings(sex, "OBJECT_HEADERS").get((age, action), 0)
        if header:
            records[action]["printed_header_cleanup_height"] = header
        if (age, action) in settings(sex, "OBJECT_ERASE"):
            records[action]["printed_ground_erase_polygon"] = settings(sex, "OBJECT_ERASE")[(age, action)]
        band = object_ground(sex, age, action)
        if band is not None:
            records[action]["checker_shadow_cleanup_band"] = [0, band, box[2] - box[0], box[3] - box[1]]
        if (age, action) in settings(sex, "TOY_HOLES"):
            records[action]["audited_background_seeds"] = settings(sex, "TOY_HOLES")[(age, action)]
        if (age, action) in settings(sex, "CARE_BLUE"):
            zone, ellipses = settings(sex, "CARE_BLUE")[(age, action)]
            records[action]["blue_checker_cleanup"] = {"zone": zone,
                "protected_source_bubble_ellipses": ellipses,
                "protected_source_foam_polygons": settings(sex, "CARE_PROTECT").get((age, action), [])}
        if action == "scratcher_play":
            records[action].update({
                "retained_source_silhouette": settings(sex, "SCRATCHER_OUTLINES")[age],
                "printed_header_cleanup_height": settings(sex, "SCRATCHER_HEADERS").get(age, 0),
                "protected_source_fur_polygons": settings(sex, "SCRATCHER_PROTECT").get(age, []),
                "audited_background_seeds": settings(sex, "SCRATCHER_HOLES").get(age, []),
                "checker_shadow_cleanup_band": [0, box[3] - box[1] - 45, box[2] - box[0], box[3] - box[1]],
            })
    feasible = []
    for values in frames.values():
        for im in values:
            _, parts = components(np.array(im.getchannel("A")) > 20)
            area, _, box = parts[0]
            max_dim = max(box[2] - box[0], box[3] - box[1])
            feasible.append(area * ((FRAME - 2 * SAFE) / max_dim) ** 2)
    target_area = int(min(24500, min(feasible) * .98))
    normalization = {"largest_component_target_area": target_area, "baseline": FRAME - SAFE,
        "method": "same largest-component visual mass for every pose, safe complete silhouette"}
    for action in frames:
        frames[action] = [normalize(im, target_area) for im in frames[action]]
    dst = RUNTIME / f"res-lion-{sex}-{age}" / "drawable-nodpi"
    assets = {}
    for action, values in frames.items():
        png = action.startswith("idle_") or action.startswith("fetch_") or action == "rope_play"
        name = f"lion_{sex}_{age}_{action}." + ("png" if png else "webp")
        out = Image.new("RGBA", (FRAME * len(values), FRAME))
        for i, im in enumerate(values):
            out.alpha_composite(im, (FRAME * i, 0))
        path = dst / name
        save(out, path, check=check)
        assets[name] = {"sex": sex, "age": age, "action": action,
            "path": str(path.relative_to(ROOT)), "frames": len(values), "width": out.width,
            "height": out.height, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "rgba_sha256": pixel_digest(out), **records[action]}
    token, box = head_token(sheets["main"], sex, age)
    path = dst / f"lion_{sex}_{age}_promenade_token.png"
    save(token, path, check=check)
    portrait = {path.name: {"sex": sex, "age": age, "path": str(path.relative_to(ROOT)),
        "width": FRAME, "height": FRAME, "frames": 1, "source": "main", "source_cells": [box],
        "derivation": "head crop of same-sex same-age idle_down",
        "retained_source_outline": settings(sex, "HEAD_OUTLINES").get(age),
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "rgba_sha256": pixel_digest(token)}}
    if qa_dir:
        qa_dir = Path(qa_dir)
        qa_dir.mkdir(parents=True, exist_ok=True)
        tiles = [(action + (f" {i+1}" if len(values) > 1 else ""), im)
                 for action, values in frames.items() for i, im in enumerate(values)]
        tiles.append(("promenade_token", token))
        cols = 8; tw = 160; th = 184
        for suffix, background, ink in (("all", "#d6e3dc", "#143627"),
                                        ("dark", "#343d49", "#edf2f4")):
            preview = Image.new("RGB", (cols * tw, math.ceil(len(tiles) / cols) * th), background)
            draw = ImageDraw.Draw(preview)
            for i, (label, im) in enumerate(tiles):
                x = (i % cols) * tw; y = (i // cols) * th
                patch = im.resize((tw, tw), Image.Resampling.LANCZOS)
                preview.paste(patch, (x, y), patch)
                draw.text((x + 4, y + tw + 3), label, fill=ink)
            preview.save(qa_dir / f"lion-{sex}-{age}-{suffix}.png")
    print(f"OK lion {sex} {age}: {len(frames)} gameplay resources + portrait; visual area {target_area}", flush=True)
    return sex, age, metadata, assets, portrait, normalization


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--qa-dir", type=Path, help="Scratch previews for visual review")
    parser.add_argument("--check", action="store_true",
                        help="Rebuild and compare exact visible RGBA without replacing runtime files")
    parser.add_argument("--jobs", type=int, default=4, help="Independent pack preparation workers")
    args = parser.parse_args()
    metadata = json.loads((SOURCE / "sources.json").read_text())
    manifest = {"version": "0.8.10", "species": "lion", "frame_size": FRAME,
        "safe_margin": SAFE, "sources": metadata, "assets": {}, "portraits": {},
        "cutout_probes": [dict(probe, sex=sex) for sex in SEXES
                          for probe in getattr(GEOMETRY[sex], "CUTOUT_PROBES", [])],
        "normalization": {},
        "notes": ["Sixteen original user PNG files are preserved byte for byte.",
            "Every sex and age has its own explicit source geometry and runtime directory.",
            "Printed checkerboards, labels and borders are excluded with audited source-space masks.",
            "LEFT idle is mirrored because the supplied cells labelled LEFT face right.",
            "Run uses the six supplied walking poses at the existing engine run cadence.",
            "Rope and scratcher use one supplied interaction pose, animated by the existing engine.",
            "The fixed scratcher is excluded because the room separately renders this prop.",
            "Detached bubbles and mood symbols are retained within the normalization safety margin.",
            "The old lioness main sheet has long grey hair; the separately supplied objects sheet has a shorter grey tuft. Both originals are preserved.",
            "No original drawing is substituted between species, sex or age."]}
    tasks = [(sex, age, metadata[sex][age], args.check, str(args.qa_dir) if args.qa_dir else None)
             for sex in SEXES for age in AGES]
    if args.jobs < 1:
        parser.error("--jobs must be at least 1")
    if args.jobs == 1:
        results = map(prepare_pack, tasks)
        executor = None
    else:
        executor = ProcessPoolExecutor(max_workers=min(args.jobs, len(tasks)))
        results = executor.map(prepare_pack, tasks)
    try:
        for sex, age, sources, assets, portraits, normalization in results:
            manifest["sources"][sex][age] = sources
            manifest["assets"].update(assets)
            manifest["portraits"].update(portraits)
            manifest["normalization"][f"{sex}_{age}"] = normalization
    finally:
        if executor:
            executor.shutdown()
    path = ROOT / "lion-sprite-manifest.json"
    encoded = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    if args.check:
        if not path.is_file() or path.read_text() != encoded:
            raise RuntimeError("Lion manifest differs from deterministic source rebuild")
    else:
        temporary = path.with_suffix(".json.tmp")
        temporary.write_text(encoded)
        temporary.replace(path)
    print("OK lion v0.8.10: 218 gameplay resources and 8 sex- and age-specific portraits")


if __name__ == "__main__":
    main()
