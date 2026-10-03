#!/usr/bin/env python3
"""Audit the fox/bear packs against their sixteen original uploaded sheets.

The upload hashes are pinned independently of the importers. Each output frame
must belong to its exact animal/age, retain transparent safety margins and use
the common animation contract. Source-space alpha probes protect inspected fur,
care objects and background holes; importer --check verifies the full rebuild.
"""

import importlib
import json
from pathlib import Path

from PIL import Image

from validate_tiger_sprite_packs import pixel_digest
from validate_wolf_sprite_packs import AGES, FRAME, FRAMES, SAFE, check, digest


ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / "app/src/main"
SPECIES = ("fox", "bear")
ORIGINAL_HASHES = {
    ("fox", "cub", "main"): "e4aaa4f9f7eef01630c0df5134c8e8669ed20b92a42f9f604cf81a9b633f4514",
    ("fox", "cub", "objects"): "0917dafb93becb6e361788113aa8711c03b03c78e3fe2ee2dab3ffa57bc74ff9",
    ("fox", "teen", "main"): "d24d70427f63ec5901c0e939cd462d3fa6f0e188090aad178b43b7959ea62c5c",
    ("fox", "teen", "objects"): "09e59471419633df282c9d5b85940014df81256fdaac1b9140eb19d56f3d43c1",
    ("fox", "adult", "main"): "2b9c242680bb7b1081755b00a076a4d7248c774078f823ebfc4549e5213db90c",
    ("fox", "adult", "objects"): "56715952a00ec716f9d70010a1691e4051044850896d64863f69644b1f9c881f",
    ("fox", "old", "main"): "5254150bf5364fba59869e917bf73657e6c24404f53fd5cac89a45a54d292a2b",
    ("fox", "old", "objects"): "63e1352fb5ea620b56a78caebc8380fbe6f5c602452009d4b5511e85c2bf15b4",
    ("bear", "cub", "main"): "268c4f2ce7f2b14ba0eeeafd0ca169e3041b636040b40b18a7c866b4466b83f1",
    ("bear", "cub", "objects"): "b99141fb7a6f54a5e2ba62ca53d0fd4af0de856af15a7a21640fe9fd8485f60e",
    ("bear", "teen", "main"): "2f5bf0ee15c5c89864f0b090cb18b39b485db21713787d245cc446fbeb439ba0",
    ("bear", "teen", "objects"): "709d2c0521da0b33b3361b4bb579b6cab49858e2332d54311333d6f3f6869e04",
    ("bear", "adult", "main"): "511329c3ca51fdff8edf08fc5c44233de83e1751dc9d1913b2c350fe2914f5f8",
    ("bear", "adult", "objects"): "83485b73bc6f6d862e65344c130077e74769e5fd0cbce6e99068bb63c01c7cbd",
    ("bear", "old", "main"): "5b50cc8931e75d50e9a056c3355a2aa3d2392f0c37cfa94239eb8f42acbb4343",
    ("bear", "old", "objects"): "545c6fe1ff09e505a6355d578a7efec8a546b8fb2e3f403780edcb4e70ef1aa3",
}


def local_path(relative):
    path = (ROOT / relative).resolve()
    check(path.is_relative_to(ROOT), f"Manifest path leaves project: {relative}")
    check(path.is_file(), f"Missing file: {relative}")
    return path


