#!/usr/bin/env python3
"""Validate all tiger resources against their eight immutable original sheets.

The shared frame contract is imported from the wolf validator because the user
supplied the same animation layout. Source hashes, provenance and decoded pixels
are checked independently of the generator so a renamed or reencoded animal
cannot silently be substituted for a tiger.
"""

from pathlib import Path
import hashlib
import json

from PIL import Image

from validate_wolf_sprite_packs import AGES, FRAME, FRAMES, SAFE, check, digest


ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / "app/src/main"
SOURCE = ROOT / "source-assets/tiger-v089"
ORIGINAL_HASHES = {
    ("cub", "main"): "57484bc67ef7ab9885e6f44a2ea09a0115bcd7ba92052fe379b97c7178b061d6",
    ("cub", "objects"): "ca401b27d4117d3c062997b62a87bf3f69e101ca645a1e1139d2970083bc23a1",
    ("teen", "main"): "953e10668cd2e3057f003e5f781695a9c19877a3a1021b255fd5c60eda8ce9cd",
    ("teen", "objects"): "4eb240681b5ac763e9b4dc0cbe0e66776564392fa3aae8992eaa32d4a865a6ed",
    ("adult", "main"): "8116da4b3ef2f247fe0c76023117d7aed73c3fcb070726a4850c373c8121dd7e",
    ("adult", "objects"): "23fdb2f0b52334e46555a2ccaef03462be36d139364c72dfc0ae37963e64433a",
    ("old", "main"): "135c39746cebfbd59212eabb3f7dc77fd88aab7e62b062b640ad511fe891f5ae",
    ("old", "objects"): "02d53ab2a2ca2096af32c395ad0036d7df8ff7d47c0570089f20d6660b431bcc",
}


def local_path(relative):
    path = (ROOT / relative).resolve()
    check(path.is_relative_to(ROOT), f"Manifest path leaves project: {relative}")
    check(path.is_file(), f"Missing file: {relative}")
    return path


def pixel_digest(image):
    rgba = image.convert("RGBA")
    # Ignore encoder-specific RGB stored behind fully transparent pixels.
    rgba = Image.alpha_composite(Image.new("RGBA", rgba.size), rgba)
    return hashlib.sha256(str(rgba.size).encode() + rgba.tobytes()).hexdigest()


def validate_cutout_probes(manifest):
    """Check visually audited background/fur points in original crop coordinates."""
    from prepare_v089_tiger_assets import extract_main_pose, extract_object_pose

    probes = manifest.get("cutout_probes", [])
    check(probes, "Tiger cutouts need audited background and preserved-character probes")
    assets = {(record["age"], record["action"]): record for record in manifest["assets"].values()}
    sheets, cutouts, coverage = {}, {}, set()
    care_checked = scratcher_checked = False
    for index, probe in enumerate(probes, 1):
        label = f"Cutout probe {index}"
        age, action, frame_index = probe["age"], probe["action"], probe["frame"]
        point, expected = probe["point"], probe["expect"]
        check(probe.get("space") == "source_cell", f"{label}: unknown coordinate space")
        check(bool(probe.get("description")), f"{label}: inspection rationale is missing")
        check((age, action) in assets, f"{label}: action does not exist for this age")
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
                cutouts[key] = extract_main_pose(sheets[sheet_key], age, source_action, frame_index-1)
            else:
                check(frame_index == 1, f"{label}: object poses have one source frame")
                cutouts[key] = extract_object_pose(sheets[sheet_key], age, action)
        cutout = cutouts[key]
        check(len(point) == 2 and all(isinstance(value, int) for value in point)
              and 0 <= point[0] < cutout.width and 0 <= point[1] < cutout.height,
              f"{label}: point lies outside its original crop")
        alpha = cutout.getchannel("A").getpixel(tuple(point))
        if expected == "transparent":
            check(alpha == 0, f"{label} {age}/{action}/{frame_index}: background remains at {point}, alpha={alpha}")
        else:
            minimum = probe.get("min_alpha", 255)
            check(isinstance(minimum, int) and 128 <= minimum <= 255,
                  f"{label}: an opaque probe must preserve visible character/object pixels")
            check(alpha >= minimum,
                  f"{label} {age}/{action}/{frame_index}: protected pixels lost at {point}, alpha={alpha}")
        coverage.add((age, expected))
        care_checked |= action in {"groom_foam", "soap", "comb", "towel"}
        scratcher_checked |= action == "scratcher_play"
    check(coverage == {(age, expected) for age in AGES for expected in ("transparent", "opaque")},
          "Each tiger age needs both background-removal and character-preservation probes")
    check(care_checked and scratcher_checked, "Cutout probes must cover care artwork and the separate scratcher prop")
    print(f"Tiger cutout probes: PASS — {len(probes)} source-space background/fur/object checks across four ages")


