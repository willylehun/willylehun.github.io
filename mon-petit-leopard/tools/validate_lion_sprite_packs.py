#!/usr/bin/env python3
"""Validate the lion's eight sex/age packs against 16 immutable user sheets.

The frame contract is shared with the existing wolf/tiger packs. Original upload
hashes are pinned here independently of generator metadata. Decoded RGBA checks
ignore RGB hidden behind alpha zero so lossless WebP encoder versions cannot
masquerade as artwork changes, while wrong species, sex or age remain forbidden.
"""

from pathlib import Path
import json

from PIL import Image

from validate_wolf_sprite_packs import AGES, FRAME, FRAMES, SAFE, check, digest
from validate_tiger_sprite_packs import pixel_digest


ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / "app/src/main"
SOURCE = ROOT / "source-assets/lion-v0810"
SEXES = ("male", "female")
ORIGINAL_HASHES = {
    ("male", "cub", "main"): "b3d3951a88f7962e02561d979af27a4db6c982b99691f656401022d373a75cc4",
    ("male", "cub", "objects"): "f837213c7b1eb9b6c33ae1dda14f30dad9b08ed47988b8d94f04fe51fed9ddd3",
    ("male", "teen", "main"): "d4ff1b77f29c8a22eb6e92970b9369279c336bc15588167cef2a8bd1fea43721",
    ("male", "teen", "objects"): "98408cf6234f9efaba82dde9c102a0d4f884569107ce68726181c5d835028593",
    ("male", "adult", "main"): "1b750152e4462e607089b0bf05198ddad634210f70b2b428afbe8e3cc858b8c0",
    ("male", "adult", "objects"): "1517e31ba1a1b0310269a27a81104837b67478eec58ade8898c2e5d98b79e27c",
    ("male", "old", "main"): "0fb34f80973f52f83f78dfe43c19c59abda1ecb760e309639b301d6f760a03a8",
    ("male", "old", "objects"): "9e43c1a6e80b29c340377ab34680dc9fce1a15dbb3c18c81b48479d8de90fbf0",
    ("female", "cub", "main"): "f09903e798ff90c6ccb3bf07971b296808a5501c79d2a2845f672a5dcc233a3d",
    ("female", "cub", "objects"): "be4832339317bc4489bb2137b5678fd9c2778d2aba4c7a4102fca054dd084a6a",
    ("female", "teen", "main"): "345b57f54c514fe456eaaaf69664469b75e258ada150edbe173027a21213a753",
    ("female", "teen", "objects"): "d155e49ec1d1c3802f2d3af1c3aab2309d6709a8d8d1c3858fec86ed904b27f5",
    ("female", "adult", "main"): "77e9e294e6a7d955bcad3b21e917ad3a46f6688b09173084cad214b6bb3a38ff",
    ("female", "adult", "objects"): "0ed510e7b83c1b1c67cb87369a794027442dca5709dbab5bb169a067eca92a16",
    ("female", "old", "main"): "492d2c56866a33a8d195d446301051a1005ce24bfd0aeba7d5da35056053bfd9",
    ("female", "old", "objects"): "97451cf2cab44d7e8ce8dcef7adf0b9f2492185bda965f5075b6c43707282db6",
}


def local_path(relative):
    path = (ROOT / relative).resolve()
    check(path.is_relative_to(ROOT), f"Manifest path leaves project: {relative}")
    check(path.is_file(), f"Missing file: {relative}")
    return path


