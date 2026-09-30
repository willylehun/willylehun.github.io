#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main"
COMMON = ROOT / "res" / "drawable-nodpi"

PACKS = {
    "cub": {
        "dir": ROOT / "res-cub" / "drawable-nodpi",
        "files": {
            "leopard_cub_idle.png": (640, 640),
            "leopard_cub_happy.png": (640, 640),
            "leopard_cub_tired.png": (640, 640),
            "leopard_cub_sleep.png": (640, 640),
            "leopard_cub_walk_side.webp": (2560, 640),
            "leopard_cub_walk_front.webp": (2560, 640),
            "leopard_cub_walk_back.webp": (2560, 640),
            "leopard_cub_face_moods.webp": (3520, 320),
        },
    },
    "teen": {
        "dir": ROOT / "res-teen" / "drawable-nodpi",
        "files": {
            "leopard_teen_idle.png": (640, 640),
            "leopard_teen_happy.png": (640, 640),
            "leopard_teen_tired.png": (640, 640),
            "leopard_teen_sleep.png": (640, 640),
            "leopard_teen_walk_side.webp": (2560, 640),
            "leopard_teen_walk_front.webp": (2560, 640),
            "leopard_teen_walk_back.webp": (2560, 640),
            "leopard_teen_face_moods.webp": (3520, 320),
        },
    },
    "adult": {
        "dir": ROOT / "res-adult" / "drawable-nodpi",
        "files": {
            "leopard_adult_idle.png": (640, 640),
            "leopard_adult_happy.png": (640, 640),
            "leopard_adult_tired.png": (640, 640),
            "leopard_adult_sleep.png": (640, 640),
            "leopard_adult_walk_side.webp": (2560, 640),
            "leopard_adult_walk_front.webp": (2560, 640),
            "leopard_adult_walk_back.webp": (2560, 640),
            "leopard_adult_face_moods.webp": (3520, 320),
        },
    },
    "old": {
        "dir": ROOT / "res-old" / "drawable-nodpi",
        "files": {
            "leopard_old_idle.png": (640, 640),
            "leopard_old_happy.png": (640, 640),
            "leopard_old_tired.png": (640, 640),
            "leopard_old_sleep.png": (640, 640),
            "leopard_old_walk_side.webp": (2560, 640),
            "leopard_old_walk_front.webp": (2560, 640),
            "leopard_old_walk_back.webp": (2560, 640),
            "leopard_old_face_moods.webp": (3520, 320),
        },
    },
}

def png_size(data: bytes) -> tuple[int, int]:
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("signature PNG invalide")
    return struct.unpack(">II", data[16:24])

def webp_size(data: bytes) -> tuple[int, int]:
    if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise ValueError("signature WEBP invalide")
    kind = data[12:16]
    if kind == b"VP8X":
        w = 1 + int.from_bytes(data[24:27], "little")
        h = 1 + int.from_bytes(data[27:30], "little")
        return w, h
    if kind == b"VP8L":
        b0, b1, b2, b3 = data[21:25]
        w = 1 + b0 + ((b1 & 0x3F) << 8)
        h = 1 + ((b1 >> 6) & 0x03) + (b2 << 2) + ((b3 & 0x0F) << 10)
        return w, h
    if kind == b"VP8 ":
        marker = data.find(b"\x9d\x01\x2a", 20, 96)
        if marker < 0:
            raise ValueError("en-tête VP8 introuvable")
        w = int.from_bytes(data[marker+3:marker+5], "little") & 0x3FFF
        h = int.from_bytes(data[marker+5:marker+7], "little") & 0x3FFF
        return w, h
    raise ValueError(f"chunk WEBP non pris en charge: {kind!r}")

def image_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if path.suffix == ".png":
        return png_size(data)
    if path.suffix == ".webp":
        return webp_size(data)
    raise ValueError(f"extension non prise en charge: {path.suffix}")

def fail(msg: str) -> None:
    raise SystemExit("ERREUR SPRITES: " + msg)

def main() -> None:
    common = sorted(COMMON.glob("leopard_*")) if COMMON.exists() else []
    if common:
        fail("des sprites de personnage restent dans la zone commune: " +
             ", ".join(p.name for p in common))

    hashes: dict[str, tuple[str, str]] = {}

    for stage, info in PACKS.items():
        folder: Path = info["dir"]
        expected: dict[str, tuple[int, int]] = info["files"]

        if not folder.is_dir():
            fail(f"dossier absent pour {stage}: {folder}")

        actual = {p.name for p in folder.glob("leopard_*") if p.is_file()}
        if actual != set(expected):
            missing = sorted(set(expected) - actual)
            extra = sorted(actual - set(expected))
            fail(f"pack {stage} incorrect; manquants={missing}, en trop={extra}")

        prefix = f"leopard_{stage}_"
        for name, expected_size in expected.items():
            if not name.startswith(prefix):
                fail(f"{name} n'appartient pas au pack {stage}")

            path = folder / name
            got = image_size(path)
            if got != expected_size:
                fail(f"{stage}/{name}: dimensions {got}, attendu {expected_size}")

            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            previous = hashes.get(digest)
            if previous and previous[0] != stage:
                fail(
                    f"fichier identique partagé entre âges: "
                    f"{previous[0]}/{previous[1]} et {stage}/{name}"
                )
            hashes[digest] = (stage, name)

        print(f"OK {stage}: {len(expected)} assets séparés et valides")

    print("OK technique: 4 packs séparés, directions et atlas d’humeurs par âge disponibles.")

if __name__ == "__main__":
    main()
