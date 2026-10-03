#!/usr/bin/env python3
"""Extract the user's eight wolf reference sheets without redrawing a sprite.

The reference PNGs are opaque presentation sheets, not ready-to-use atlases.
This importer removes their printed checkerboard, labels and borders, keeps the
four ages separate, and packs the supplied poses onto transparent 256px frames.
Only Pillow and NumPy are needed, as for the existing leopard importers.
"""
from __future__ import annotations

import argparse
from collections import deque
import hashlib
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source-assets" / "wolf-v086"
RUNTIME = ROOT / "app" / "src" / "main"
AGES = ("cub", "teen", "adult", "old")
FRAME = 256
SAFE = 16


def row(bounds, top, bottom):
    return [(bounds[i] + 2, top, bounds[i + 1] - 2, bottom)
            for i in range(len(bounds) - 1)]


# Explicit source coordinates prevent a shifted grid from importing labels or
# a neighbouring age. Coordinates are in the unmodified original source PNG.
MAIN = {
    "cub": {
        "idle_down": [(14, 99, 209, 248)],
        "idle_left": [(221, 99, 409, 248)],
        "idle_right": [(421, 99, 611, 248)],
        "idle_up": [(622, 99, 809, 248)],
        "walk_down": row([818, 934, 1050, 1166, 1282, 1401, 1525], 99, 234),
        "walk_left": row([13, 140, 264, 389, 513, 637, 764], 293, 425),
        "walk_right": row([771, 897, 1022, 1148, 1274, 1399, 1524], 293, 425),
        "walk_up": row([13, 140, 264, 389, 513, 638, 764], 471, 620),
        "jump": row([771, 920, 1069, 1219, 1369, 1524], 471, 620),
        "eat": row([14, 134, 253, 376], 668, 859),
        "sleep": row([384, 514, 645, 782], 668, 859),
        "moods": row([789, 913, 1032, 1150, 1272, 1394, 1524], 668, 778)
                 + row([789, 913, 1032, 1150, 1272, 1394, 1524], 796, 916),
    },
    "teen": {
        "idle_down": [(14, 99, 209, 248)],
        "idle_left": [(221, 99, 410, 248)],
        "idle_right": [(421, 99, 611, 248)],
        "idle_up": [(622, 99, 809, 248)],
        "walk_down": row([818, 934, 1050, 1166, 1282, 1401, 1525], 99, 247),
        "walk_left": row([13, 140, 264, 389, 513, 638, 764], 293, 425),
        "walk_right": row([771, 897, 1022, 1148, 1274, 1399, 1524], 293, 425),
        "walk_up": row([13, 140, 264, 389, 513, 638, 764], 471, 620),
        "jump": row([771, 920, 1069, 1219, 1369, 1524], 471, 620),
        "eat": row([14, 134, 253, 376], 668, 859),
        "sleep": row([384, 514, 645, 782], 668, 859),
        "moods": row([790, 914, 1033, 1151, 1272, 1394, 1523], 668, 778)
                 + row([790, 914, 1033, 1151, 1272, 1394, 1523], 796, 916),
    },
    "adult": {
        "idle_down": [(12, 101, 201, 275)],
        "idle_left": [(211, 101, 395, 275)],
        "idle_right": [(403, 101, 589, 275)],
        "idle_up": [(597, 101, 777, 275)],
        "walk_down": row([783, 886, 996, 1104, 1214, 1325, 1439], 101, 275),
        "walk_left": row([12, 130, 248, 366, 483, 600, 720], 320, 473),
        "walk_right": row([728, 851, 968, 1084, 1201, 1318, 1438], 320, 473),
        "walk_up": row([14, 131, 248, 365, 482, 599, 720], 518, 686),
        "jump": row([728, 869, 1009, 1152, 1293, 1438], 518, 686),
        "eat": row([14, 128, 240, 353], 732, 925),
        "sleep": row([362, 486, 613, 737], 732, 925),
        "moods": row([744, 860, 971, 1083, 1199, 1315, 1436], 732, 835)
                 + row([744, 860, 971, 1083, 1199, 1315, 1436], 858, 975),
    },
    "old": {
        "idle_down": [(13, 99, 196, 271)],
        "idle_left": [(209, 99, 389, 271)],
        "idle_right": [(400, 99, 579, 271)],
        "idle_up": [(590, 99, 763, 271)],
        "walk_down": row([772, 882, 991, 1104, 1213, 1323, 1439], 99, 270),
        "walk_left": row([12, 133, 250, 367, 485, 601, 720], 315, 457),
        "walk_right": row([729, 849, 969, 1085, 1201, 1319, 1437], 315, 457),
        "walk_up": row([14, 131, 248, 367, 483, 600, 720], 501, 655),
        "jump": row([729, 869, 1009, 1152, 1293, 1437], 501, 655),
        "eat": row([14, 128, 243, 356], 703, 910),
        "sleep": row([364, 486, 608, 737], 703, 910),
        "moods": row([744, 862, 973, 1086, 1202, 1316, 1438], 703, 831)
                 + row([744, 862, 973, 1086, 1202, 1316, 1438], 850, 976),
    },
}

