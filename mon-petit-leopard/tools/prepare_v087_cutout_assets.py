#!/usr/bin/env python3
"""Remove reviewed background pockets from generated leopard sprite frames.

The annotations identify background seeds inside small, bounded regions. A
region is never erased wholesale: only the connected neutral background is
selected, leaving the character outline, pale fur and coloured effects intact.
Run after the normal sprite preparation steps, before validation/building.
"""
from __future__ import annotations

from collections import defaultdict, deque
import base64
import hashlib
import json
from pathlib import Path
import zlib

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / "app/src/main"
ANNOTATIONS = ROOT / "source-assets/cutout-v087"
MANIFEST = ROOT / "cutout-audit-manifest.json"
FRAME = 256


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def rgba_digest(im: Image.Image) -> str:
    return digest(im.convert("RGBA").tobytes())


def select_background(arr: np.ndarray, region: dict,
                      protected_mask: np.ndarray | None = None) -> np.ndarray:
    """Flood only a reviewed pocket; no global white/grey colour deletion."""
    sx, sy = region["seed"]
    left, top, right, bottom = region["box"]
    if not (0 <= left <= sx < right <= FRAME and
            0 <= top <= sy < bottom <= FRAME):
        raise ValueError(f"Invalid seed or bounding region: {region}")
    result = np.zeros((FRAME, FRAME), dtype=bool)
    if arr[sy, sx, 3] == 0:
        return result
    rgb = arr[:, :, :3].astype(np.int16)
    chroma = rgb.max(axis=2) - rgb.min(axis=2)
    lightness = rgb.mean(axis=2)
    candidate = ((arr[:, :, 3] > 0) &
                 (chroma <= region.get("max_chroma", 34)) &
                 (lightness >= region.get("min_luma", 110)) &
                 (lightness <= region.get("max_luma", 255)))
    if protected_mask is not None:
        candidate &= ~protected_mask
    if not candidate[sy, sx]:
        raise ValueError(f"Annotated background seed is not neutral: {region}; "
                         f"pixel={arr[sy, sx].tolist()}")
    queue = deque([(sx, sy)])
    result[sy, sx] = True
    while queue:
        x, y = queue.popleft()
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if (left <= nx < right and top <= ny < bottom and
                    candidate[ny, nx] and not result[ny, nx]):
                result[ny, nx] = True
                queue.append((nx, ny))
    if int(result.sum()) > region.get("max_pixels", (right-left)*(bottom-top)):
        raise ValueError(f"Background selection exceeds the reviewed limit: {region}")
    return result


def repair_frame(im: Image.Image, finding: dict) -> tuple[Image.Image, dict]:
    before = np.array(im.convert("RGBA"))
    after = before.copy()
    selection = np.zeros((FRAME, FRAME), dtype=bool)
    protected_image = Image.new("L", (FRAME, FRAME), 0)
    painter = ImageDraw.Draw(protected_image)
    for polygon in finding.get("keep_polygons", []):
        if len(polygon) < 3 or any(not (0 <= x < FRAME and 0 <= y < FRAME)
                                   for x, y in polygon):
            raise ValueError("Invalid protected character polygon")
        painter.polygon([tuple(point) for point in polygon], fill=255)
    protected_mask = np.array(protected_image) > 0
    regions = []
    for region in finding["regions"]:
        mask = select_background(after, region, protected_mask)
        selection |= mask
        after[mask] = 0
        regions.append({"seed": region["seed"], "box": region["box"],
                        "zone": region.get("zone", "reviewed background"),
                        "removed_pixels": int(mask.sum())})
    protected = []
    for point in finding.get("keep_points", []) + finding.get("keep_effect_points", []):
        x, y = point
        if before[y, x, 3] <= 20:
            raise ValueError(f"Protected character/effect point is empty: {point}")
        if not np.array_equal(after[y, x], before[y, x]):
            raise ValueError(f"Background selection reached a protected detail: {point}")
        protected.append({"point": point, "rgba": before[y, x].tolist()})
    if not np.array_equal(before[~selection], after[~selection]):
        raise ValueError("Pixels outside the reviewed background selection changed")
    if not np.array_equal(before[protected_mask], after[protected_mask]):
        raise ValueError("A protected character area changed")
    visible_before = int((before[:, :, 3] > 20).sum())
    visible_after = int((after[:, :, 3] > 20).sum())
    if visible_after < visible_before * 0.65:
        raise ValueError("Background correction removes too much of the frame")
    # Keep a compact, exact record of the discarded background. The size
    # contract can reconstruct the normalized input instead of mistaking a
    # newly transparent tail gap or detached bubble for a smaller character.
    removed_rgba = np.zeros_like(before)
    removed_rgba[selection] = before[selection]
    encoded_removed = base64.b64encode(
        zlib.compress(removed_rgba.tobytes(), level=9)
    ).decode("ascii")
    return Image.fromarray(after), {
        "frame": finding["frame"], "regions": regions, "protected": protected,
        "before_frame_rgba_sha256": digest(before.tobytes()),
        "removed_rgba_zlib_base64": encoded_removed,
        "keep_polygons": finding.get("keep_polygons", []),
        "protected_area_rgba_sha256": digest(before[protected_mask].tobytes()),
        "protected_area_pixels": int(protected_mask.sum()),
        "removed_pixels": int(selection.sum()),
        "visible_before": visible_before, "visible_after": visible_after,
    }