def validate_cutout_probes(manifest):
    """Check inspected background/fur/object points in original crop coordinates."""
    from prepare_v0810_lion_assets import extract_main_pose, extract_object_pose

    probes = manifest.get("cutout_probes", [])
    check(probes, "Lion cutouts need audited background and preserved-character probes")
    assets = {(record["sex"], record["age"], record["action"]): record
              for record in manifest["assets"].values()}
    sheets, cutouts, coverage = {}, {}, set()
    care_coverage, scratcher_coverage = set(), set()
    for index, probe in enumerate(probes, 1):
        label = f"Lion cutout probe {index}"
        sex, age, action, frame_index = probe["sex"], probe["age"], probe["action"], probe["frame"]
        point, expected = probe["point"], probe["expect"]
        check(probe.get("space") == "source_cell", f"{label}: unknown coordinate space")
        check(bool(probe.get("description")), f"{label}: inspection rationale is missing")
        check((sex, age, action) in assets, f"{label}: action does not exist for this sex/age")
        record = assets[sex, age, action]
        check(isinstance(frame_index, int) and 1 <= frame_index <= record["frames"],
              f"{label}: frame lies outside its animation")
        check(expected in {"transparent", "opaque"}, f"{label}: unexpected alpha criterion")
        source_kind = record["source"]
        sheet_key = sex, age, source_kind
        if sheet_key not in sheets:
            with Image.open(local_path(manifest["sources"][sex][age][source_kind]["path"])) as image:
                sheets[sheet_key] = image.convert("RGBA")
        key = sex, age, action, frame_index
        if key not in cutouts:
            if source_kind == "main":
                source_action = action.replace("run_", "walk_", 1) if action.startswith("run_") else action
                cutouts[key] = extract_main_pose(sheets[sheet_key], sex, age, source_action, frame_index-1)
            else:
                check(frame_index == 1, f"{label}: object poses have one source frame")
                cutouts[key] = extract_object_pose(sheets[sheet_key], sex, age, action)
        cutout = cutouts[key]
        check(len(point) == 2 and all(isinstance(value, int) for value in point)
              and 0 <= point[0] < cutout.width and 0 <= point[1] < cutout.height,
              f"{label}: point lies outside its original crop")
        alpha = cutout.getchannel("A").getpixel(tuple(point))
        if expected == "transparent":
            check(alpha == 0, f"{label} {key}: background remains at {point}, alpha={alpha}")
        else:
            minimum = probe.get("min_alpha", 255)
            check(isinstance(minimum, int) and 128 <= minimum <= 255,
                  f"{label}: opaque probes must preserve visible character/object pixels")
            check(alpha >= minimum, f"{label} {key}: protected pixels lost at {point}, alpha={alpha}")
        coverage.add((sex, age, expected))
        if action in {"groom_foam", "soap", "comb", "towel"}:
            care_coverage.add(sex)
        if action == "scratcher_play":
            scratcher_coverage.add(sex)
    check(coverage == {(sex, age, expected) for sex in SEXES for age in AGES
                       for expected in ("transparent", "opaque")},
          "Each lion sex/age needs background-removal and character-preservation probes")
    check(care_coverage == scratcher_coverage == set(SEXES),
          "Both sexes need inspected care cutouts and separation from the fixed scratcher")
    print(f"Lion cutout probes: PASS — {len(probes)} source-space background/fur/object checks across eight sex/age packs")


def validate_sources(manifest):
    check(set(manifest["sources"]) == set(SEXES), "Lion sources must keep male and female separate")
    source_index = json.loads((SOURCE / "sources.json").read_text())
    check(set(source_index) == set(SEXES), "The original upload index must cover both sexes")
    source_sizes, source_paths, source_hashes = {}, set(), set()
    for sex in SEXES:
        check(set(manifest["sources"][sex]) == set(AGES), f"{sex}: four explicit age source pairs are required")
        check(set(source_index[sex]) == set(AGES), f"{sex}: source index must cover all ages")
        for age in AGES:
            records = manifest["sources"][sex][age]
            check(set(records) == {"main", "objects"}, f"{sex}/{age}: exactly two original source sheets")
            check(set(source_index[sex][age]) == {"main", "objects"}, f"{sex}/{age}: exactly two upload records")
            for kind, record in records.items():
                path = local_path(record["path"])
                name = f"lion_{sex}_{age}_{kind}_source.png"
                check(path == (SOURCE / name).resolve(), f"{sex}/{age}/{kind}: source belongs to another pack")
                check(record["file"] == name, f"{sex}/{age}/{kind}: original file identity differs")
                for field in ("file", "original_filename", "sha256"):
                    check(record[field] == source_index[sex][age][kind][field],
                          f"{name}: manifest disagrees with original upload index ({field})")
                original_hash = digest(path)
                check(original_hash == record["sha256"] == ORIGINAL_HASHES[sex, age, kind],
                      f"{name}: original bytes changed or belong to another sex/age")
                check(path not in source_paths and original_hash not in source_hashes,
                      f"{name}: source reused by another sex/age/sheet")
                source_paths.add(path)
                source_hashes.add(original_hash)
                with Image.open(path) as image:
                    check(list(image.size) == record["dimensions"], f"{name}: source dimensions changed")
                    source_sizes[sex, age, kind] = image.size
    check(len(source_paths) == 16, "Exactly sixteen distinct original lion sheets are required")
    return source_sizes


