#!/usr/bin/env python3
"""Focused regression checks for normal and read-only tiger resource saving."""
from io import BytesIO
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from PIL import Image

import prepare_v089_tiger_assets as importer


class TigerAssetSaveTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="tiger-save-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.root_patch = patch.object(importer, "ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        self.source = Image.new("RGBA", (96, 80))
        self.source.putdata([
            ((x * 17 + y * 31) % 256, (x * 11 + y * 7) % 256,
             (x * 3 + y * 19) % 256,
             0 if (x + y) % 5 == 0 else 127 if (x + y) % 3 == 0 else 255)
            for y in range(80) for x in range(96)
        ])

    def encode(self, im, suffix, alternate=False):
        buffer = BytesIO()
        if suffix == ".webp":
            im.save(buffer, "WEBP", lossless=True, exact=True,
                    method=0 if alternate else 6)
        else:
            im.save(buffer, "PNG", optimize=False,
                    compress_level=0 if alternate else 6)
        return buffer.getvalue()

    def assert_pixels(self, path, expected):
        with Image.open(path) as actual:
            self.assertEqual(actual.size, expected.size)
            self.assertEqual(actual.convert("RGBA").tobytes(), expected.tobytes())

    def test_normal_save_creates_missing_file_and_replaces_existing_file(self):
        for suffix in (".png", ".webp"):
            with self.subTest(suffix=suffix):
                path = self.root / ("normal" + suffix)
                importer.save(self.source, path)
                self.assert_pixels(path, self.source)
                changed = self.source.copy()
                changed.putpixel((1, 1), (12, 34, 56, 78))
                importer.save(changed, path)
                self.assert_pixels(path, changed)

    def test_check_rejects_missing_file_without_creating_it(self):
        for suffix in (".png", ".webp"):
            with self.subTest(suffix=suffix):
                path = self.root / ("missing" + suffix)
                with self.assertRaisesRegex(RuntimeError, "Missing resource"):
                    importer.save(self.source, path, check=True)
                self.assertFalse(path.exists())

    def test_check_accepts_different_lossless_encoding_without_replacing_it(self):
        for suffix in (".png", ".webp"):
            with self.subTest(suffix=suffix):
                normal = self.encode(self.source, suffix)
                alternate = self.encode(self.source, suffix, alternate=True)
                self.assertNotEqual(normal, alternate)
                path = self.root / ("alternate" + suffix)
                path.write_bytes(alternate)
                self.assert_pixels(path, self.source)
                before_mtime = path.stat().st_mtime_ns
                importer.save(self.source, path, check=True)
                self.assertEqual(path.read_bytes(), alternate)
                self.assertEqual(path.stat().st_mtime_ns, before_mtime)

    def test_check_rejects_every_changed_rgba_channel(self):
        for suffix in (".png", ".webp"):
            for channel in range(4):
                with self.subTest(suffix=suffix, channel=channel):
                    changed = self.source.copy()
                    pixel = list(changed.getpixel((1, 1)))
                    pixel[channel] = (pixel[channel] + 1) % 256
                    changed.putpixel((1, 1), tuple(pixel))
                    path = self.root / ("channel" + str(channel) + suffix)
                    encoded = self.encode(changed, suffix)
                    path.write_bytes(encoded)
                    with self.assertRaisesRegex(RuntimeError, "Resource differs"):
                        importer.save(self.source, path, check=True)
                    self.assertEqual(path.read_bytes(), encoded)

    def test_check_rejects_changed_rgb_even_when_alpha_is_zero(self):
        for suffix in (".png", ".webp"):
            with self.subTest(suffix=suffix):
                changed = self.source.copy()
                self.assertEqual(changed.getpixel((0, 0))[3], 0)
                changed.putpixel((0, 0), (1, 2, 3, 0))
                path = self.root / ("hidden-rgb" + suffix)
                path.write_bytes(self.encode(changed, suffix))
                with self.assertRaisesRegex(RuntimeError, "Resource differs"):
                    importer.save(self.source, path, check=True)

    def test_check_rejects_changed_dimensions(self):
        for suffix in (".png", ".webp"):
            with self.subTest(suffix=suffix):
                path = self.root / ("size" + suffix)
                path.write_bytes(self.encode(self.source.crop((0, 0, 95, 80)), suffix))
                with self.assertRaisesRegex(RuntimeError, "Resource differs"):
                    importer.save(self.source, path, check=True)


if __name__ == "__main__":
    unittest.main(verbosity=2)