OBJECT_ACTIONS = ("bottle", "comb", "bowl", "fetch_mouse", "fetch_plush",
                  "fetch_ball", "fetch_tennis", "fetch_yarn", "groom_foam",
                  "rope_play", "scratcher_play", "soap", "towel")
OBJECTS = {
    "cub": row([14, 322, 620, 919, 1216, 1524], 105, 340)
           + row([14, 322, 620, 919, 1216, 1524], 392, 623)
           + [(164, 663, 560, 892), (574, 670, 961, 892), (971, 670, 1363, 892)],
    "teen": row([14, 322, 620, 919, 1216, 1524], 105, 340)
            + row([14, 322, 620, 919, 1216, 1524], 392, 623)
            + [(164, 663, 560, 892), (574, 670, 961, 892), (971, 670, 1363, 892)],
    "adult": row([10, 296, 582, 869, 1155, 1438], 99, 348)
             + row([10, 296, 582, 869, 1155, 1438], 397, 647)
             + [(150, 687, 525, 947), (534, 695, 909, 947), (920, 695, 1295, 947)],
    "old": row([14, 305, 587, 869, 1147, 1438], 101, 341)
           + row([14, 305, 587, 869, 1147, 1438], 389, 640)
           + [(157, 681, 531, 925), (541, 689, 911, 925), (921, 689, 1285, 925)],
}


def neighbors(y, x, h, w):
    if y: yield y - 1, x
    if y + 1 < h: yield y + 1, x
    if x: yield y, x - 1
    if x + 1 < w: yield y, x + 1


def components(mask):
    """Four-connected components, independent of optional native libraries."""
    h, w = mask.shape
    labels = np.zeros((h, w), np.int32)
    found = []
    ident = 0
    for y, x in zip(*np.nonzero(mask)):
        if labels[y, x]:
            continue
        ident += 1
        labels[y, x] = ident
        queue = deque([(int(y), int(x))])
        size = 0
        x0 = x1 = int(x)
        y0 = y1 = int(y)
        while queue:
            yy, xx = queue.popleft()
            size += 1
            x0 = min(x0, xx); x1 = max(x1, xx)
            y0 = min(y0, yy); y1 = max(y1, yy)
            for ny, nx in neighbors(yy, xx, h, w):
                if mask[ny, nx] and not labels[ny, nx]:
                    labels[ny, nx] = ident
                    queue.append((ny, nx))
        found.append((size, ident, (x0, y0, x1 + 1, y1 + 1)))
    return labels, sorted(found, reverse=True)