def validate_sources(species, manifest, source_paths, source_hashes):
    source_root = ROOT / f"source-assets/{species}-v0811"
    source_index = json.loads((source_root / "sources.json").read_text())
    check(set(manifest["sources"]) == set(AGES), f"{species}: four age-specific source pairs required")
    check(set(source_index) == set(AGES), f"{species}: original upload index must cover all ages")
    sizes = {}
    for age in AGES:
        records = manifest["sources"][age]
        check(set(records) == {"main", "objects"}, f"{species}/{age}: exactly two original sheets required")
        check(set(source_index[age]) == {"main", "objects"}, f"{species}/{age}: upload index has wrong sheets")
        for kind, record in records.items():
            name = f"{species}_{age}_{kind}_source.png"
            path = local_path(record["path"])
            check(path == (source_root / name).resolve(), f"{name}: source belongs to another animal/age")
            check(record["file"] == name, f"{name}: original file identity differs")
            upload_field = "original_upload" if species == "bear" else "original_filename"
            check(isinstance(record[upload_field], str) and record[upload_field].strip(),
                  f"{name}: original uploaded filename must be preserved")
            for field in ("file", upload_field, "sha256"):
                check(record[field] == source_index[age][kind][field],
                      f"{name}: manifest disagrees with original upload index ({field})")
            sha = digest(path)
            check(sha == record["sha256"] == ORIGINAL_HASHES[species, age, kind],
                  f"{name}: original bytes changed or belong to another animal/age")
            check(path not in source_paths and sha not in source_hashes,
                  f"{name}: source reused for another animal, age or sheet")
            source_paths.add(path)
            source_hashes.add(sha)
            with Image.open(path) as image:
                expected_size = (1448, 1086) if (species, age) == ("fox", "cub") else (1536, 1024)
                check(image.size == expected_size and list(image.size) == record["dimensions"],
                      f"{name}: original dimensions differ from the supplied sheet")
                sizes[age, kind] = image.size
    return sizes


def other_animal_pixels():
    whole, frames = set(), set()
    for species in ("leopard", "wolf", "tiger", "lion"):
        for path in RUNTIME.glob(f"res*/drawable*/{species}_*"):
            if not path.is_file():
                continue
            with Image.open(path) as image:
                whole.add(pixel_digest(image))
                if image.height == FRAME and image.width % FRAME == 0:
                    for start in range(0, image.width, FRAME):
                        frames.add(pixel_digest(image.crop((start, 0, start + FRAME, FRAME))))
    check(whole and frames, "Historical animal art is required for anti-substitution checks")
    return whole, frames


def validate_cutout_probes(species, manifest):
    """Check inspected points before scaling, in the original crop coordinates."""
    importer = importlib.import_module(f"prepare_v0811_{species}_assets")
    probes = manifest.get("cutout_probes", [])
    check(probes, f"{species}: audited background and character-preservation probes required")
    assets = {(record["age"], record["action"]): record for record in manifest["assets"].values()}
    sheets, cutouts, coverage = {}, {}, set()
    care_checked = scratcher_checked = False
    for index, probe in enumerate(probes, 1):
        label = f"{species} cutout probe {index}"
        age, action, frame_index = probe["age"], probe["action"], probe["frame"]
        point, expected = probe["point"], probe["expect"]
        check(probe.get("space") == "source_cell", f"{label}: unknown coordinate space")
        check(bool(probe.get("description")), f"{label}: inspection rationale is missing")
        check((age, action) in assets, f"{label}: action does not exist at this age")
        record = assets[age, action]
        check(isinstance(frame_index, int) and 1 <= frame_index <= record["frames"],
              f"{label}: frame lies outside its animation")
        check(expected in {"transparent", "opaque"}, f"{label}: unexpected alpha criterion")
        source_kind = record["source"]
        sheet_key = age, source_kind
        if sheet_key not in sheets:
            with Image.open(local_path(manifest["sources"][age][source_kind]["path"])) as image:
                sheets[sheet_key] = image.convert("RGBA")
        key = age, action, frame_index
        if key not in cutouts:
            if source_kind == "main":
                source_action = action.replace("run_", "walk_", 1) if action.startswith("run_") else action
                cutouts[key] = importer.extract_main_pose(sheets[sheet_key], age, source_action, frame_index - 1)
            else:
                check(frame_index == 1, f"{label}: object poses have one original frame")
                cutouts[key] = importer.extract_object_pose(sheets[sheet_key], age, action)
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
                  f"{label}: opaque probe must preserve visible animal/object pixels")
            check(alpha >= minimum, f"{label} {key}: protected pixels lost at {point}, alpha={alpha}")
        coverage.add((age, expected))
        care_checked |= action in {"groom_foam", "soap", "comb", "towel"}
        scratcher_checked |= action == "scratcher_play"
    check(coverage == {(age, expected) for age in AGES for expected in ("transparent", "opaque")},
          f"{species}: every age needs both background-removal and preserved-character probes")
    check(care_checked and scratcher_checked,
          f"{species}: probes must cover care and separation from the fixed scratcher")
    print(f"{species} cutout probes: PASS — {len(probes)} source-space background/fur/object checks")
    return len(probes)


