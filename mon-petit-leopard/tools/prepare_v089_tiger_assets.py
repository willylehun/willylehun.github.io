#!/usr/bin/env python3
"""Import the eight original tiger sheets into four independent age packs.

The originals are opaque presentation sheets with a printed checkerboard.
This is a source-preserving atlas extraction: no drawing is generated.  Only
the audited source cells, background topology and normalization are used.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageOps

# Reuse pure, source-independent extraction primitives, without changing the
# wolf importer's module state, source coordinates or output resources.
from prepare_v086_wolf_assets import components, extract, normalize

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source-assets" / "tiger-v089"
RUNTIME = ROOT / "app" / "src" / "main"
AGES = ("cub", "teen", "adult", "old")
FRAME = 256
SAFE = 16


def row(bounds, top, bottom):
    return [(bounds[i] + 2, top, bounds[i + 1] - 2, bottom)
            for i in range(len(bounds) - 1)]


# Coordinates belong to the unchanged 1536x1024 tiger originals.  Adult/old
# sheets have different row heights and must never use wolf crop positions.
MAIN = {
    "cub": {
        "idle_down": [(13, 98, 211, 253)],
        "idle_left": [(220, 98, 411, 253)],
        "idle_right": [(418, 98, 613, 253)],
        "idle_up": [(621, 98, 811, 253)],
        "walk_down": row([817, 932, 1048, 1166, 1281, 1401, 1525], 98, 252),
        "walk_left": row([13, 140, 262, 386, 511, 635, 763], 295, 427),
        "walk_right": row([770, 899, 1023, 1146, 1272, 1394, 1524], 295, 427),
        "walk_up": row([13, 140, 262, 387, 511, 635, 764], 471, 620),
        "jump": row([770, 917, 1067, 1217, 1370, 1523], 471, 620),
        "eat": row([13, 133, 255, 375], 664, 857),
        "sleep": row([383, 511, 643, 781], 664, 857),
        "moods": row([788, 913, 1033, 1150, 1270, 1392, 1524], 664, 775)
                 + row([788, 913, 1033, 1150, 1270, 1392, 1524], 796, 911),
    },
    "teen": {
        "idle_down": [(13, 97, 211, 249)],
        "idle_left": [(220, 97, 411, 249)],
        "idle_right": [(419, 97, 613, 249)],
        "idle_up": [(622, 97, 811, 249)],
        "walk_down": row([818, 932, 1049, 1165, 1281, 1402, 1525], 97, 248),
        "walk_left": row([13, 140, 263, 388, 513, 635, 764], 292, 426),
        "walk_right": row([770, 900, 1023, 1147, 1272, 1394, 1524], 292, 426),
        "walk_up": row([13, 140, 263, 388, 512, 635, 764], 470, 619),
        "jump": row([770, 919, 1067, 1217, 1369, 1524], 470, 619),
        "eat": row([13, 132, 252, 377], 665, 858),
        "sleep": row([384, 512, 644, 783], 665, 858),
        "moods": row([789, 913, 1033, 1150, 1271, 1392, 1524], 665, 774)
                 + row([789, 913, 1033, 1150, 1271, 1392, 1524], 795, 911),
    },
    "adult": {
        "idle_down": [(12, 100, 213, 272)],
        "idle_left": [(222, 100, 416, 272)],
        "idle_right": [(424, 100, 620, 272)],
        "idle_up": [(627, 100, 819, 272)],
        "walk_down": row([825, 937, 1054, 1170, 1287, 1405, 1527], 100, 272),
        "walk_left": row([12, 142, 259, 386, 514, 637, 765], 313, 450),
        "walk_right": row([771, 901, 1024, 1149, 1271, 1395, 1526], 313, 450),
        "walk_up": row([12, 141, 259, 385, 510, 633, 763], 492, 653),
        "jump": row([770, 917, 1072, 1219, 1374, 1526], 492, 653),
        "eat": row([14, 131, 253, 373], 693, 885),
        "sleep": row([380, 510, 642, 782], 693, 885),
        "moods": row([788, 909, 1028, 1149, 1272, 1396, 1523], 693, 793)
                 + row([788, 909, 1028, 1149, 1272, 1396, 1523], 814, 919),
    },
    "old": {
        "idle_down": [(13, 99, 211, 269)],
        "idle_left": [(220, 99, 413, 269)],
        "idle_right": [(421, 99, 616, 269)],
        "idle_up": [(624, 99, 811, 269)],
        "walk_down": row([818, 932, 1045, 1165, 1282, 1403, 1527], 99, 269),
        "walk_left": row([13, 143, 268, 391, 514, 639, 766], 311, 447),
        "walk_right": row([772, 900, 1025, 1150, 1276, 1399, 1526], 311, 447),
        "walk_up": row([13, 143, 264, 387, 512, 637, 766], 488, 644),
        "jump": row([773, 918, 1070, 1219, 1372, 1526], 488, 644),
        "eat": row([14, 130, 255, 379], 687, 880),
        "sleep": row([386, 509, 646, 784], 687, 880),
        "moods": row([790, 909, 1029, 1153, 1276, 1395, 1526], 687, 790)
                 + row([790, 909, 1029, 1153, 1276, 1395, 1526], 809, 915),
    },
}

OBJECT_ACTIONS = ("bottle", "comb", "bowl", "fetch_mouse", "fetch_plush",
                  "fetch_ball", "fetch_tennis", "fetch_yarn", "groom_foam",
                  "rope_play", "scratcher_play", "soap", "towel")
OBJECTS = {
    "cub": [(17,105,318,339),(328,105,616,339),(625,105,913,339),(923,105,1210,339),(1220,105,1521,339),
            (17,391,318,622),(328,391,616,622),(625,391,913,622),(923,391,1210,622),(1220,391,1521,622),
            (164,671,560,892),(573,671,960,892),(971,671,1363,892)],
    "teen": [(17,105,318,339),(328,105,616,339),(625,105,913,339),(923,105,1210,339),(1220,105,1521,339),
             (17,391,318,622),(328,391,616,622),(625,391,913,622),(923,391,1210,622),(1220,391,1521,622),
             (164,671,560,892),(573,671,960,892),(971,671,1363,892)],
    "adult": [(13,95,309,323),(318,95,614,323),(623,95,915,323),(924,95,1218,323),(1228,95,1523,323),
              (13,365,309,601),(318,365,614,601),(623,365,915,601),(924,365,1218,601),(1228,365,1523,601),
              (178,645,563,885),(573,645,962,885),(971,645,1358,885)],
    "old": [(14,99,306,328),(317,99,619,328),(629,99,909,328),(919,99,1219,328),(1230,99,1521,328),
            (14,374,306,618),(317,374,617,618),(626,374,909,618),(919,374,1220,618),(1231,374,1521,618),
            (185,662,567,898),(576,662,960,898),(969,662,1351,898)],
}

MAIN_GROUND = {
    "cub": {"walk_down":112,"walk_left":95,"walk_right":95,"walk_up":112,"jump":110},
    "teen": {"walk_down":119,"walk_left":93,"walk_right":93,"walk_up":111,"jump":112},
    "adult": {"walk_down":137,"walk_left":92,"walk_right":92,"walk_up":119,"jump":118},
    "old": {"walk_down":133,"walk_left":91,"walk_right":91,"walk_up":115,"jump":122},
}
# Exact numeral outlines: printed digits touch the paw contours in these
# source cells.  The paw's final dark outline row remains outside each mask.
LABEL_MASKS = {
    ("cub", "jump", 4): [(71,134),(78,134),(78,135),(80,135),(81,137),(81,147),(71,147)],
    ("teen", "walk_down", 1): [(52,137),(63,137),(63,149),(52,149)],
    ("teen", "walk_down", 4): [(49,136),(54,136),(56,137),(58,137),(59,140),(59,149),(49,149)],
    ("teen", "walk_down", 5): [(49,137),(60,137),(60,149),(49,149)],
}
FUR_PROTECT = {}
TOY_ACTIONS = ("fetch_mouse", "fetch_ball", "fetch_tennis", "fetch_yarn", "rope_play")
TOY_HOLES = {}
CARE_BLUE = {
    ('cub', 'groom_foam'): ((0, 0, 287, 231), [(33.5, 69.5, 12.8, 12.8), (35.5, 90.5, 3.4, 4.1), (47, 109.5, 7.8, 7.8), (62.6, 120.1, 3.1, 3.1), (73.4, 24, 4.6, 6.2), (77, 18.2, 3.2, 3.2), (128.4, 12.5, 3.8, 3.8), (129.9, 18.4, 2.1, 2.1), (235, 71.8, 4.6, 4.6), (235.8, 96.2, 12.5, 12.5), (229.9, 120.1, 4.1, 5.0), (254.9, 133.0, 3.5, 3.5), (235.9, 152.0, 12.9, 12.9), (229.6, 170.4, 4.5, 4.5)]),
    ('teen', 'groom_foam'): ((0, 0, 287, 231), [(75.2, 20.0, 7.7, 7.7), (63.2, 35.3, 5.5, 5.5), (93.8, 11.2, 2.7, 3.7), (27.4, 77.1, 3.4, 3.4), (41.7, 96.1, 12.7, 12.7), (59.7, 115.0, 5.7, 5.7), (235.3, 61.0, 4.3, 4.8), (235.7, 83.0, 9.4, 9.4), (215.5, 108.1, 12.2, 12.2), (237.2, 144.1, 13.1, 13.1), (257.9, 133.7, 4.5, 4.5), (230.0, 167.0, 4.7, 4.7)]),
    ('adult', 'groom_foam'): ((0, 0, 294, 242), [(116.8, 13.1, 10.7, 10.7), (89.4, 25.3, 7.0, 7.0), (79.0, 33.4, 3.2, 3.2), (61.9, 48.6, 8.7, 8.7), (64.3, 96.9, 13.4, 13.4), (77.0, 123.3, 3.7, 4.7), (230.2, 50.1, 5.8, 5.8), (230.4, 79.0, 11.9, 11.9), (242.2, 148.1, 11.3, 11.3), (253.0, 133.2, 3.2, 3.2), (257.6, 163.0, 3.5, 4.5), (267.6, 198.0, 7.3, 7.3)]),
    ('old', 'groom_foam'): ((0, 0, 301, 244), [(61.4, 27.1, 10.7, 10.7), (53.0, 44.3, 3.0, 3.0), (47.2, 90.0, 13.6, 13.6), (61.0, 114.9, 7.9, 7.9), (251.8, 65.8, 13.4, 13.4), (228.1, 80.8, 5.6, 5.6), (228.4, 100.2, 10.0, 10.0), (239.4, 143.2, 14.8, 14.8), (269.3, 190.7, 8.5, 8.5), (261.7, 212.8, 14.1, 14.1)]),
    ('cub', 'soap'): ((0, 0, 387, 221), [(273.1, 54.1, 5.1, 5.1), (282.9, 76.0, 12.5, 12.5), (268.3, 92.6, 4.4, 5.5), (288.6, 126.0, 9.0, 9.0), (282.1, 155.6, 13.0, 13.0), (283.6, 175.7, 3.0, 3.8), (122.7, 203.9, 8.2, 8.2), (198.7, 207.6, 8.0, 8.0)]),
    ('teen', 'soap'): ((0, 0, 387, 221), [(272.7, 46.4, 4.4, 4.4), (277.4, 65.7, 13.8, 13.8), (265.2, 88.9, 5.0, 5.0), (286.1, 112.7, 10.5, 10.5), (274.3, 137.8, 3.0, 3.0), (290.4, 154.6, 6.4, 6.4), (287.7, 182.5, 8.4, 8.4), (120.3, 200.0, 8.6, 8.6), (194.6, 205.3, 7.6, 7.6), (279.6, 163.9, 7.0, 7.0)]),
    ('adult', 'soap'): ((0, 0, 389, 240), [(268.8, 44.3, 6.4, 6.4), (266.2, 63.1, 3.1, 3.3), (281.8, 82.6, 11.9, 11.9), (284.3, 109.1, 5.7, 5.7), (301.6, 157.8, 11.2, 11.2), (291.5, 185.4, 9.0, 9.0)]),
    ('old', 'soap'): ((0, 0, 384, 236), [(274.1, 52.4, 9.1, 9.1), (280.5, 95.3, 16.9, 16.9), (270.9, 121.7, 5.0, 5.0), (294.7, 152.3, 8.9, 8.9), (286.8, 177.4, 6.1, 6.1), (315.3, 209.7, 12.2, 12.2)]),
}
CARE_FOAM = {}
CARE_BOX_OVERRIDES = {('adult', 'groom_foam'): (924, 359, 1218, 601)}
for (_age, _action), _box in CARE_BOX_OVERRIDES.items():
    OBJECTS[_age][OBJECT_ACTIONS.index(_action)] = _box
CARE_PROTECT = {
    ('cub', 'groom_foam'): [[(79, 33), (86, 27), (90, 21), (90, 17), (96, 12), (104, 12), (109, 15), (113, 15), (118, 20), (118, 26), (127, 27), (130, 30), (131, 35), (126, 39), (119, 40), (116, 45), (118, 49), (116, 54), (110, 57), (101, 54), (101, 62), (99, 67), (95, 69), (92, 67), (92, 61), (88, 62), (84, 59), (82, 54), (77, 54), (75, 49), (74, 44), (78, 39)], [(101, 162), (107, 162), (111, 167), (117, 167), (120, 172), (122, 177), (119, 181), (113, 182), (110, 178), (108, 185), (102, 186), (98, 182), (97, 176), (91, 178), (90, 173), (92, 167), (97, 166)], [(80, 200), (87, 198), (93, 200), (95, 205), (94, 210), (99, 211), (101, 216), (99, 222), (93, 224), (88, 222), (83, 223), (77, 221), (72, 221), (71, 217), (71, 211), (76, 208)], [(190, 166), (197, 164), (202, 168), (205, 169), (209, 167), (216, 170), (220, 177), (219, 182), (225, 184), (226, 190), (223, 195), (231, 199), (231, 204), (237, 207), (238, 212), (235, 215), (228, 216), (223, 219), (217, 217), (220, 210), (217, 202), (208, 199), (200, 196), (197, 181)]],
    ('teen', 'groom_foam'): [[(72, 49), (76, 46), (82, 44), (82, 37), (87, 33), (90, 32), (91, 29), (96, 28), (96, 25), (102, 22), (111, 22), (116, 25), (117, 24), (122, 27), (122, 31), (125, 30), (127, 31), (127, 36), (123, 39), (119, 41), (119, 45), (113, 47), (107, 46), (103, 50), (97, 50), (95, 55), (89, 55), (89, 61), (91, 60), (96, 62), (99, 65), (98, 69), (93, 73), (87, 75), (83, 72), (80, 71), (78, 65), (74, 64), (72, 60), (68, 58), (69, 53)], [(98, 157), (105, 157), (109, 162), (115, 162), (118, 166), (117, 171), (114, 173), (116, 177), (113, 181), (107, 179), (104, 185), (98, 186), (94, 183), (91, 183), (89, 179), (86, 178), (86, 171), (91, 168), (90, 163), (94, 159)], [(81, 199), (87, 199), (92, 203), (92, 209), (99, 211), (101, 216), (98, 222), (94, 224), (90, 224), (85, 222), (80, 222), (74, 222), (71, 218), (70, 213), (73, 208), (77, 207)], [(190, 165), (197, 163), (202, 165), (206, 165), (210, 169), (211, 173), (216, 174), (220, 181), (220, 185), (225, 187), (225, 193), (220, 197), (223, 201), (228, 202), (231, 207), (231, 214), (228, 219), (222, 221), (217, 219), (219, 214), (217, 207), (211, 202), (203, 198), (199, 183)]],
    ('adult', 'groom_foam'): [[(109, 41), (112, 38), (112, 35), (116, 33), (116, 29), (121, 25), (127, 23), (133, 24), (139, 27), (144, 28), (147, 30), (152, 30), (156, 34), (157, 39), (154, 43), (150, 43), (145, 42), (140, 39), (137, 40), (138, 44), (135, 48), (130, 49), (126, 46), (122, 49), (119, 48), (119, 53), (121, 54), (120, 59), (116, 62), (112, 59), (111, 54), (108, 53), (104, 48), (105, 44)], [(103, 149), (109, 148), (112, 153), (110, 157), (106, 158), (103, 154)], [(96, 166), (100, 164), (103, 166), (105, 170), (110, 168), (117, 169), (119, 173), (117, 177), (114, 179), (116, 185), (115, 188), (111, 190), (105, 186), (105, 194), (101, 198), (96, 196), (94, 192), (90, 190), (91, 185), (92, 182), (87, 181), (85, 178), (85, 174), (90, 169)], [(102, 201), (107, 200), (109, 203), (108, 207), (104, 210), (100, 208), (100, 204)], [(231, 195), (231, 189), (234, 188), (237, 190), (239, 195), (237, 198), (235, 197), (235, 203), (232, 205), (229, 203), (228, 199)], [(230, 163), (234, 163), (239, 164), (241, 168), (239, 171), (244, 173), (244, 176), (249, 178), (251, 182), (249, 187), (247, 188), (252, 190), (255, 193), (255, 199), (252, 202), (251, 207), (247, 209), (243, 205), (241, 197), (239, 193), (235, 191), (233, 185), (228, 182)], [(243, 211), (247, 207), (250, 206), (253, 208), (254, 211), (258, 214), (261, 216), (264, 217), (265, 222), (262, 226), (259, 228), (256, 228), (255, 232), (251, 233), (247, 233), (248, 230), (246, 225), (246, 222), (241, 218)], [(143, 17), (144, 14), (146, 13), (148, 14), (149, 16), (147, 19), (145, 20), (144, 21), (143, 21)]],
    ('old', 'groom_foam'): [[(88, 35), (90, 32), (89, 28), (91, 24), (94, 23), (100, 23), (103, 19), (108, 17), (113, 19), (117, 24), (120, 24), (123, 27), (123, 29), (127, 29), (130, 32), (130, 36), (127, 40), (122, 41), (118, 40), (117, 45), (111, 47), (106, 45), (105, 48), (101, 48), (103, 56), (100, 61), (96, 62), (94, 65), (89, 65), (86, 61), (81, 62), (80, 59), (80, 55), (82, 51), (79, 48), (79, 43), (83, 39), (86, 38)], [(78, 146), (83, 145), (83, 141), (87, 140), (91, 143), (92, 148), (94, 151), (98, 151), (100, 155), (97, 159), (95, 160), (96, 164), (93, 168), (89, 169), (86, 166), (84, 171), (80, 172), (77, 169), (77, 166), (73, 165), (71, 161), (72, 157), (77, 155), (76, 150)], [(86, 199), (87, 195), (91, 196), (96, 194), (99, 198), (99, 203), (97, 208), (101, 210), (100, 217), (106, 221), (109, 225), (108, 233), (104, 235), (100, 235), (96, 231), (95, 235), (89, 235), (85, 233), (81, 236), (76, 236), (72, 233), (69, 233), (65, 230), (62, 232), (58, 231), (57, 227), (53, 226), (52, 223), (57, 220), (61, 218), (61, 214), (68, 212), (68, 208), (74, 206), (74, 202), (77, 199), (82, 199)], [(217, 117), (224, 116), (229, 120), (231, 125), (230, 129), (234, 133), (234, 143), (231, 148), (232, 152), (231, 156), (236, 158), (237, 163), (244, 163), (249, 168), (250, 172), (247, 178), (243, 180), (247, 184), (247, 188), (244, 193), (250, 197), (249, 201), (245, 204), (249, 208), (251, 210), (250, 216), (254, 220), (255, 225), (252, 230), (248, 232), (243, 231), (239, 228), (236, 232), (231, 234), (236, 227), (235, 220), (231, 215), (220, 209), (218, 188), (218, 169), (213, 163), (211, 151), (216, 138)], [(278, 215), (282, 219), (282, 223), (279, 227), (271, 230), (265, 228), (267, 219)], [(190, 45), (197, 47), (201, 49), (199, 50), (202, 52), (206, 56), (202, 55), (207, 59), (211, 64), (208, 63), (211, 67), (213, 71), (211, 71), (212, 75), (213, 79), (210, 77), (211, 82), (211, 85), (209, 83), (209, 88), (207, 87), (206, 91), (204, 89), (202, 93), (199, 92), (196, 94), (192, 94), (195, 90), (197, 86), (197, 81), (198, 78), (197, 73), (197, 68), (193, 60)]],
    ('cub', 'soap'): [[(246, 157), (251, 159), (254, 160), (256, 155), (260, 155), (264, 160), (268, 160), (273, 164), (275, 168), (274, 174), (269, 178), (274, 181), (281, 182), (284, 188), (281, 194), (277, 197), (270, 198), (265, 194), (263, 190), (255, 190), (251, 178)]],
    ('teen', 'soap'): [[(256, 164), (257, 159), (261, 156), (266, 156), (269, 159), (271, 163), (275, 165), (276, 169), (273, 173), (269, 175), (273, 178), (277, 179), (277, 182), (281, 184), (284, 189), (283, 194), (279, 197), (273, 198), (269, 196), (270, 191), (267, 186), (263, 182), (262, 177), (258, 173)]],
    ('adult', 'soap'): [],
    ('old', 'soap'): [[(268, 174), (269, 171), (274, 170), (279, 172), (281, 177), (280, 181), (284, 184), (289, 185), (293, 190), (291, 196), (287, 199), (289, 202), (289, 207), (293, 210), (294, 215), (291, 221), (285, 223), (280, 224), (275, 224), (272, 220), (270, 213), (267, 210), (265, 204), (260, 201), (260, 195), (264, 190), (261, 185), (261, 180), (264, 176)]],
}
# Audited outlines retain ears/forepaws and exclude the separately drawn room prop.
SCRATCHER_CROPS = {'cub': (164, 671, 560, 892),
 'teen': (164, 660, 560, 892),
 'adult': (178, 637, 563, 885),
 'old': (185, 657, 567, 898)}
SCRATCHER_HEADERS = {'cub': 0, 'teen': 12, 'adult': 10, 'old': 7}
SCRATCHER_HOLES = {'cub': [(211, 156)],
 'teen': [(211, 161), (197, 191)],
 'adult': [(216, 182), (191, 210)],
 'old': [(221, 167)]}
SCRATCHER_OUTLINES = {'cub': [(0, 0),
         (226, 0),
         (226, 45),
         (229, 49),
         (230, 53),
         (229, 57),
         (231, 59),
         (232, 64),
         (230, 69),
         (231, 72),
         (229, 75),
         (235, 73),
         (241, 74),
         (248, 78),
         (252, 83),
         (255, 90),
         (255, 96),
         (253, 100),
         (250, 104),
         (246, 109),
         (242, 114),
         (238, 119),
         (234, 124),
         (230, 129),
         (225, 134),
         (218, 138),
         (209, 142),
         (202, 145),
         (200, 157),
         (194, 166),
         (190, 173),
         (191, 221),
         (0, 221)],
 'teen': [(0, 0),
          (238, 0),
          (238, 48),
          (241, 50),
          (240, 57),
          (238, 60),
          (240, 63),
          (239, 66),
          (237, 68),
          (244, 67),
          (250, 69),
          (255, 72),
          (259, 77),
          (262, 83),
          (263, 89),
          (262, 94),
          (259, 99),
          (255, 104),
          (250, 109),
          (246, 114),
          (242, 120),
          (240, 126),
          (235, 132),
          (229, 138),
          (222, 143),
          (212, 147),
          (204, 150),
          (200, 160),
          (196, 166),
          (196, 187),
          (200, 201),
          (200, 232),
          (0, 232)],
 'adult': [(0, 0),
           (234, 0),
           (234, 48),
           (235, 51),
           (233, 55),
           (231, 59),
           (236, 58),
           (242, 59),
           (247, 62),
           (251, 66),
           (249, 69),
           (255, 71),
           (259, 76),
           (262, 82),
           (263, 88),
           (262, 94),
           (260, 99),
           (257, 104),
           (254, 109),
           (251, 114),
           (247, 119),
           (243, 125),
           (240, 132),
           (237, 139),
           (234, 145),
           (230, 150),
           (224, 155),
           (215, 159),
           (210, 162),
           (202, 161),
           (199, 166),
           (198, 174),
           (199, 184),
           (198, 198),
           (194, 206),
           (192, 210),
           (200, 214),
           (204, 220),
           (205, 230),
           (201, 234),
           (193, 237),
           (193, 248),
           (0, 248)],
 'old': [(0, 0),
         (228, 0),
         (228, 36),
         (229, 39),
         (231, 42),
         (231, 46),
         (232, 49),
         (233, 53),
         (233, 59),
         (232, 63),
         (231, 65),
         (234, 66),
         (241, 68),
         (247, 72),
         (250, 77),
         (253, 81),
         (255, 86),
         (253, 91),
         (253, 96),
         (252, 101),
         (250, 104),
         (247, 108),
         (243, 112),
         (240, 118),
         (237, 125),
         (233, 133),
         (230, 140),
         (225, 149),
         (219, 157),
         (213, 164),
         (209, 169),
         (213, 175),
         (216, 182),
         (216, 189),
         (214, 195),
         (211, 200),
         (207, 202),
         (205, 205),
         (204, 211),
         (203, 216),
         (202, 226),
         (197, 228),
         (192, 230),
         (192, 241),
         (0, 241)]}
SCRATCHER_PROTECT = {'old': [[(218, 38),
          (223, 39),
          (227, 41),
          (229, 44),
          (229, 48),
          (231, 51),
          (231, 56),
          (230, 60),
          (229, 64),
          (226, 67),
          (219, 67),
          (218, 61),
          (216, 56),
          (216, 47)]]}
for _age, _box in SCRATCHER_CROPS.items():
    OBJECTS[_age][OBJECT_ACTIONS.index("scratcher_play")] = _box
HEAD_BOXES = {"cub":(50,103,168,202),"teen":(69,98,157,170),
              "adult":(70,100,160,176),"old":(51,102,167,186)}
# Lower paths follow the supplied chin, excluding the torso below the face.
# These coordinates are absolute source pixels for straightforward auditing.
HEAD_OUTLINES = {
    "teen": [(69,98),(157,98),(157,151),(152,155),(144,160),(135,162),
             (123,168),(113,169),(102,167),(90,163),(80,159),(69,152)],
    "adult": [(70,100),(160,100),(160,154),(153,158),(140,163),(131,168),
              (123,173),(114,175),(104,172),(94,166),(87,163),(77,159),(70,155)],
    "old": [(51,102),(167,102),(167,170),(158,174),(146,177),(133,178),
            (121,184),(110,185),(99,181),(92,178),(80,177),(70,175),(59,172),(51,166)],
}


CUTOUT_PROBES = [
    {'age': 'cub', 'action': 'jump', 'frame': 5, 'point': [75, 140], 'expect': 'transparent', 'description': 'Printed numeral removed without the touching paw contour', 'space': 'source_cell'},
    {'age': 'cub', 'action': 'jump', 'frame': 5, 'point': [86, 130], 'expect': 'opaque', 'description': 'Paw fur next to the removed printed numeral retained', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'walk_down', 'frame': 2, 'point': [57, 143], 'expect': 'transparent', 'description': 'Printed numeral removed without the touching paw contour', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'walk_down', 'frame': 2, 'point': [55, 131], 'expect': 'opaque', 'description': 'Paw fur next to the removed printed numeral retained', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'walk_down', 'frame': 5, 'point': [53, 143], 'expect': 'transparent', 'description': 'Printed numeral removed without the touching paw contour', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'walk_down', 'frame': 5, 'point': [65, 131], 'expect': 'opaque', 'description': 'Paw fur next to the removed printed numeral retained', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'walk_down', 'frame': 6, 'point': [56, 144], 'expect': 'transparent', 'description': 'Printed numeral removed without the touching paw contour', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'walk_down', 'frame': 6, 'point': [64, 130], 'expect': 'opaque', 'description': 'Paw fur next to the removed printed numeral retained', 'space': 'source_cell'},
    {'age': 'cub', 'action': 'scratcher_play', 'frame': 1, 'point': [300, 100], 'expect': 'transparent', 'description': 'Fixed scratcher pole excluded from character sprite', 'space': 'source_cell'},
    {'age': 'cub', 'action': 'scratcher_play', 'frame': 1, 'point': [235, 90], 'expect': 'opaque', 'description': 'Visible forepaw retained while fixed scratcher is excluded', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'scratcher_play', 'frame': 1, 'point': [300, 100], 'expect': 'transparent', 'description': 'Fixed scratcher pole excluded from character sprite', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'scratcher_play', 'frame': 1, 'point': [245, 90], 'expect': 'opaque', 'description': 'Visible forepaw retained while fixed scratcher is excluded', 'space': 'source_cell'},
    {'age': 'adult', 'action': 'scratcher_play', 'frame': 1, 'point': [300, 100], 'expect': 'transparent', 'description': 'Fixed scratcher pole excluded from character sprite', 'space': 'source_cell'},
    {'age': 'adult', 'action': 'scratcher_play', 'frame': 1, 'point': [248, 90], 'expect': 'opaque', 'description': 'Visible forepaw retained while fixed scratcher is excluded', 'space': 'source_cell'},
    {'age': 'old', 'action': 'scratcher_play', 'frame': 1, 'point': [300, 100], 'expect': 'transparent', 'description': 'Fixed scratcher pole excluded from character sprite', 'space': 'source_cell'},
    {'age': 'old', 'action': 'scratcher_play', 'frame': 1, 'point': [240, 90], 'expect': 'opaque', 'description': 'Visible forepaw retained while fixed scratcher is excluded', 'space': 'source_cell'},
    {'age': 'old', 'action': 'scratcher_play', 'frame': 1, 'point': [230, 55], 'expect': 'opaque', 'description': 'Open pale cheek contour protected against an alpha flood', 'space': 'source_cell'},
    {'age': 'old', 'action': 'scratcher_play', 'frame': 1, 'point': [221, 167], 'expect': 'transparent', 'description': 'Enclosed checkerboard behind the raised forepaw removed', 'space': 'source_cell'},
    {'age': 'old', 'action': 'groom_foam', 'frame': 1, 'point': [204, 65], 'expect': 'opaque', 'description': 'White right cheek preserved through its open source outline', 'space': 'source_cell'},
    {'age': 'old', 'action': 'idle_up', 'frame': 1, 'point': [151, 125], 'expect': 'opaque', 'description': 'White tail tip of the old tiger remains visible', 'space': 'source_cell'},
    {'age': 'adult', 'action': 'groom_foam', 'frame': 1, 'point': [117, 4], 'expect': 'opaque', 'description': 'Complete upper soap bubble retained above the original panel border', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'soap', 'frame': 1, 'point': [266, 166], 'expect': 'opaque', 'description': 'True white foam retained beneath the supplied soap pose', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'soap', 'frame': 1, 'point': [255, 149], 'expect': 'transparent', 'description': 'Printed blue checkerboard under the right hand removed', 'space': 'source_cell'},
    {'age': 'teen', 'action': 'soap', 'frame': 1, 'point': [266, 128], 'expect': 'transparent', 'description': 'Blue checkerboard between the right hand and bubbles removed', 'space': 'source_cell'},
    {'age': 'cub', 'action': 'groom_foam', 'frame': 1, 'point': [210, 120], 'expect': 'transparent', 'description': 'Blue background at the right cheek removed', 'space': 'source_cell'},
    {'age': 'adult', 'action': 'groom_foam', 'frame': 1, 'point': [225, 104], 'expect': 'transparent', 'description': 'Pale background between shoulder and bubbles removed', 'space': 'source_cell'},
]


def extract_main_pose(sheet, age, action, frame_index):
    return extract(sheet, MAIN[age][action][frame_index], action in ("sleep", "moods"),
                   MAIN_GROUND[age].get(action), LABEL_MASKS.get((age, action, frame_index)),
                   protected_polygons=FUR_PROTECT.get((age, action, frame_index), ()),
                   protect_closed_floor=True)


def object_ground(age, action):
    box = OBJECTS[age][OBJECT_ACTIONS.index(action)]
    height = box[3] - box[1]
    if action in TOY_ACTIONS:
        return round(height * .75)
    if action in ("bottle", "comb", "fetch_plush", "groom_foam", "soap", "towel"):
        return height - 40
    return None


def without_scratcher(im, age):
    if age not in SCRATCHER_OUTLINES:
        raise RuntimeError(f"Missing audited tiger scratcher outline: {age}")
    mask = Image.new("L", im.size)
    ImageDraw.Draw(mask).polygon(SCRATCHER_OUTLINES[age], fill=255)
    arr = np.array(im)
    arr[np.array(mask) == 0] = 0
    labels, pieces = components(arr[:, :, 3] > 20)
    if pieces:
        arr[labels != pieces[0][1]] = 0
    return Image.fromarray(arr)


def extract_object_pose(sheet, age, action):
    box = OBJECTS[age][OBJECT_ACTIONS.index(action)]
    if action == "scratcher_play":
        cropped = np.array(sheet.crop(box).convert("RGBA"))
        header = SCRATCHER_HEADERS[age]
        if header:
            rgb = cropped[:header,:,:3].astype("int16")
            green = (rgb[:,:,1] > rgb[:,:,0] + 4) & (rgb[:,:,1] > rgb[:,:,2] + 8)
            # Green belongs only to the printed header in this audited band.
            # Mark this background for removal; supplied ear RGB is untouched.
            cropped[:header][green] = 255
        local = Image.fromarray(cropped)
        value = extract(local, (0,0,local.width,local.height),
                        floor_band=local.height-45, protect_closed_floor=False,
                        hole_seeds=SCRATCHER_HOLES[age],
                        protected_polygons=SCRATCHER_PROTECT.get(age, ()))
        return without_scratcher(value, age)
    value = extract(sheet, box, action in ("groom_foam", "soap"),
                    object_ground(age, action), hole_seeds=TOY_HOLES.get((age, action), ()),
                    care_blue=CARE_BLUE.get((age, action)), care_foam=CARE_FOAM.get((age, action)),
                    protected_polygons=CARE_PROTECT.get((age, action), ()),
                    protect_closed_floor=False)
    return without_scratcher(value, age) if action == "scratcher_play" else value


def head_token(sheet, age):
    full_box = MAIN[age]["idle_down"][0]
    full = extract_main_pose(sheet, age, "idle_down", 0)
    box = HEAD_BOXES[age]
    head = full.crop((box[0]-full_box[0], box[1]-full_box[1],
                      box[2]-full_box[0], box[3]-full_box[1]))
    w, h = head.size
    mask = Image.new("L", head.size)
    outline = ([(x-box[0],y-box[1]) for x,y in HEAD_OUTLINES[age]]
               if age in HEAD_OUTLINES else [(0,0),(w,0),(w,h*.72),(w*.90,h*.84),
                 (w*.76,h*.94),(w*.53,h),(w*.32,h*.96),(w*.11,h*.83),(0,h*.72)])
    ImageDraw.Draw(mask).polygon(outline, fill=255)
    arr = np.array(head)
    arr[np.array(mask) == 0] = 0
    return normalize(Image.fromarray(arr),30000), box


def save(im, path, check=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    staging = ROOT / "build" / "tiger-assets-staging"
    staging.mkdir(parents=True, exist_ok=True)
    temporary = staging / (path.name + ".tmp")
    try:
        if path.suffix == ".webp":
            im.save(temporary,"WEBP",lossless=True,method=6,exact=True)
        else:
            im.save(temporary,"PNG",optimize=False)
        with Image.open(temporary) as encoded:
            encoded.load()
            if encoded.size != im.size or "A" not in encoded.getbands():
                raise RuntimeError(f"Invalid encoded resource: {path}")
            rebuilt_rgba = encoded.convert("RGBA").tobytes() if check else None
        if check:
            if not path.is_file():
                raise RuntimeError(f"Missing resource for source rebuild check: {path}")
            # Lossless codec versions may encode identical pixels differently.
            # Compare every RGBA byte, including RGB under alpha=0, without
            # tolerances; compressed bytes remain tracked by the manifest.
            with Image.open(path) as existing:
                identical = (existing.size == im.size
                             and existing.convert("RGBA").tobytes() == rebuilt_rgba)
            if not identical:
                raise RuntimeError(f"Resource differs from deterministic source rebuild: {path}")
        else:
            temporary.replace(path)
    finally:
        if temporary.exists():
            temporary.unlink()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--qa-dir",type=Path,help="Scratch previews for visual review")
    parser.add_argument("--check",action="store_true",
                        help="Rebuild and compare exact decoded RGBA without replacing runtime files")
    args = parser.parse_args()
    metadata = json.loads((SOURCE / "sources.json").read_text())
    manifest = {"version":"0.8.9","species":"tiger","frame_size":FRAME,
        "safe_margin":SAFE,"sources":metadata,"assets":{},"portraits":{},"cutout_probes":CUTOUT_PROBES,
        "notes":["Eight original user PNG files are preserved byte for byte.",
            "All printed checkerboards, labels and borders are excluded with audited source-space masks.",
            "LEFT idle is mirrored because every source labelled LEFT faces right.",
            "Run uses the six supplied walking poses at the existing engine run cadence.",
            "Rope and scratcher have one supplied interaction pose, animated by the existing engine.",
            "The fixed scratcher is removed from the character pose, because the scene owns this prop.",
            "Detached bubbles and mood symbols are retained and safely positioned after normalization.",
            "Every runtime frame is 256px with a 16px safe margin and common visual mass within its age.",
            "No drawing is shared across ages or species; original tiger sleeping poses each have one tail."]}
    for age in AGES:
        sheets={}
        for kind in ("main","objects"):
            source=SOURCE / metadata[age][kind]["file"]
            if hashlib.sha256(source.read_bytes()).hexdigest()!=metadata[age][kind]["sha256"]:
                raise RuntimeError(f"Modified original source: {source}")
            sheets[kind]=Image.open(source).convert("RGBA")
            metadata[age][kind]["path"]=str(source.relative_to(ROOT))
            metadata[age][kind]["dimensions"]=list(sheets[kind].size)
        frames={};records={}
        for action,boxes in MAIN[age].items():
            values=[extract_main_pose(sheets["main"],age,action,i) for i in range(len(boxes))]
            if action=="idle_left": values=[ImageOps.mirror(values[0])]
            frames[action]=values
            records[action]={"source":"main","source_cells":boxes,
                "derivation":"horizontal mirror of mislabeled source idle" if action=="idle_left" else "source poses"}
            if action in MAIN_GROUND[age]:
                records[action]["checker_shadow_cleanup_bands"]=[[0,MAIN_GROUND[age][action],b[2]-b[0],b[3]-b[1]] for b in boxes]
            label_masks={str(i+1):LABEL_MASKS[(age,action,i)] for i in range(len(boxes)) if (age,action,i) in LABEL_MASKS}
            if label_masks: records[action]["printed_label_masks"]=label_masks
            protected={str(i+1):FUR_PROTECT[(age,action,i)] for i in range(len(boxes)) if (age,action,i) in FUR_PROTECT}
            if protected: records[action]["protected_source_fur_polygons"]=protected
        for direction in ("down","left","right","up"):
            frames["run_"+direction]=frames["walk_"+direction]
            records["run_"+direction]={**records["walk_"+direction],"derivation":"supplied walk sequence played at run cadence"}
        for action,box in zip(OBJECT_ACTIONS,OBJECTS[age]):
            if action=="bowl" or (action=="bottle" and age!="cub"): continue
            frames[action]=[extract_object_pose(sheets["objects"],age,action)]
            records[action]={"source":"objects","source_cells":[box],
                "derivation":"fixed scratcher removed; one supplied tiger pose" if action=="scratcher_play" else "one supplied object interaction pose"}
            band=object_ground(age,action)
            if band is not None:
                records[action]["checker_shadow_cleanup_band"]=[0,band,box[2]-box[0],box[3]-box[1]]
            if (age,action) in TOY_HOLES:
                records[action]["audited_background_seeds"]=TOY_HOLES[(age,action)]
            if (age,action) in CARE_BLUE:
                records[action]["blue_checker_cleanup"]={"zone":CARE_BLUE[(age,action)][0],"protected_source_bubble_ellipses":CARE_BLUE[(age,action)][1],"protected_source_foam_polygons":CARE_PROTECT.get((age,action),[])}
            if action=="scratcher_play":
                records[action]["retained_source_silhouette"]=SCRATCHER_OUTLINES[age]
                records[action]["printed_header_cleanup_height"]=SCRATCHER_HEADERS[age]
                records[action]["protected_source_fur_polygons"]=SCRATCHER_PROTECT.get(age,[])
                records[action]["audited_background_seeds"]=SCRATCHER_HOLES[age]
                records[action]["checker_shadow_cleanup_band"]=[0,box[3]-box[1]-45,box[2]-box[0],box[3]-box[1]]
        feasible=[]
        for values in frames.values():
            for im in values:
                _,parts=components(np.array(im.getchannel("A"))>20)
                area,_,box=parts[0]
                max_dim=max(box[2]-box[0],box[3]-box[1])
                feasible.append(area*((FRAME-2*SAFE)/max_dim)**2)
        target_area=int(min(24500,min(feasible)*.98))
        manifest.setdefault("normalization",{})[age]={"largest_component_target_area":target_area,
            "baseline":FRAME-SAFE,"method":"same largest-component visual mass for every pose, safe complete silhouette"}
        for action in frames:
            frames[action]=[normalize(im,target_area) for im in frames[action]]
        dst=RUNTIME / f"res-tiger-{age}" / "drawable-nodpi"
        for action,values in frames.items():
            png=action.startswith("idle_") or action.startswith("fetch_") or action=="rope_play"
            name=f"tiger_{age}_{action}."+("png" if png else "webp")
            out=Image.new("RGBA",(FRAME*len(values),FRAME))
            for i,im in enumerate(values): out.alpha_composite(im,(FRAME*i,0))
            path=dst / name;save(out,path,check=args.check)
            manifest["assets"][name]={"age":age,"action":action,"path":str(path.relative_to(ROOT)),
                "frames":len(values),"width":out.width,"height":out.height,
                "sha256":hashlib.sha256(path.read_bytes()).hexdigest(),**records[action]}
        token,box=head_token(sheets["main"],age)
        path=dst / f"tiger_{age}_promenade_token.png";save(token,path,check=args.check)
        manifest["portraits"][path.name]={"age":age,"path":str(path.relative_to(ROOT)),
            "width":FRAME,"height":FRAME,"frames":1,"source":"main","source_cells":[box],
            "derivation":"head crop of same-age idle_down","retained_source_outline":HEAD_OUTLINES.get(age),
            "sha256":hashlib.sha256(path.read_bytes()).hexdigest()}
        if args.qa_dir:
            args.qa_dir.mkdir(parents=True,exist_ok=True)
            tiles=[]
            for action,values in frames.items():
                for i,im in enumerate(values):
                    tiles.append((action+(f" {i+1}" if len(values)>1 else ""),im))
            tiles.append(("promenade_token",token))
            cols=8;tw=160;th=184
            preview=Image.new("RGB",(cols*tw,math.ceil(len(tiles)/cols)*th),"#d6e3dc")
            draw=ImageDraw.Draw(preview)
            for i,(label,im) in enumerate(tiles):
                x=(i%cols)*tw;y=(i//cols)*th
                patch=im.resize((tw,tw),Image.Resampling.LANCZOS)
                preview.paste(patch,(x,y),patch)
                draw.text((x+4,y+tw+3),label,fill="#143627")
            preview.save(args.qa_dir / f"tiger-{age}-all.png")
        print(f"OK tiger {age}: {len(frames)} gameplay resources + portrait; visual area {target_area}; own sources only",flush=True)
    manifest_path=ROOT / "tiger-sprite-manifest.json"
    encoded_manifest=json.dumps(manifest,ensure_ascii=False,indent=2)+"\n"
    if args.check:
        if not manifest_path.is_file() or manifest_path.read_text()!=encoded_manifest:
            raise RuntimeError("Manifest differs from deterministic source rebuild")
    else:
        temporary=ROOT / "tiger-sprite-manifest.json.tmp"
        temporary.write_text(encoded_manifest)
        temporary.replace(manifest_path)
    print("OK tiger v0.8.9: 109 gameplay resources and 4 same-age portrait tokens")


if __name__=="__main__":
    main()