def extract(sheet, box, effects=False, floor_band=None):
    """Remove only edge-connected neutral checkerboard, preserving white fur.

    White eyes, teeth, eyebrows and fur enclosed by the source drawing remain
    opaque. A global white colour key would erase these important details.
    """
    rgba = np.array(sheet.crop(box).convert("RGBA"))
    rgb = rgba[:, :, :3].astype(np.int16)
    high = rgb.max(axis=2)
    low = rgb.min(axis=2)
    neutral_light = (high - low <= 20) & (low >= 185)
    green_frame = (rgb[:, :, 1] > rgb[:, :, 0] + 4) & (rgb[:, :, 1] > rgb[:, :, 2] + 8)
    # Printed green titles/borders may touch the crop edge. Interior green
    # question marks and the tennis ball are part of the supplied drawing.
    yy, xx = np.indices(neutral_light.shape)
    if floor_band is not None:
        # The floor shadow is printed over checkerboard on the toy poses.
        # Limit this extra key to the annotated ground band below the belly;
        # it never changes face, ear, eyebrow or tail extraction thresholds.
        neutral_light |= (yy >= floor_band) & (high - low <= 10) & (low >= 105)
    border = (yy < 8) | (xx < 2) | (xx >= neutral_light.shape[1] - 2)
    candidate = neutral_light | (green_frame & border)
    h, w = candidate.shape
    background = np.zeros((h, w), bool)
    queue = deque()
    for y, x in [(0, x) for x in range(w)] + [(h-1, x) for x in range(w)] \
                + [(y, 0) for y in range(h)] + [(y, w-1) for y in range(h)]:
        if candidate[y, x] and not background[y, x]:
            background[y, x] = True
            queue.append((y, x))
    while queue:
        y, x = queue.popleft()
        for ny, nx in neighbors(y, x, h, w):
            if candidate[ny, nx] and not background[ny, nx]:
                background[ny, nx] = True
                queue.append((ny, nx))
    # Closed gaps between paws can contain printed checkerboard even though
    # they are not connected to the outside. Its alternating neutral grey and
    # white cells differ from the smoothly shaded warm fur. Require all these
    # independent checks before removing a closed patch; white eyebrows fail
    # the neutral/alternating-tone tests and therefore remain intact.
    enclosed_labels, enclosed = components(neutral_light & ~background)
    for area, ident, bounds in enclosed:
        if area < 35:
            continue
        values = rgb[enclosed_labels == ident]
        luminance = values.mean(axis=1)
        pure_grey = np.mean(values.max(axis=1) - values.min(axis=1) <= 3)
        bright_fraction = np.mean(luminance > 234)
        dark_fraction = np.mean(luminance < 220)
        if pure_grey >= .80 and .20 <= bright_fraction <= .70 \
                and dark_fraction >= .06 and luminance.std() >= 11:
            background |= enclosed_labels == ident
    rgba[background] = 0
    labels, comps = components(~background)
    if not comps:
        raise RuntimeError(f"Empty source cell {box}")
    main = comps[0]
    keep = labels == main[1]
    if effects:
        # Keep detached Zzz/thought bubbles/hearts/question marks. Crops stop
        # above printed mood names, and the subject's baseline rejects numbers.
        bottom = main[2][3]
        for area, ident, bounds in comps[1:]:
            bw = bounds[2] - bounds[0]; bh = bounds[3] - bounds[1]
            thin_border = min(bw, bh) <= 4 and max(bw, bh) >= 10
            if area >= 4 and not thin_border and bounds[1] < bottom - 3 and bounds[3] <= bottom + 1:
                keep |= labels == ident
    rgba[~keep] = 0
    return Image.fromarray(rgba)


def visible_area(im):
    mask = np.array(im.getchannel("A")) > 20
    _, parts = components(mask)
    if not parts:
        raise RuntimeError("Empty extracted sprite")
    return parts[0][0]