def main():
    manifest = json.loads((ROOT / "tiger-sprite-manifest.json").read_text())
    check(manifest["species"] == "tiger", "Manifest species must be tiger")
    check(manifest["frame_size"] == FRAME, "All animals must use the same 256px canvas")
    check(manifest["safe_margin"] == SAFE, "Tiger sprites need the shared 16px safety margin")
    check(set(manifest["sources"]) == set(AGES), "Each tiger age needs its own original source pair")
    source_index = json.loads((SOURCE / "sources.json").read_text())
    check(set(source_index) == set(AGES), "The original upload index must cover all four ages")

    source_sizes = {}
    source_paths = set()
    for age in AGES:
        records = manifest["sources"][age]
        check(set(records) == {"main", "objects"}, f"{age}: exactly two original source sheets")
        check(set(source_index[age]) == {"main", "objects"}, f"{age}: exactly two upload records")
        for kind, record in records.items():
            path = local_path(record["path"])
            name = f"tiger_{age}_{kind}_source.png"
            check(path == (SOURCE / name).resolve(), f"{age}/{kind}: source must belong to its age")
            check(record["file"] == name, f"{age}/{kind}: original file identity differs")
            for field in ("file", "original_filename", "sha256"):
                check(record[field] == source_index[age][kind][field],
                      f"{age}/{kind}: manifest disagrees with the original upload index ({field})")
            check(digest(path) == record["sha256"] == ORIGINAL_HASHES[age, kind],
                  f"{name}: original uploaded bytes changed or belong to a different age")
            check(path not in source_paths, f"{name}: source reused by another age or sheet")
            source_paths.add(path)
            with Image.open(path) as image:
                check(list(image.size) == record["dimensions"], f"{name}: source dimensions changed")
                source_sizes[age, kind] = image.size
    check(len(source_paths) == 8, "Exactly eight distinct original tiger sheets are required")

    assets, portraits = manifest["assets"], manifest["portraits"]
    check(len(assets) == 109, "Expected 109 tiger gameplay resources")
    check(len(portraits) == 4, "Each tiger age needs its own promenade portrait")
    check(not set(assets) & set(portraits), "Gameplay resources and portraits must be disjoint")
    records = {**assets, **portraits}

    other_hashes, other_pixels, other_frame_pixels = set(), set(), set()
    for species in ("leopard", "wolf"):
        for path in RUNTIME.glob(f"res*/drawable*/{species}_*"):
            if not path.is_file():
                continue
            other_hashes.add(digest(path))
            with Image.open(path) as image:
                other_pixels.add(pixel_digest(image))
                if image.height == FRAME and image.width % FRAME == 0:
                    for start in range(0, image.width, FRAME):
                        other_frame_pixels.add(pixel_digest(image.crop((start, 0, start + FRAME, FRAME))))

    expected_paths, expected_assets, expected_portraits = set(), set(), set()
    age_pixel_owners, frame_pixel_owners = {}, {}
    total_frames = 0
    for age in AGES:
        frame_counts = dict(FRAMES)
        if age == "cub":
            frame_counts["bottle.webp"] = 1
        frame_counts["promenade_token.png"] = 1
        folder = RUNTIME / f"res-tiger-{age}/drawable-nodpi"
        expected = {f"tiger_{age}_{name}": count for name, count in frame_counts.items()}
        actual = {path.name for path in folder.iterdir() if path.is_file()}
        check(actual == set(expected),
              f"{age}: missing={sorted(set(expected)-actual)}, unexpected={sorted(actual-set(expected))}")
        for name, count in expected.items():
            is_portrait = name.endswith("_promenade_token.png")
            (expected_portraits if is_portrait else expected_assets).add(name)
            record = records[name]
            path = local_path(record["path"])
            expected_paths.add(path)
            check(path == (folder / name).resolve(), f"{name}: resource leaves its own age folder")
            check(record["age"] == age, f"{name}: manifest age mismatch")
            check(record["frames"] == count, f"{name}: supplied frame count changed")
            check((record["width"], record["height"]) == (FRAME * count, FRAME),
                  f"{name}: declared dimensions disagree with the runtime animation contract")
            source_kind = record["source"]
            is_main = is_portrait or any(f"_{action}" in name for action in
                ("idle_", "walk_", "run_", "jump.", "eat.", "sleep.", "moods."))
            check(source_kind == ("main" if is_main else "objects"),
                  f"{name}: action uses the wrong kind of source sheet")
            cells = record["source_cells"]
            check(len(cells) == count, f"{name}: each runtime frame needs its original crop")
            width, height = source_sizes[age, source_kind]
            for cell in cells:
                check(len(cell) == 4 and all(isinstance(value, int) for value in cell)
                      and 0 <= cell[0] < cell[2] <= width and 0 <= cell[1] < cell[3] <= height,
                      f"{name}: crop outside its original source: {cell}")
            check(bool(record["derivation"]), f"{name}: extraction method must be recorded")
            file_hash = digest(path)
            check(file_hash == record["sha256"], f"{name}: output differs from its audited manifest")
            check(file_hash not in other_hashes, f"{name}: a leopard/wolf asset was substituted")
            with Image.open(path) as image:
                check(image.size == (FRAME * count, FRAME), f"{name}: actual dimensions {image.size}")
                check("A" in image.getbands(), f"{name}: opaque sheet has no transparency")
                decoded = pixel_digest(image)
                check(decoded not in other_pixels, f"{name}: reencoded leopard/wolf artwork was substituted")
                if decoded in age_pixel_owners:
                    check(age_pixel_owners[decoded][0] == age,
                          f"{name}: drawing reused from another age: {age_pixel_owners[decoded]}")
                age_pixel_owners[decoded] = (age, name)
                for index in range(count):
                    frame = image.crop((index * FRAME, 0, (index + 1) * FRAME, FRAME))
                    frame_hash = pixel_digest(frame)
                    check(frame_hash not in other_frame_pixels,
                          f"{name} frame {index + 1}: a leopard/wolf frame was substituted")
                    if frame_hash in frame_pixel_owners:
                        check(frame_pixel_owners[frame_hash][0] == age,
                              f"{name} frame {index + 1}: frame reused from another age: {frame_pixel_owners[frame_hash]}")
                    frame_pixel_owners[frame_hash] = (age, name, index + 1)
                    alpha = frame.getchannel("A")
                    bounds = alpha.getbbox()
                    check(bounds is not None, f"{name} frame {index + 1}: empty sprite")
                    check(alpha.getextrema() == (0, 255),
                          f"{name} frame {index + 1}: missing transparent background or opaque character")
                    check(bounds[0] >= SAFE and bounds[1] >= SAFE
                          and bounds[2] <= FRAME-SAFE and bounds[3] <= FRAME-SAFE,
                          f"{name} frame {index + 1}: clipping risk at {bounds}")
            total_frames += count
        for direction in ("down", "left", "right", "up"):
            walk = assets[f"tiger_{age}_walk_{direction}.webp"]
            run = assets[f"tiger_{age}_run_{direction}.webp"]
            check(run["source_cells"] == walk["source_cells"] and run["source"] == walk["source"],
                  f"{age}/{direction}: running must reuse the six supplied same-age walking poses")
            with Image.open(local_path(walk["path"])) as walk_image, Image.open(local_path(run["path"])) as run_image:
                check(pixel_digest(walk_image) == pixel_digest(run_image),
                      f"{age}/{direction}: walking and running drawings differ")
        check("mirror" in assets[f"tiger_{age}_idle_left.png"]["derivation"],
              f"{age}: left-facing idle must record the correction of its right-facing source")
        print(f"OK tiger {age}: {len(expected)-1} gameplay assets + portrait, own age sources, 256px canvas/16px margins")

    check(set(assets) == expected_assets, "Manifest contains an absent or unregistered gameplay resource")
    check(set(portraits) == expected_portraits, "Each promenade portrait must stay separate from gameplay")
    actual_paths = {path.resolve() for path in RUNTIME.glob("res*/drawable*/tiger_*") if path.is_file()}
    check(actual_paths == expected_paths,
          "Tiger resources exist outside their four age packs: " + str(sorted(map(str, actual_paths-expected_paths))))
    validate_cutout_probes(manifest)
    print(f"Tiger sprite validation: PASS — 8 original sheets, 113 outputs, {total_frames} transparent frames, no age/species substitution")


if __name__ == "__main__":
    main()
