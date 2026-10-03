#!/usr/bin/env python3
"""Validate complete wolf packs, transparent frame bounds and source provenance."""

from pathlib import Path
import hashlib
import json

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / "app/src/main"
AGES = ("cub", "teen", "adult", "old")
FRAME = 256
SAFE = 16
FRAMES = {
    **{f"idle_{direction}.png": 1 for direction in ("down", "left", "right", "up")},
    **{f"walk_{direction}.webp": 6 for direction in ("down", "left", "right", "up")},
    **{f"run_{direction}.webp": 6 for direction in ("down", "left", "right", "up")},
    "jump.webp": 5, "eat.webp": 3, "sleep.webp": 3, "moods.webp": 12,
    **{f"fetch_{toy}.png": 1 for toy in ("ball", "tennis", "yarn", "mouse", "plush")},
    "rope_play.png": 1,
    **{f"{care}.webp": 1 for care in ("groom_foam", "soap", "comb", "towel")},
    "scratcher_play.webp": 1,
}


def check(ok, message):
    if not ok:
        raise AssertionError(message)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def local_path(relative):
    path = (ROOT / relative).resolve()
    check(path.is_relative_to(ROOT), f"Manifest path leaves project: {relative}")
    check(path.is_file(), f"Missing file: {relative}")
    return path


def main():
    manifest = json.loads((ROOT / "wolf-sprite-manifest.json").read_text())
    check(manifest["species"] == "wolf", "Manifest species must be wolf")
    check(manifest["frame_size"] == FRAME, "Wolf and leopard frames must use the same 256px canvas")
    check(manifest["safe_margin"] == SAFE, "Expected 16px safety margin")
    check(set(manifest["sources"]) == set(AGES), "Each age needs its own source pair")

    source_sizes = {}
    sources_seen = set()
    for age in AGES:
        check(set(manifest["sources"][age]) == {"main", "objects"}, f"{age}: exactly two original sheets")
        source_sizes[age] = {}
        for kind, record in manifest["sources"][age].items():
            source = local_path(record["path"])
            check(source.name == f"wolf_{age}_{kind}_source.png", f"{age}/{kind}: source age must match")
            actual_hash = digest(source)
            check(actual_hash == record["sha256"], f"{source.name}: original upload has changed")
            check(actual_hash not in sources_seen, f"{source.name}: source reused for another age or sheet")
            sources_seen.add(actual_hash)
            with Image.open(source) as image:
                check(list(image.size) == record["dimensions"], f"{source.name}: source dimensions changed")
                source_sizes[age][kind] = image.size

    assets = manifest["assets"]
    portraits = manifest["portraits"]
    check(len(assets) == 109, "Expected 109 gameplay wolf resources")
    check(len(portraits) == 4, "Each wolf age needs its own promenade portrait")
    all_records = {**assets, **portraits}
    check(len(all_records) == 113, "Gameplay and portrait records must be disjoint")
    expected_names = set()
    expected_paths = set()
    hashes = {}
    leopard_hashes = {
        digest(path)
        for age in AGES
        for path in (RUNTIME / f"res-{age}/drawable-nodpi").glob("leopard_*")
        if path.is_file()
    }
    total_frames = 0

    for age in AGES:
        frame_counts = dict(FRAMES)
        if age == "cub":
            frame_counts["bottle.webp"] = 1
        frame_counts["promenade_token.png"] = 1
        folder = RUNTIME / f"res-wolf-{age}/drawable-nodpi"
        expected = {f"wolf_{age}_{name}": frames for name, frames in frame_counts.items()}
        actual = {path.name for path in folder.glob("wolf_*") if path.is_file()}
        check(actual == set(expected),
              f"{age}: missing={sorted(set(expected)-actual)}, unexpected={sorted(actual-set(expected))}")
        age_frames = 0
        for name, frames in expected.items():
            expected_names.add(name)
            record = all_records[name]
            path = local_path(record["path"])
            expected_paths.add(path)
            check(path == (folder / name).resolve(), f"{name}: resource must stay in its own age folder")
            check(record["age"] == age, f"{name}: manifest age mismatch")
            check(record["frames"] == frames, f"{name}: supplied animation frame count changed")
            check((record["width"], record["height"]) == (FRAME * frames, FRAME),
                  f"{name}: declared dimensions disagree with the animation contract")
            check(record["source"] in {"main", "objects"}, f"{name}: missing source provenance")
            cells = record["source_cells"]
            check(len(cells) == frames, f"{name}: each frame needs its source crop")
            source_width, source_height = source_sizes[age][record["source"]]
            for cell in cells:
                check(len(cell) == 4 and 0 <= cell[0] < cell[2] <= source_width
                      and 0 <= cell[1] < cell[3] <= source_height,
                      f"{name}: crop outside its original age sheet: {cell}")
            check(bool(record["derivation"]), f"{name}: extraction method must be recorded")
            actual_hash = digest(path)
            check(actual_hash == record["sha256"], f"{name}: generated asset differs from audited manifest")
            check(actual_hash not in leopard_hashes, f"{name}: a leopard drawing was substituted for a wolf")
            if actual_hash in hashes:
                check(hashes[actual_hash][0] == age,
                      f"{name}: identical sprite reused from {hashes[actual_hash]}")
            hashes[actual_hash] = (age, name)
            with Image.open(path) as strip:
                strip.load()
                check(strip.size == (FRAME * frames, FRAME), f"{name}: actual dimensions {strip.size}")
                check("A" in strip.getbands(), f"{name}: opaque sheet was not converted to transparency")
                for index in range(frames):
                    alpha = strip.crop((index * FRAME, 0, (index + 1) * FRAME, FRAME)).getchannel("A")
                    bounds = alpha.getbbox()
                    check(bounds is not None, f"{name} frame {index + 1}: empty sprite")
                    check(alpha.getextrema() == (0, 255), f"{name} frame {index + 1}: transparent background/opaque character missing")
                    check(bounds[0] >= SAFE and bounds[1] >= SAFE
                          and bounds[2] <= FRAME-SAFE and bounds[3] <= FRAME-SAFE,
                          f"{name} frame {index + 1}: clipping risk {bounds}")
            age_frames += frames
        total_frames += age_frames
        print(f"OK wolf {age}: {len(expected)-1} gameplay assets + portrait, {age_frames} frames, 256px canvas and 16px margins")

    check(set(all_records) == expected_names, "Manifest contains an absent or unregistered wolf resource")
    actual_paths = {path.resolve() for path in RUNTIME.glob("res*/drawable*/wolf_*") if path.is_file()}
    check(actual_paths == expected_paths,
          "Wolf resources exist outside the four age packs: " + str(sorted(str(p) for p in actual_paths-expected_paths)))
    print(f"Wolf sprite validation: PASS — 8 original sheets, 113 outputs, {total_frames} nonempty frames, no age/species mixing")


if __name__ == "__main__":
    main()