def normalize(im, target_area=24500):
    source = np.array(im)
    labels, parts = components(source[:, :, 3] > 20)
    if not parts:
        raise RuntimeError("Empty sprite")
    area, ident, bounds = parts[0]
    body = source.copy()
    body[labels != ident] = 0
    obj = Image.fromarray(body).crop(bounds)
    scale = min(math.sqrt(target_area / area),
                (FRAME - SAFE * 2) / obj.width,
                (FRAME - SAFE * 2) / obj.height)
    nw = max(1, round(obj.width * scale)); nh = max(1, round(obj.height * scale))
    obj = obj.resize((nw, nh), Image.Resampling.LANCZOS)
    arr = np.array(obj)
    arr[arr[:, :, 3] <= 2] = 0
    obj = Image.fromarray(arr)
    out = Image.new("RGBA", (FRAME, FRAME))
    x = (FRAME - nw) // 2
    y = FRAME - SAFE - nh
    out.alpha_composite(obj, (x, y))

    # Floating speech bubbles and mood symbols are retained as source pixels,
    # but safely positioned independently. A remote Zzz bubble must not shrink
    # the sleeping wolf or force a different size for its entire age pack.
    extras = source.copy()
    extras[labels == ident] = 0
    extras = Image.fromarray(extras)
    eb = extras.getchannel("A").getbbox()
    if eb:
        patch = extras.crop(eb)
        ew = max(1, round(patch.width * scale))
        eh = max(1, round(patch.height * scale))
        limit = min(1.0, (FRAME - 2 * SAFE) / ew, (FRAME - 2 * SAFE) / eh)
        ew = max(1, round(ew * limit)); eh = max(1, round(eh * limit))
        patch = patch.resize((ew, eh), Image.Resampling.LANCZOS)
        ex = round(x + (eb[0] - bounds[0]) * scale)
        ey = round(y + (eb[1] - bounds[1]) * scale)
        ex = max(SAFE, min(FRAME - SAFE - ew, ex))
        ey = max(SAFE, min(FRAME - SAFE - eh, ey))
        out.alpha_composite(patch, (ex, ey))
    return out


def without_scratcher(im, age):
    """Keep the supplied wolf and its forepaws, excluding the fixed prop.

    The paths follow the visible right-hand outline of the wolf in each object
    sheet. No body part or absent animation frame is synthesized.
    """
    # Paths are relative to that age's scratcher source cell. Updated only after
    # visual comparison against the original at full size.
    outlines = {
        "cub": [(0, 0), (229, 0), (229, 64), (235, 65), (241, 66),
                (245, 72), (243, 76), (249, 76), (258, 80), (263, 87),
                (263, 101), (259, 108), (248, 118), (235, 128), (229, 136),
                (220, 143), (207, 148), (210, 162), (207, 171), (202, 176),
                (197, 179), (186, 182), (183, 186), (184, 191), (190, 194),
                (196, 196), (199, 200), (200, 208), (198, 213), (191, 217),
                (178, 218), (151, 218), (145, 229), (0, 229)],
        "teen": [(0, 0), (229, 0), (229, 64), (235, 65), (241, 66),
                 (245, 72), (243, 76), (249, 76), (258, 80), (263, 87),
                 (263, 101), (259, 108), (248, 118), (235, 128), (229, 136),
                 (220, 143), (207, 148), (210, 162), (207, 171), (202, 176),
                 (197, 179), (186, 182), (183, 186), (184, 191), (190, 194),
                 (196, 196), (199, 200), (200, 208), (198, 213), (191, 217),
                 (178, 218), (151, 218), (145, 229), (0, 229)],
        "adult": [(0, 0), (227, 0), (228, 53), (230, 56), (229, 64),
                  (235, 67), (243, 69), (245, 74), (243, 79), (249, 80),
                  (257, 85), (261, 91), (262, 99), (260, 110), (254, 119),
                  (243, 129), (235, 141), (228, 150), (218, 159), (210, 162),
                  (205, 166), (207, 176), (208, 185), (207, 197), (204, 203),
                  (198, 211), (192, 214), (182, 216), (184, 222), (192, 222),
                  (199, 226), (201, 233), (201, 240), (197, 244), (186, 247), (0, 260)],
        "old": [(0, 0), (222, 0), (222, 64), (230, 68), (235, 72),
                (234, 78), (242, 79), (249, 85), (253, 94), (251, 107),
                (247, 112), (237, 123), (226, 133), (218, 141), (210, 147),
                (204, 151), (201, 155), (204, 165), (205, 176), (203, 183),
                (198, 190), (192, 195), (183, 198), (177, 200), (184, 206),
                (185, 215), (181, 221), (170, 223), (153, 221), (150, 244), (0, 244)],
    }
    mask = Image.new("L", im.size)
    ImageDraw.Draw(mask).polygon(outlines[age], fill=255)
    arr = np.array(im)
    arr[np.array(mask) == 0] = 0
    labels, pieces = components(arr[:, :, 3] > 20)
    if pieces:
        arr[labels != pieces[0][1]] = 0
    return Image.fromarray(arr)