def save_verified(im: Image.Image, destination: Path):
    staging = ROOT / "build/cutout-assets-staging"
    staging.mkdir(parents=True, exist_ok=True)
    temporary = staging / destination.name
    if destination.suffix == ".png":
        im.save(temporary, "PNG", optimize=False)
    else:
        im.save(temporary, "WEBP", lossless=True, exact=True, method=6)
    with Image.open(temporary) as saved:
        saved.load()
        if saved.size != im.size or rgba_digest(saved) != rgba_digest(im):
            raise ValueError(f"Sprite changed during encoding: {destination.name}")
    temporary.replace(destination)


def main():
    annotation_files = sorted(ANNOTATIONS.glob("leopard-*.json"))
    if not annotation_files:
        raise ValueError("Reviewed leopard cutout annotations are missing")
    grouped = defaultdict(list)
    annotations_sha256 = {}
    for source in annotation_files:
        document = json.loads(source.read_text())
        if document["species"] != "leopard" or document["frame_size"] != [FRAME, FRAME]:
            raise ValueError(f"Wrong species/frame size: {source}")
        annotations_sha256[source.name] = digest(source.read_bytes())
        for finding in document["findings"]:
            age = finding.get("age", document.get("age"))
            if age not in ("cub", "teen", "adult", "old"):
                raise ValueError(f"Unknown age: {age}")
            name = finding["resource"]
            if not name.startswith(f"leopard_{age}_") or Path(name).name != name:
                raise ValueError(f"Wrong resource for age {age}: {name}")
            grouped[(age, name)].append(finding)

    report = {"version": "0.8.7", "frame_size": FRAME,
              "annotations_sha256": annotations_sha256, "files": []}
    for (age, name), findings in sorted(grouped.items()):
        path = RUNTIME / f"res-{age}/drawable-nodpi" / name
        with Image.open(path) as source:
            source.load()
            original = source.convert("RGBA")
        if original.height != FRAME or original.width % FRAME:
            raise ValueError(f"Unexpected sprite dimensions: {name} {original.size}")
        output = original.copy()
        record = {"path": path.relative_to(ROOT).as_posix(),
                  "before_sha256": digest(path.read_bytes()),
                  "before_rgba_sha256": rgba_digest(original),
                  "size": list(original.size), "frames": []}
        visited = set()
        for finding in sorted(findings, key=lambda item: item["frame"]):
            number = finding["frame"]
            if number in visited or not 1 <= number <= original.width // FRAME:
                raise ValueError(f"Duplicate/out-of-range frame: {name} #{number}")
            visited.add(number)
            x = (number - 1) * FRAME
            frame = original.crop((x, 0, x + FRAME, FRAME))
            repaired, details = repair_frame(frame, finding)
            output.paste(repaired, (x, 0))
            record["frames"].append(details)
        save_verified(output, path)
        record["sha256"] = digest(path.read_bytes())
        record["rgba_sha256"] = rgba_digest(output)
        report["files"].append(record)
    report["annotated_frames"] = sum(len(item["frames"]) for item in report["files"])
    report["removed_pixels"] = sum(frame["removed_pixels"] for item in report["files"]
                                    for frame in item["frames"])
    temporary = ROOT / "build/cutout-assets-staging/cutout-audit-manifest.json"
    temporary.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    temporary.replace(MANIFEST)
    print(f"Cutout repair: {len(report['files'])} leopard resources, "
          f"{report['annotated_frames']} reviewed frames, "
          f"{report['removed_pixels']} background pixels removed; protected details intact")


if __name__ == "__main__":
    main()
