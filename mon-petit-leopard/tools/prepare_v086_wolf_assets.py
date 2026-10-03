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
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps

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
        "walk_right": row([771, 900, 1023, 1147, 1272, 1394, 1523], 293, 425),
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
        "walk_right": row([771, 900, 1024, 1147, 1272, 1394, 1524], 293, 425),
        "walk_up": row([13, 140, 264, 389, 513, 638, 764], 471, 620),
        "jump": row([771, 920, 1069, 1219, 1369, 1524], 471, 620),
        "eat": row([14, 134, 253, 376], 668, 859),
        "sleep": row([384, 514, 645, 782], 668, 859),
        "moods": row([790, 914, 1032, 1150, 1272, 1392, 1523], 668, 778)
                 + row([790, 914, 1032, 1150, 1272, 1392, 1523], 796, 916),
    },
    "adult": {
        "idle_down": [(12, 101, 201, 275)],
        "idle_left": [(211, 101, 395, 275)],
        "idle_right": [(403, 101, 589, 275)],
        "idle_up": [(597, 101, 777, 275)],
        "walk_down": row([783, 886, 996, 1104, 1214, 1325, 1439], 101, 275),
        "walk_left": row([12, 130, 248, 366, 483, 600, 720], 320, 473),
        "walk_right": row([728, 849, 967, 1083, 1200, 1317, 1438], 320, 473),
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
        "walk_right": row([729, 851, 967, 1085, 1202, 1318, 1437], 315, 457),
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
    "cub": [(17,105,318,339),(327,105,616,339),(625,105,913,339),(923,105,1210,339),(1220,105,1521,339),
            (17,391,318,622),(327,391,616,622),(625,391,912,622),(923,391,1210,622),(1220,391,1521,622),
            (164,663,560,892),(573,671,960,892),(971,671,1363,892)],
    "teen": [(17,105,316,338),(328,105,615,338),(626,105,912,338),(924,105,1209,338),(1221,105,1520,338),
             (17,391,317,621),(328,391,615,621),(626,391,911,621),(924,391,1209,621),(1221,391,1520,621),
             (164,663,560,892),(574,672,960,892),(972,672,1362,892)],
    "adult": [(12,99,290,347),(300,99,576,347),(586,99,863,347),(872,99,1149,347),(1159,99,1436,347),
              (12,395,291,647),(301,395,577,647),(586,395,863,647),(872,395,1148,647),(1158,395,1436,647),
              (150,687,525,946),(536,695,907,946),(918,695,1295,946)],
    "old": [(16,101,299,339),(309,101,580,339),(591,101,861,339),(871,101,1141,339),(1151,101,1434,339),
            (16,388,299,639),(309,388,580,639),(590,388,861,639),(871,388,1141,639),(1151,388,1434,639),
            (157,681,531,924),(541,687,908,924),(918,687,1286,924)],
}

# Audited source-space masks. The two teen walking labels touch the dark toe
# outline, so removing an entire bottom row would also cut the toe itself.
LABEL_MASKS = {
    ("teen", "walk_down", 1): [(48,132),(54,132),(54,134),(56,135),(56,139),(54,141),(58,142),(58,145),(48,145),(48,141),(50,139),(52,137),(52,135),(48,135)],
    ("teen", "walk_down", 3): [(52,132),(55,132),(55,135),(55,139),(57,140),(57,142),(55,142),(55,145),(52,145),(52,143),(48,143),(48,140),(49,138),(50,136),(51,135)],
}

# The original pale tail outline is open by one pixel at these three tips.
# Exact inner-fur masks prevent a background flood through that source gap.
# The small paw mask protects the cream toe adjacent to the printed numeral.
FUR_PROTECT = {
    ("old", "idle_up", 0): [[(117,118),(128,114),(138,109),(143,107),(144,112),(145,116),(145,126),(143,132),(142,136),(136,137),(127,137),(115,134),(119,129),(110,127),(113,124)]],
    ("old", "walk_left", 0): [[(84,32),(89,31),(96,34),(101,38),(105,42),(109,47),(111,54),(107,57),(99,56),(93,52),(88,54),(84,50),(83,43),(83,37)]],
    ("teen", "walk_down", 1): [[(57,128),(62,126),(69,128),(71,131),(69,134),(65,135),(61,134),(57,132)]],
    ("teen", "walk_down", 3): [[(43,0),(47,0),(50,4),(55,7),(59,11),(60,14),(60,18),(57,22),(51,25),(48,22),(42,20),(39,15),(35,13),(35,9),(38,5)],[(57,128),(62,127),(68,129),(68,133),(65,135),(61,135),(57,133)]],
    ("teen", "walk_right", 2): [[(80,86),(84,85),(89,88),(91,90),(91,94),(93,97),(96,100),(96,103),(93,105),(88,104),(83,101),(83,96),(80,94),(78,92)]],
}