def head_token(sheet, age):
    boxes = {"cub": (57, 104, 165, 201), "teen": (66, 99, 159, 184),
             "adult": (54, 99, 157, 198), "old": (46, 99, 159, 206)}
    # First isolate the full, closed character silhouette. Cropping a raw
    # head first opens the white muzzle at the lower edge, making an ordinary
    # edge flood-fill incorrectly classify that white fur as background.
    full_box = MAIN[age]["idle_down"][0]
    full = extract(sheet, full_box)
    b = boxes[age]
    head = full.crop((b[0] - full_box[0], b[1] - full_box[1],
                      b[2] - full_box[0], b[3] - full_box[1]))
    w, h = head.size
    mask = Image.new("L", head.size)
    ImageDraw.Draw(mask).polygon([(0, 0), (w, 0), (w, h*.72), (w*.89, h*.82),
                                 (w*.74, h*.92), (w*.53, h), (w*.34, h*.96),
                                 (w*.14, h*.84), (0, h*.72)], fill=255)
    arr = np.array(head)
    arr[np.array(mask) == 0] = 0
    return normalize(Image.fromarray(arr), 30000), boxes[age]


def save(im, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    # An interrupted encoder must never replace a valid resource with a zero-
    # byte file. Verify the complete temporary image before its atomic rename.
    staging = ROOT / "build" / "wolf-assets-staging"
    staging.mkdir(parents=True, exist_ok=True)
    temporary = staging / (path.name + ".tmp")
    try:
        if path.suffix == ".webp":
            im.save(temporary, "WEBP", lossless=True, method=6, exact=True)
        else:
            im.save(temporary, "PNG", optimize=False)
        with Image.open(temporary) as check:
            check.load()
            if check.size != im.size or "A" not in check.getbands():
                raise RuntimeError(f"Invalid encoded resource: {path}")
        temporary.replace(path)
    finally:
        if temporary.exists():
            temporary.unlink()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--qa-dir", type=Path, help="Optional scratch previews for visual review")
    args = parser.parse_args()
    metadata = json.loads((SOURCE / "sources.json").read_text())
    manifest = {"version": "0.8.6", "species": "wolf", "frame_size": FRAME,
                "safe_margin": SAFE, "sources": metadata, "assets": {}, "portraits": {},
                "notes": ["Eight original user PNG files are preserved byte for byte.",
                          "All source checkerboards are printed pixels, removed by edge-connected masking.",
                          "LEFT idle is mirrored because the source labelled LEFT faces right.",
                          "Run uses the six supplied walking poses, as for leopard movement.",
                          "Rope and scratcher each have one supplied pose; engine motion animates the interaction.",
                          "Detached bubbles and mood symbols are safely repositioned after body normalization.",
                          "No drawing is shared across age packs or between species."]}
    for age in AGES:
        sheets = {}
        for kind in ("main", "objects"):
            source = SOURCE / metadata[age][kind]["file"]
            digest = hashlib.sha256(source.read_bytes()).hexdigest()
            if digest != metadata[age][kind]["sha256"]:
                raise RuntimeError(f"Modified original source: {source}")
            sheets[kind] = Image.open(source).convert("RGBA")
            metadata[age][kind]["path"] = str(source.relative_to(ROOT))
            metadata[age][kind]["dimensions"] = list(sheets[kind].size)

        frames = {}
        records = {}
        for action, boxes in MAIN[age].items():
            values = [extract(sheets["main"], b, action in ("sleep", "moods")) for b in boxes]
            if action == "idle_left":
                values = [ImageOps.mirror(values[0])]
            frames[action] = values
            records[action] = {"source": "main", "source_cells": boxes,
                               "derivation": "horizontal mirror of mislabeled source idle" if action == "idle_left" else "source poses"}

        for direction in ("down", "left", "right", "up"):
            frames["run_" + direction] = frames["walk_" + direction]
            records["run_" + direction] = {**records["walk_" + direction], "derivation": "supplied walk sequence played at run cadence"}

        for action, box in zip(OBJECT_ACTIONS, OBJECTS[age]):
            if action == "bowl" or (action == "bottle" and age != "cub"):
                continue
            floor_band = round((box[3] - box[1]) * .75) if action in (
                "fetch_mouse", "fetch_ball", "fetch_tennis", "fetch_yarn", "rope_play") else None
            value = extract(sheets["objects"], box, action in ("groom_foam", "soap"), floor_band)
            if action == "scratcher_play":
                value = without_scratcher(value, age)
            frames[action] = [value]
            records[action] = {"source": "objects", "source_cells": [box],
                               "derivation": "fixed scratcher removed; one supplied wolf pose" if action == "scratcher_play" else "one supplied object interaction pose"}
            if floor_band is not None:
                records[action]["checker_shadow_cleanup_band"] = [0, floor_band, box[2] - box[0], box[3] - box[1]]

        # Use one feasible visual mass per age for every animation. Detached
        # mood/care icons do not count toward the character's mass. The lowest
        # feasible scale ensures the full drawing and its icons fit without
        # reducing only a particular mood or an object interaction.
        feasible = []
        for values in frames.values():
            for im in values:
                _, parts = components(np.array(im.getchannel("A")) > 20)
                area, _, box = parts[0]
                max_dim = max(box[2] - box[0], box[3] - box[1])
                feasible.append(area * ((FRAME - 2 * SAFE) / max_dim) ** 2)
        target_area = int(min(24500, min(feasible) * .98))
        manifest.setdefault("normalization", {})[age] = {
            "largest_component_target_area": target_area,
            "baseline": FRAME - SAFE,
            "method": "same largest-component visual mass for every pose, safe complete silhouette"}
        for action in frames:
            frames[action] = [normalize(im, target_area) for im in frames[action]]

        dst = RUNTIME / f"res-wolf-{age}" / "drawable-nodpi"
        for action, values in frames.items():
            png = action.startswith("idle_") or action.startswith("fetch_") or action == "rope_play"
            name = f"wolf_{age}_{action}." + ("png" if png else "webp")
            out = Image.new("RGBA", (FRAME * len(values), FRAME))
            for i, im in enumerate(values):
                out.alpha_composite(im, (FRAME * i, 0))
            path = dst / name
            save(out, path)
            manifest["assets"][name] = {"age": age, "action": action,
                "path": str(path.relative_to(ROOT)), "frames": len(values),
                "width": out.width, "height": out.height,
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), **records[action]}

        token, box = head_token(sheets["main"], age)
        path = dst / f"wolf_{age}_promenade_token.png"
        save(token, path)
        manifest["portraits"][path.name] = {"age": age, "path": str(path.relative_to(ROOT)),
            "width": FRAME, "height": FRAME, "frames": 1, "source": "main", "source_cells": [box],
            "derivation": "head crop of same-age idle_down", "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}

        if args.qa_dir:
            args.qa_dir.mkdir(parents=True, exist_ok=True)
            tiles = []
            for action, values in frames.items():
                for i, im in enumerate(values):
                    tiles.append((action + (f" {i+1}" if len(values) > 1 else ""), im))
            tiles.append(("promenade_token", token))
            cols = 8; tw = 160; th = 184
            preview = Image.new("RGB", (cols * tw, math.ceil(len(tiles)/cols) * th), "#d6e3dc")
            draw = ImageDraw.Draw(preview)
            for i, (label, im) in enumerate(tiles):
                x = (i % cols) * tw; y = (i // cols) * th
                patch = im.resize((tw, tw), Image.Resampling.LANCZOS)
                preview.paste(patch, (x, y), patch)
                draw.text((x+4, y+tw+3), label, fill="#143627")
            preview.save(args.qa_dir / f"wolf-{age}-all.png")
        print(f"OK wolf {age}: {len(frames)} gameplay resources + portrait; visual area {target_area}; own sources only", flush=True)

    manifest_path = ROOT / "wolf-sprite-manifest.json"
    manifest_temp = ROOT / "wolf-sprite-manifest.json.tmp"
    manifest_temp.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
    manifest_temp.replace(manifest_path)
    print("OK wolf v0.8.6: 109 gameplay resources and 4 same-age portrait tokens")


if __name__ == "__main__":
    main()