def other_animal_pixels():
    whole, frames = set(), set()
    for species in ("leopard", "wolf", "tiger"):
        for path in RUNTIME.glob(f"res*/drawable*/{species}_*"):
            if not path.is_file():
                continue
            with Image.open(path) as image:
                whole.add(pixel_digest(image))
                if image.height == FRAME and image.width % FRAME == 0:
                    for start in range(0, image.width, FRAME):
                        frames.add(pixel_digest(image.crop((start, 0, start + FRAME, FRAME))))
    return whole, frames


def main():
    manifest = json.loads((ROOT / "lion-sprite-manifest.json").read_text())
    check(manifest["species"] == "lion", "Male lion and lioness must use the same lion species")
    check(manifest["frame_size"] == FRAME, "All animals must use the same 256px canvas")
    check(manifest["safe_margin"] == SAFE, "Lion sprites need the shared 16px safety margin")
    source_sizes = validate_sources(manifest)
    check(set(manifest["normalization"]) == {f"{sex}_{age}" for sex in SEXES for age in AGES},
          "Visual normalization must be explicit for each sex/age pack")
    for key, settings in manifest["normalization"].items():
        check(settings["baseline"] == FRAME-SAFE, f"{key}: wrong shared foot baseline")
        check(0 < settings["largest_component_target_area"] <= (FRAME-2*SAFE)**2,
              f"{key}: invalid target character area")

    assets, portraits = manifest["assets"], manifest["portraits"]
    check(len(assets) == 218, "Expected 218 lion gameplay resources, 109 per sex")
    check(len(portraits) == 8, "Every lion sex/age needs its own promenade portrait")
    check(not set(assets) & set(portraits), "Gameplay resources and portraits must be disjoint")
    records = {**assets, **portraits}
    other_pixels, other_frame_pixels = other_animal_pixels()
    expected_paths, expected_assets, expected_portraits = set(), set(), set()
    pack_pixel_owners, frame_pixel_owners = {}, {}
    total_frames = 0
    for sex in SEXES:
        for age in AGES:
            pack = sex, age
            prefix = f"lion_{sex}_{age}_"
            frame_counts = dict(FRAMES)
            if age == "cub":
                frame_counts["bottle.webp"] = 1
            frame_counts["promenade_token.png"] = 1
            folder = RUNTIME / f"res-lion-{sex}-{age}/drawable-nodpi"
            expected = {prefix + name: count for name, count in frame_counts.items()}
            actual = {path.name for path in folder.iterdir() if path.is_file()}
            check(actual == set(expected),
                  f"{sex}/{age}: missing={sorted(set(expected)-actual)}, unexpected={sorted(actual-set(expected))}")
            for name, count in expected.items():
                is_portrait = name.endswith("_promenade_token.png")
                (expected_portraits if is_portrait else expected_assets).add(name)
                record = records[name]
                path = local_path(record["path"])
                expected_paths.add(path)
                check(path == (folder / name).resolve(), f"{name}: resource leaves its own sex/age folder")
                check((record["sex"], record["age"]) == pack, f"{name}: manifest sex/age mismatch")
                action = name[len(prefix):].rsplit(".", 1)[0]
                if not is_portrait:
                    check(record["action"] == action, f"{name}: manifest action mismatch")
                check(record["frames"] == count, f"{name}: supplied frame count changed")
                check((record["width"], record["height"]) == (FRAME*count, FRAME),
                      f"{name}: declared dimensions disagree with runtime animation contract")
                source_kind = record["source"]
                is_main = is_portrait or action.startswith(("idle_", "walk_", "run_")) or action in {"jump", "eat", "sleep", "moods"}
                check(source_kind == ("main" if is_main else "objects"),
                      f"{name}: action uses the wrong kind of original sheet")
                cells = record["source_cells"]
                check(len(cells) == count, f"{name}: every frame needs its original crop")
                width, height = source_sizes[sex, age, source_kind]
                for cell in cells:
                    check(len(cell) == 4 and all(isinstance(value, int) for value in cell)
                          and 0 <= cell[0] < cell[2] <= width and 0 <= cell[1] < cell[3] <= height,
                          f"{name}: crop lies outside its original source: {cell}")
                check(bool(record["derivation"]), f"{name}: extraction method must be recorded")
                # This hash describes the current encoded artifact, not a hash pinned
                # across different Pillow versions. Pixel identity is checked below.
                check(digest(path) == record["sha256"], f"{name}: output differs from its current manifest")
                with Image.open(path) as image:
                    check(image.size == (FRAME*count, FRAME), f"{name}: actual dimensions {image.size}")
                    check("A" in image.getbands(), f"{name}: opaque sheet has no transparency")
                    decoded = pixel_digest(image)
                    check(decoded == record["rgba_sha256"], f"{name}: decoded artwork differs from audited RGBA")
                    check(decoded not in other_pixels, f"{name}: another animal's artwork was substituted")
                    if decoded in pack_pixel_owners:
                        check(pack_pixel_owners[decoded][0] == pack,
                              f"{name}: artwork reused from another sex/age: {pack_pixel_owners[decoded]}")
                    pack_pixel_owners[decoded] = pack, name
                    for index in range(count):
                        frame = image.crop((index*FRAME, 0, (index+1)*FRAME, FRAME))
                        frame_hash = pixel_digest(frame)
                        check(frame_hash not in other_frame_pixels,
                              f"{name} frame {index+1}: another animal's frame was substituted")
                        if frame_hash in frame_pixel_owners:
                            check(frame_pixel_owners[frame_hash][0] == pack,
                                  f"{name} frame {index+1}: frame reused from another sex/age: {frame_pixel_owners[frame_hash]}")
                        frame_pixel_owners[frame_hash] = pack, name, index+1
                        alpha = frame.getchannel("A")
                        bounds = alpha.getbbox()
                        check(bounds is not None, f"{name} frame {index+1}: empty sprite")
                        check(alpha.getextrema() == (0, 255),
                              f"{name} frame {index+1}: transparent background or opaque character missing")
                        check(bounds[0] >= SAFE and bounds[1] >= SAFE
                              and bounds[2] <= FRAME-SAFE and bounds[3] <= FRAME-SAFE,
                              f"{name} frame {index+1}: clipping risk at {bounds}")
                total_frames += count
            for direction in ("down", "left", "right", "up"):
                walk = assets[prefix + f"walk_{direction}.webp"]
                run = assets[prefix + f"run_{direction}.webp"]
                check(run["source_cells"] == walk["source_cells"] and run["source"] == walk["source"],
                      f"{sex}/{age}/{direction}: running must use the six same-pack walking poses")
                check(run["rgba_sha256"] == walk["rgba_sha256"],
                      f"{sex}/{age}/{direction}: run and walk drawings differ")
            for action in ("idle", "walk", "run"):
                extension = "png" if action == "idle" else "webp"
                for first, second in (("left", "right"), ("down", "up")):
                    check(assets[prefix + f"{action}_{first}.{extension}"]["rgba_sha256"] !=
                          assets[prefix + f"{action}_{second}.{extension}"]["rgba_sha256"],
                          f"{sex}/{age}: {action} {first}/{second} incorrectly share the same drawing")
            check("mirror" in assets[prefix + "idle_left.png"]["derivation"],
                  f"{sex}/{age}: left idle must record correction of its right-facing source")
            print(f"OK lion {sex} {age}: {len(expected)-1} gameplay assets + portrait, own sources, 256px canvas/16px margins")

    check(set(assets) == expected_assets, "Manifest contains absent or unregistered gameplay resources")
    check(set(portraits) == expected_portraits, "Every promenade portrait must stay separate from gameplay")
    actual_paths = {path.resolve() for path in RUNTIME.glob("res*/drawable*/lion_*") if path.is_file()}
    check(actual_paths == expected_paths,
          "Lion resources exist outside their eight sex/age packs: " + str(sorted(map(str, actual_paths-expected_paths))))
    check(total_frames == 698, f"Expected 698 lion frames including eight head tokens, got {total_frames}")
    validate_cutout_probes(manifest)
    print("Lion sprite validation: PASS — 16 original sheets, 226 outputs, 698 transparent frames, no species/sex/age substitution")


if __name__ == "__main__":
    main()