# The flat grey marks below the feet are painted over the printed checkerboard.
# These are source-space ground bands checked pose by pose, not a grey colour
# key for the wolf. Enclosed light fur is never seeded as background.
MAIN_GROUND = {
    "cub": {"walk_down": 106, "walk_left": 100, "walk_right": 100, "walk_up": 115, "jump": 108},
    "teen": {"walk_down": 113, "walk_left": 98, "walk_right": 98, "walk_up": 112, "jump": 110},
    "adult": {"walk_down": 132, "walk_left": 110, "walk_right": 110, "walk_up": 125, "jump": 122},
    "old": {"walk_down": 128, "walk_left": 106, "walk_right": 106, "walk_up": 114, "jump": 112},
}
TOY_ACTIONS = ("fetch_mouse", "fetch_ball", "fetch_tennis", "fetch_yarn", "rope_play")
TOY_HOLE = {"cub": (78,170), "teen": (78,170), "adult": (75,185), "old": (75,185)}

# Some care sheets tint the checkerboard blue between actual soap bubbles.
# Each local zone is outside/right of the face. Explicit ellipse interiors
# protect the supplied bubbles; their source colours are never repainted.
# Tuples are (centre x, centre y, horizontal radius, vertical radius).
CARE_BLUE = {
    ('cub', 'soap'): ((235, 25, 310, 156), [[275.78, 42.86, 4.74, 4.74], [280.42, 62.13, 12.92, 12.92], [268.04, 85.42, 5.43, 5.43], [289.95, 111.8, 12.54, 12.54], [293.16, 146.65, 7.21, 7.21], [274.61, 137.61, 3.26, 3.26], [285, 130, 3, 3]]),
    ('teen', 'soap'): ((234, 25, 308, 156), [[273, 47.5, 4.5, 4.5], [276.32, 66.99, 12.45, 12.45], [265.5, 87.8, 5, 5], [285.54, 112.07, 10.59, 10.59], [290.5, 150, 6.7, 6.7], [273.0, 137.0, 4.0, 4.0]]),
    ('adult', 'soap'): ((230, 30, 304, 155), [[261.56, 48.58, 5.31, 5.31], [268.3, 70.56, 12.14, 12.14], [255, 85.5, 5.2, 5.2], [277.54, 107.19, 10.83, 10.83], [281.75, 143.52, 7.27, 7.27], [267.17, 126.04, 4.03, 4.03], [272.92, 159.83, 5.17, 5.17], [282.38, 192.19, 11.83, 11.83]]),
    ('old', 'soap'): ((226, 30, 301, 166), [[263.5, 45.5, 5.2, 5.2], [268.4, 66.7, 13.78, 13.78], [279.34, 117.19, 12.56, 12.56], [285.43, 159.48, 8.15, 8.15], [276.76, 175.33, 6.09, 6.09]]),
    ('cub', 'groom_foam'): ((202, 35, 266, 165), [[234.12, 58.15, 5.43, 5.43], [235.41, 78.27, 10.07, 10.07], [214.83, 106.96, 12.96, 12.96], [235.72, 142.87, 14.1, 14.1], [256.52, 130.78, 5.56, 5.56], [231.5, 167.5, 5.5, 5.5]]),
    ('teen', 'groom_foam'): ((201, 35, 263, 165), [[232.7, 60.7, 4.8, 4.8], [233.7, 78.7, 10.4, 10.4], [213.3, 107.3, 13, 13], [233.3, 142.7, 14, 14], [254.7, 131, 5.4, 5.4], [228, 166.7, 5.3, 5.3]]),
    ('adult', 'groom_foam'): ((202, 42, 272, 177), [[228.26, 68.38, 6.02, 6.02], [217.34, 90.6, 12.45, 12.45], [234.18, 171.86, 11.07, 11.07], [244.8, 157.22, 3.92, 3.92], [245.7, 187, 4.7, 4.7], [252.32, 210.43, 6.82, 6.82]]),
    ('old', 'groom_foam'): ((200, 48, 258, 177), [[231.2, 72.41, 4.63, 4.63], [231.22, 92.1, 10.17, 10.17], [210.92, 119.39, 12.05, 12.05], [226.91, 148.38, 12.02, 12.02], [244.5, 137.3, 4.4, 4.4]]),
}