def main():
    other_pixels, other_frame_pixels = other_animal_pixels()
    source_paths, source_hashes = set(), set()
    pack_pixel_owners, frame_pixel_owners = {}, {}
    total_frames = total_probes = total_outputs = 0
    for species in SPECIES:
        manifest = json.loads((ROOT / f"{species}-sprite-manifest.json").read_text())
        check(manifest["version"] == "0.8.11", f"{species}: wrong integration version")
        check(manifest["species"] == species, f"{species}: manifest belongs to another animal")
        check(manifest["frame_size"] == FRAME, f"{species}: common 256px canvas required")
        check(manifest["safe_margin"] == SAFE, f"{species}: common 16px safety margin required")
        source_sizes = validate_sources(species, manifest, source_paths, source_hashes)
        check(set(manifest["normalization"]) == set(AGES), f"{species}: normalization must cover all four ages")
        for age, settings in manifest["normalization"].items():
            check(settings["baseline"] == FRAME - SAFE, f"{species}/{age}: shared foot baseline differs")
            check(0 < settings["largest_component_target_area"] <= (FRAME - 2 * SAFE) ** 2,
                  f"{species}/{age}: invalid target character area")
        assets, portraits = manifest["assets"], manifest["portraits"]
        check(len(assets) == 109, f"{species}: expected 109 gameplay resources")
        check(len(portraits) == 4, f"{species}: every age needs its own promenade portrait")
        check(not set(assets) & set(portraits), f"{species}: gameplay and portraits must be disjoint")
        records = {**assets, **portraits}
        expected_paths, expected_assets, expected_portraits = set(), set(), set()
        species_frames = 0
        for age in AGES:
            pack = species, age
            prefix = f"{species}_{age}_"
            frame_counts = dict(FRAMES)
            if age == "cub":
                frame_counts["bottle.webp"] = 1
            frame_counts["promenade_token.png"] = 1
            folder = RUNTIME / f"res-{species}-{age}/drawable-nodpi"
            expected = {prefix + name: count for name, count in frame_counts.items()}
            actual = {path.name for path in folder.iterdir() if path.is_file()}
            check(actual == set(expected),
                  f"{species}/{age}: missing={sorted(set(expected)-actual)}, unexpected={sorted(actual-set(expected))}")
            for name, count in expected.items():
                is_portrait = name.endswith("_promenade_token.png")
                (expected_portraits if is_portrait else expected_assets).add(name)
                record = records[name]
                path = local_path(record["path"])
                expected_paths.add(path)
                check(path == (folder / name).resolve(), f"{name}: resource leaves its own animal/age folder")
                check(record["age"] == age, f"{name}: manifest age differs")
                action = name[len(prefix):].rsplit(".", 1)[0]
                if not is_portrait:
                    check(record["action"] == action, f"{name}: manifest action differs")
                check(record["frames"] == count, f"{name}: supplied frame count differs")
                check((record["width"], record["height"]) == (FRAME * count, FRAME),
                      f"{name}: declared dimensions disagree with the runtime contract")
                source_kind = record["source"]
                is_main = is_portrait or action.startswith(("idle_", "walk_", "run_")) or action in {"jump", "eat", "sleep", "moods"}
                check(source_kind == ("main" if is_main else "objects"), f"{name}: wrong type of original sheet")
                cells = record["source_cells"]
                check(len(cells) == count, f"{name}: every frame needs its original crop")
                width, height = source_sizes[age, source_kind]
                for cell in cells:
                    check(len(cell) == 4 and all(isinstance(value, int) for value in cell)
                          and 0 <= cell[0] < cell[2] <= width and 0 <= cell[1] < cell[3] <= height,
                          f"{name}: crop lies outside its original source: {cell}")
                check(bool(record["derivation"]), f"{name}: extraction method must be recorded")
                check(digest(path) == record["sha256"], f"{name}: output differs from its current manifest")
                with Image.open(path) as image:
                    check(image.size == (FRAME * count, FRAME), f"{name}: wrong actual dimensions {image.size}")
                    check("A" in image.getbands(), f"{name}: opaque output has no transparency")
                    decoded = pixel_digest(image)
                    check(decoded == record["rgba_sha256"], f"{name}: decoded artwork differs from audited RGBA")
                    check(decoded not in other_pixels, f"{name}: historical animal artwork was substituted")
                    if decoded in pack_pixel_owners:
                        check(pack_pixel_owners[decoded][0] == pack,
                              f"{name}: artwork reused from another animal/age: {pack_pixel_owners[decoded]}")
                    pack_pixel_owners[decoded] = pack, name
                    for index in range(count):
                        frame = image.crop((index * FRAME, 0, (index + 1) * FRAME, FRAME))
                        frame_hash = pixel_digest(frame)
                        check(frame_hash not in other_frame_pixels,
                              f"{name} frame {index + 1}: historical animal frame was substituted")
                        if frame_hash in frame_pixel_owners:
                            check(frame_pixel_owners[frame_hash][0] == pack,
                                  f"{name} frame {index + 1}: frame reused from another animal/age: {frame_pixel_owners[frame_hash]}")
                        frame_pixel_owners[frame_hash] = pack, name, index + 1
                        alpha = frame.getchannel("A")
                        bounds = alpha.getbbox()
                        check(bounds is not None, f"{name} frame {index + 1}: empty sprite")
                        check(alpha.getextrema() == (0, 255), f"{name} frame {index + 1}: background/opaque character missing")
                        check(bounds[0] >= SAFE and bounds[1] >= SAFE
                              and bounds[2] <= FRAME - SAFE and bounds[3] <= FRAME - SAFE,
                              f"{name} frame {index + 1}: clipping risk at {bounds}")
                species_frames += count
            for direction in ("down", "left", "right", "up"):
                walk = assets[prefix + f"walk_{direction}.webp"]
                run = assets[prefix + f"run_{direction}.webp"]
                check(run["source_cells"] == walk["source_cells"] and run["source"] == walk["source"],
                      f"{species}/{age}/{direction}: run must use the six same-pack walk poses")
                check(run["rgba_sha256"] == walk["rgba_sha256"], f"{species}/{age}/{direction}: run/walk drawings differ")
            for action in ("idle", "walk", "run"):
                extension = "png" if action == "idle" else "webp"
                for first, second in (("left", "right"), ("down", "up")):
                    check(assets[prefix + f"{action}_{first}.{extension}"]["rgba_sha256"] !=
                          assets[prefix + f"{action}_{second}.{extension}"]["rgba_sha256"],
                          f"{species}/{age}: {action} {first}/{second} incorrectly share the same drawing")
            print(f"OK {species} {age}: {len(expected)-1} gameplay assets + portrait, own sources, 256px/16px margins")
        check(set(assets) == expected_assets, f"{species}: unregistered or absent gameplay resource")
        check(set(portraits) == expected_portraits, f"{species}: unregistered or absent promenade portrait")
        actual_paths = {path.resolve() for path in RUNTIME.glob(f"res*/drawable*/{species}_*") if path.is_file()}
        check(actual_paths == expected_paths, f"{species}: resources exist outside their four age packs")
        check(species_frames == 349, f"{species}: expected 349 frames including four head tokens, got {species_frames}")
        total_frames += species_frames
        total_outputs += len(records)
        total_probes += validate_cutout_probes(species, manifest)
    check(len(source_paths) == 16 and len(source_hashes) == 16, "Exactly sixteen distinct original sheets are required")
    check(total_outputs == 226 and total_frames == 698, "Incomplete combined fox/bear resource set")
    print(f"Forest sprite validation: PASS — 16 original sheets, 226 outputs, 698 transparent frames, {total_probes} alpha probes; no animal/age substitution")


if __name__ == "__main__":
    main()
