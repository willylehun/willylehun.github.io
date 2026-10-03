"""Reconstruct verified sprite geometry before reviewed background removal.

The patch contains only the original RGBA pixels that were erased. Rebuilding
the source from the final image keeps size checks independent of the changed
connectivity of its foreground, without changing the runtime image.
"""

from __future__ import annotations

import base64
import binascii
import hashlib
import re
import zlib

import numpy as np
from PIL import Image


FRAME = 256
RAW_BYTES = FRAME * FRAME * 4


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def verify_digest(data, expected, label):
    check(isinstance(expected, str) and re.fullmatch(r"[0-9a-f]{64}", expected),
          f"{label}: invalid SHA-256 digest")
    check(hashlib.sha256(data).hexdigest() == expected,
          f"{label}: RGBA pixels differ from the audited image")


def decode_removed_rgba(frame_record: dict, label: str = "cutout frame") -> np.ndarray:
    """Return a read-only uint8 (256, 256, 4) array of erased source pixels."""
    check(isinstance(frame_record, dict), f"{label}: frame audit must be an object")
    encoded = frame_record.get("removed_rgba_zlib_base64")
    check(isinstance(encoded, str) and 0 < len(encoded) <= 4 * ((RAW_BYTES + 1026) // 3),
          f"{label}: missing or oversized RGBA removal patch")
    try:
        compressed = base64.b64decode(encoded, validate=True)
        decoder = zlib.decompressobj()
        raw = decoder.decompress(compressed, RAW_BYTES + 1)
    except (binascii.Error, ValueError, zlib.error) as error:
        raise AssertionError(f"{label}: invalid compressed RGBA removal patch") from error
    check(len(raw) == RAW_BYTES and decoder.eof
          and not decoder.unconsumed_tail and not decoder.unused_data,
          f"{label}: patch must contain exactly one 256px RGBA frame")
    removed = np.frombuffer(raw, dtype=np.uint8).reshape(FRAME, FRAME, 4)
    check(np.all(removed[removed[:, :, 3] == 0] == 0),
          f"{label}: pixels outside the removal mask must have zero RGBA")
    return removed


def reconstruct_cutout_source(record: dict, image: Image.Image) -> Image.Image:
    """Verify the final strip and return its exact RGBA source before cutout.

    Raises AssertionError for stale final pixels, invalid patches, nonzero
    output in an erased region, or any mismatch with the audited source hashes.
    The caller's image and all runtime files are left untouched.
    """
    check(isinstance(record, dict), "Cutout resource audit must be an object")
    label = str(record.get("path", "cutout sprite"))
    after = image.convert("RGBA")
    check(list(after.size) == record.get("size") and after.height == FRAME
          and after.width > 0 and after.width % FRAME == 0,
          f"{label}: final and audited frame dimensions disagree")
    verify_digest(after.tobytes(), record.get("rgba_sha256"), label + " final")
    details = record.get("frames")
    check(isinstance(details, list) and details, f"{label}: missing frame audits")
    before = after.copy()
    visited = set()
    for frame_record in details:
        check(isinstance(frame_record, dict), f"{label}: frame audit must be an object")
        number = frame_record.get("frame")
        check(type(number) is int and 1 <= number <= after.width // FRAME
              and number not in visited,
              f"{label}: duplicate or invalid frame number {number}")
        visited.add(number)
        frame_label = f"{label} #{number}"
        removed = decode_removed_rgba(frame_record, frame_label)
        mask = removed[:, :, 3] > 0
        removed_count = frame_record.get("removed_pixels")
        check(type(removed_count) is int and removed_count == int(mask.sum()),
              f"{frame_label}: recorded removal count differs from the RGBA patch")
        left = (number - 1) * FRAME
        bounds = (left, 0, left + FRAME, FRAME)
        original_frame = np.array(after.crop(bounds))
        check(np.all(original_frame[mask] == 0),
              f"{frame_label}: an audited removal is not fully transparent in the final image")
        original_frame[mask] = removed[mask]
        reconstructed = Image.fromarray(original_frame)
        verify_digest(reconstructed.tobytes(), frame_record.get("before_frame_rgba_sha256"),
                      frame_label + " before cutout")
        before.paste(reconstructed, (left, 0))
    # This also checks every frame absent from the cutout annotations: none of
    # their pixels may have changed when the other frames were repaired.
    verify_digest(before.tobytes(), record.get("before_rgba_sha256"), label + " before cutout")
    return before