# The softly outlined foam clusters contain intentional white, unlike the
# tinted checkerboard between them. Polygons stay inside the supplied clumps.
CARE_FOAM = {
    ("cub", "soap"): [(244,160),(253,157),(265,157),(274,161),(279,172),(275,181),(282,182),(288,191),(285,198),(278,201),(267,194),(254,187),(251,177)],
    ("teen", "soap"): [(243,160),(253,156),(264,157),(272,162),(275,171),(273,180),(283,184),(285,194),(279,199),(267,192),(255,187),(250,176)],
    ("adult", "soap"): [(238,164),(247,160),(251,157),(259,157),(265,162),(266,170),(270,175),(265,183),(263,195),(259,206),(255,211),(247,195),(244,178)],
    ("old", "soap"): [(232,170),(243,166),(247,166),(250,169),(258,169),(265,174),(265,183),(273,185),(274,191),(270,198),(282,201),(285,210),(279,216),(272,216),(260,208),(249,197),(247,188)],
    ("cub", "groom_foam"): [(195,171),(204,167),(211,168),(215,174),(220,176),(222,183),(218,189),(225,192),(230,201),(233,207),(229,214),(213,215),(207,207),(202,192)],
    ("teen", "groom_foam"): [(194,171),(203,167),(210,168),(214,174),(219,177),(221,185),(218,191),(225,194),(230,202),(232,210),(226,215),(212,215),(207,207),(201,192)],
    ("adult", "groom_foam"): [(211,179),(218,178),(223,182),(225,189),(233,191),(237,198),(237,207),(241,216),(248,224),(247,233),(240,240),(227,240),(221,230),(216,214),(210,196)],
    ("old", "groom_foam"): [(191,184),(197,179),(203,181),(211,178),(216,181),(217,188),(222,190),(224,195),(221,200),(229,202),(230,211),(226,216),(233,220),(235,227),(236,229),(236,234),(232,238),(217,238),(211,228),(205,215),(201,200)],
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



def shifted(a, dy, dx, fill=0):
    """Read a neighbouring array without wrapping its opposite edge."""
    out = np.full_like(a, fill)
    h, w = a.shape[:2]
    sy0, sy1 = max(0, dy), min(h, h + dy)
    sx0, sx1 = max(0, dx), min(w, w + dx)
    out[max(0, -dy):min(h, h - dy), max(0, -dx):min(w, w - dx)] = a[sy0:sy1, sx0:sx1]
    return out

def unmatte_exterior(im, original_rgb, protected):
    """Remove checkerboard colour from the one-pixel exterior antialias ring.

    The source outline was blended against an opaque checkerboard. Estimate the
    original dark outline only from neighbouring pixels in the inward direction,
    and estimate the checker colour only from already removed neutral pixels.
    Interior pixels and explicit fur/foam protection masks remain byte-identical.
    """
    a = np.array(im)
    rgb = original_rgb.astype(float)
    mask = a[:, :, 3] > 0
    labels, parts = components(mask)
    body = labels == parts[0][1]
    outside = ~mask
    h, w = mask.shape
    queue = deque()
    exterior = np.zeros_like(mask)
    edges = ([(0, x) for x in range(w)] + [(h - 1, x) for x in range(w)]
             + [(y, 0) for y in range(h)] + [(y, w - 1) for y in range(h)])
    for y, x in edges:
        if outside[y, x] and not exterior[y, x]:
            exterior[y, x] = True
            queue.append((y, x))
    while queue:
        y, x = queue.popleft()
        for ny, nx in neighbors(y, x, h, w):
            if outside[ny, nx] and not exterior[ny, nx]:
                exterior[ny, nx] = True
                queue.append((ny, nx))
    expanded = Image.fromarray((exterior * 255).astype("uint8")).filter(ImageFilter.MaxFilter(3))
    border = body & (np.array(expanded) > 0)
    inward_x = np.zeros_like(mask, float)
    inward_y = inward_x.copy()
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            neighbour = shifted(exterior, dy, dx)
            inward_x -= neighbour * dx
            inward_y -= neighbour * dy

    best_brightness = np.full(mask.shape, 999.)
    foreground = np.zeros_like(rgb)
    background_sum = np.zeros_like(rgb)
    count = np.zeros(mask.shape, float)
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            if dy == dx == 0:
                continue
            colour = shifted(rgb, dy, dx)
            valid = shifted(body, dy, dx)
            brightness = colour.mean(axis=2)
            aligned = ((dx * inward_x + dy * inward_y > 0)
                       | ((inward_x == 0) & (inward_y == 0)))
            better = valid & aligned & (brightness < best_brightness)
            best_brightness[better] = brightness[better]
            foreground[better] = colour[better]
            neutral_background = (shifted(exterior, dy, dx)
                                  & (colour.min(axis=2) >= 185)
                                  & (colour.max(axis=2) - colour.min(axis=2) <= 20))
            weight = 1 / (abs(dx) + abs(dy))
            background_sum += colour * (neutral_background * weight)[:, :, None]
            count += neutral_background * weight

    background = background_sum / np.maximum(count, 1e-8)[:, :, None]
    vector = background - foreground
    opacity = (np.sum((background - rgb) * vector, axis=2)
               / np.maximum(np.sum(vector * vector, axis=2), 1))
    candidates = (border & ~protected & (rgb.min(axis=2) > 125)
                  & (rgb.max(axis=2) - rgb.min(axis=2) < 35)
                  & (best_brightness < 120) & (count > 0)
                  & (rgb.mean(axis=2) > best_brightness + 25) & (opacity < .95))
    opacity = np.clip(opacity, .001, 1)
    colour = np.clip((rgb - (1 - opacity)[:, :, None] * background)
                     / opacity[:, :, None], 0, 255)
    a[candidates, :3] = np.rint(colour[candidates]).astype("uint8")
    a[candidates, 3] = np.rint(opacity[candidates] * 255).astype("uint8")
    a[candidates & (a[:, :, 3] < 12)] = 0
    return Image.fromarray(a)


def extract(sheet, box, effects=False, floor_band=None, erase_polygon=None, hole_seeds=(), care_blue=None, care_foam=None, protected_polygons=(), protect_closed_floor=False):
    """Remove only edge-connected neutral checkerboard, preserving white fur.

    White eyes, teeth, eyebrows and fur enclosed by the source drawing remain
    opaque. A global white colour key would erase these important details.
    """
    rgba = np.array(sheet.crop(box).convert("RGBA"))
    rgb = rgba[:, :, :3].astype(np.int16)
    high = rgb.max(axis=2)
    low = rgb.min(axis=2)
    neutral_light = (high - low <= 20) & (low >= 185)
    matte_protected = np.zeros(neutral_light.shape, bool)
    green_frame = (rgb[:, :, 1] > rgb[:, :, 0] + 4) & (rgb[:, :, 1] > rgb[:, :, 2] + 8)
    # Printed green titles/borders may touch the crop edge. Interior green
    # question marks and the tennis ball are part of the supplied drawing.
    yy, xx = np.indices(neutral_light.shape)
    if care_blue is not None:
        zone, ellipses = care_blue
        x0,y0,x1,y1 = zone
        local = (xx >= x0) & (xx < x1) & (yy >= y0) & (yy < y1)
        protected = np.zeros(neutral_light.shape, bool)
        for cx,cy,rx,ry in ellipses:
            protected |= ((xx-cx)/rx)**2 + ((yy-cy)/ry)**2 <= 1
        if care_foam:
            foam = Image.new("L", (rgba.shape[1], rgba.shape[0]))
            ImageDraw.Draw(foam).polygon(care_foam, fill=255)
            protected |= np.array(foam) > 0
        blue_checker = (low >= 160) & (rgb[:,:,2] >= rgb[:,:,0]-3) & (rgb[:,:,1] >= rgb[:,:,0]-5)
        neutral_light |= local & blue_checker & ~protected
        neutral_light[protected] = False
    if floor_band is not None:
        # The floor shadow is printed over checkerboard on the toy poses.
        # Limit this extra key to the annotated ground band below the belly;
        # it never changes face, ear, eyebrow or tail extraction thresholds.
        neutral_light |= (yy >= floor_band) & (high - low <= 10) & (low >= 105)
    if protect_closed_floor and floor_band is not None:
        # A few pale toe outlines have a one-pixel gap. Seal only that topology
        # when deciding which light pixels belong INSIDE the drawing. No RGB
        # pixel is changed or synthesized, and exposed ground stays outside.
        paint = (high-low > 12) | (low < 130)
        paint[(xx < 2) | (xx >= rgba.shape[1]-2) | (yy < 1) | (yy >= rgba.shape[0]-1)] = False
        sealed = Image.fromarray((paint*255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
        open_space = ~np.array(sealed).astype(bool)
        outside = np.zeros_like(open_space)
        q = deque([(int(y),int(x)) for y,x in zip(*np.nonzero(open_space & ((yy == 0)|(xx == 0)|(yy == rgba.shape[0]-1)|(xx == rgba.shape[1]-1))))])
        for y,x in q: outside[y,x]=True
        while q:
            y,x=q.popleft()
            for ny,nx in neighbors(y,x,*open_space.shape):
                if open_space[ny,nx] and not outside[ny,nx]:
                    outside[ny,nx]=True; q.append((ny,nx))
        # Protect only the audited ground-height part of the original drawing.
        # Added closing pixels are not foreground: otherwise the mask could
        # bridge an isolated printed frame number to a toe, or keep checker
        # pixels between hairs elsewhere on the body.
        enclosed_original = ~outside & (~np.array(sealed).astype(bool) | paint)
        neutral_light[enclosed_original & (yy >= floor_band)] = False
    if care_blue is not None:
        neutral_light[protected] = False
        matte_protected |= protected
    border = (yy < 8) | (yy >= neutral_light.shape[0] - 4) | (xx < 2) | (xx >= neutral_light.shape[1] - 2)
    forced = np.zeros(neutral_light.shape, bool)
    if erase_polygon:
        mask = Image.new("L", (rgba.shape[1], rgba.shape[0]))
        ImageDraw.Draw(mask).polygon(erase_polygon, fill=255)
        forced = np.array(mask) > 0
    candidate = neutral_light | (green_frame & border) | forced
    if protected_polygons:
        protection = Image.new("L", (rgba.shape[1], rgba.shape[0]))
        draw = ImageDraw.Draw(protection)
        for polygon in protected_polygons:
            draw.polygon(polygon, fill=255)
        candidate[np.array(protection) > 0] = False
        matte_protected |= np.array(protection) > 0
    h, w = candidate.shape
    background = np.zeros((h, w), bool)
    queue = deque()
    for y, x in [(0, x) for x in range(w)] + [(h-1, x) for x in range(w)] \
                + [(y, 0) for y in range(h)] + [(y, w-1) for y in range(h)] \
                + [(sy, sx) for sx, sy in hole_seeds]:
        if candidate[y, x] and not background[y, x]:
            background[y, x] = True
            queue.append((y, x))
    while queue:
        y, x = queue.popleft()
        for ny, nx in neighbors(y, x, h, w):
            if candidate[ny, nx] and not background[ny, nx]:
                background[ny, nx] = True
                queue.append((ny, nx))
    # Closed white regions are never classified globally. The original white
    # tail tips can have the same grey/white histogram as a checkerboard. Only
    # explicitly audited background seed locations above may open a hole.
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
    return unmatte_exterior(Image.fromarray(rgba), rgb, matte_protected)


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


def extract_main_pose(sheet, age, action, frame_index):
    """Public source-space extraction entry point, also used by pixel probes."""
    return extract(sheet, MAIN[age][action][frame_index], action in ("sleep", "moods"),
                   MAIN_GROUND[age].get(action), LABEL_MASKS.get((age, action, frame_index)),
                   protected_polygons=FUR_PROTECT.get((age, action, frame_index), ()), protect_closed_floor=True)


def object_ground(age, action):
    box = OBJECTS[age][OBJECT_ACTIONS.index(action)]
    height = box[3] - box[1]
    if action in TOY_ACTIONS:
        return round(height * .75)
    # Audited bottom strips of seated care/plush poses contain a printed grey
    # floor underneath the feet and tail, separate from the coloured drawing.
    if action in ("bottle", "comb", "fetch_plush", "groom_foam", "soap", "towel"):
        return height - 40
    return None


def extract_object_pose(sheet, age, action):
    box = OBJECTS[age][OBJECT_ACTIONS.index(action)]
    seeds = [TOY_HOLE[age]] if action in TOY_ACTIONS else []
    value = extract(sheet, box, action in ("groom_foam", "soap"),
                    object_ground(age, action), hole_seeds=seeds,
                    care_blue=CARE_BLUE.get((age, action)), care_foam=CARE_FOAM.get((age, action)))
    return without_scratcher(value, age) if action == "scratcher_play" else value


def head_token(sheet, age):
    boxes = {"cub": (57, 104, 165, 201), "teen": (66, 99, 159, 184),
             "adult": (54, 99, 157, 198), "old": (46, 99, 159, 206)}
    # First isolate the full, closed character silhouette. Cropping a raw
    # head first opens the white muzzle at the lower edge, making an ordinary
    # edge flood-fill incorrectly classify that white fur as background.
    full_box = MAIN[age]["idle_down"][0]
    full = extract_main_pose(sheet, age, "idle_down", 0)
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
                          "v0.8.7 cutout audit: printed borders/digits, annotated grey ground and blue checker gaps are excluded; closed white fur is preserved.",
                          "Only the one-source-pixel exterior antialias ring is unmatted against adjacent checker colours; interior RGB is unchanged.",
                          "v0.8.8: the cub's first supplied sleeping pose has two tails; its runtime frame aliases the complete third pose from the same source sheet, including the sleep bubble.",
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
            values = [extract_main_pose(sheets["main"], age, action, i) for i in range(len(boxes))]
            if action == "idle_left":
                values = [ImageOps.mirror(values[0])]
            frames[action] = values
            records[action] = {"source": "main", "source_cells": boxes,
                               "derivation": "horizontal mirror of mislabeled source idle" if action == "idle_left" else "source poses"}
            if action in MAIN_GROUND[age]:
                records[action]["checker_shadow_cleanup_bands"] = [[0, MAIN_GROUND[age][action], b[2]-b[0], b[3]-b[1]] for b in boxes]
            label_masks = {str(i+1): LABEL_MASKS[(age, action, i)] for i in range(len(boxes)) if (age, action, i) in LABEL_MASKS}
            if label_masks:
                records[action]["printed_label_masks"] = label_masks
            protected_fur = {str(i+1): FUR_PROTECT[(age, action, i)] for i in range(len(boxes)) if (age, action, i) in FUR_PROTECT}
            if protected_fur:
                records[action]["protected_source_fur_polygons"] = protected_fur

        for direction in ("down", "left", "right", "up"):
            frames["run_" + direction] = frames["walk_" + direction]
            records["run_" + direction] = {**records["walk_" + direction], "derivation": "supplied walk sequence played at run cadence"}

        for action, box in zip(OBJECT_ACTIONS, OBJECTS[age]):
            if action == "bowl" or (action == "bottle" and age != "cub"):
                continue
            floor_band = object_ground(age, action)
            value = extract_object_pose(sheets["objects"], age, action)
            frames[action] = [value]
            records[action] = {"source": "objects", "source_cells": [box],
                               "derivation": "fixed scratcher removed; one supplied wolf pose" if action == "scratcher_play" else "one supplied object interaction pose"}
            if floor_band is not None:
                records[action]["checker_shadow_cleanup_band"] = [0, floor_band, box[2] - box[0], box[3] - box[1]]
            if action in TOY_ACTIONS:
                records[action]["audited_background_seed"] = list(TOY_HOLE[age])
            if (age, action) in CARE_BLUE:
                records[action]["blue_checker_cleanup"] = {"zone": CARE_BLUE[(age, action)][0], "protected_source_bubble_ellipses": CARE_BLUE[(age, action)][1], "protected_source_foam_polygon": CARE_FOAM[(age, action)]}

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

        if age == "cub":
            # The original first sleeping drawing itself contains two tails.
            # Reuse the complete, correct third drawing from this same-age
            # sheet after normalization. Keep its bubble and every source
            # detail; do not guess a hidden back contour by erasing a tail.
            frames["sleep"][0] = frames["sleep"][2].copy()
            records["sleep"]["source_cells"] = [MAIN[age]["sleep"][2],
                                                *MAIN[age]["sleep"][1:]]
            records["sleep"]["derivation"] = (
                "runtime frame 1 is an exact copy of normalized frame 3 from "
                "the same-age original sheet; the original frame 1 has two tails")
            records["sleep"]["runtime_frame_aliases"] = {"1": 3}
            records["sleep"]["excluded_source_cells"] = [{
                "frame": 1, "box": MAIN[age]["sleep"][0],
                "reason": "The original sleeping drawing contains two tails."}]

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
