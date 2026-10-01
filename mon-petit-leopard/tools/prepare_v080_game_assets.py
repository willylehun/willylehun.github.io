#!/usr/bin/env python3
from __future__ import annotations

import base64
import hashlib
import io
import zipfile
from pathlib import Path

from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source-assets"/"v080-game-bundle"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")
TOYS=("tennis","yarn","mouse","plush","rope")
EXPECTED_SHA256="925b9e00f0b8497373fca717f4f6f46c91f7035bdc663df2c01dc0f1fca83017"

def load_bundle():
    parts=sorted(SOURCE.glob("v080_game_assets.b64.part*"))
    if len(parts)!=8:
        raise RuntimeError(f"bundle v0.8.0 incomplet: {len(parts)} parties")
    payload="".join(p.read_text().strip() for p in parts)
    raw=base64.b64decode(payload,validate=True)
    digest=hashlib.sha256(raw).hexdigest()
    if digest!=EXPECTED_SHA256:
        raise RuntimeError(f"mauvais bundle v0.8.0: {digest}")
    return zipfile.ZipFile(io.BytesIO(raw),"r")

def validate_image(data:bytes,size:tuple[int,int],name:str):
    with Image.open(io.BytesIO(data)) as im:
        im.load()
        if im.size!=size:
            raise RuntimeError(f"{name}: {im.size} au lieu de {size}")
        rgba=im.convert("RGBA")
        lo,hi=rgba.getchannel("A").getextrema()
        if lo!=0 or hi!=255:
            raise RuntimeError(f"{name}: transparence invalide {(lo,hi)}")

def main():
    with load_bundle() as z:
        common=RUNTIME/"res"/"drawable-nodpi"
        common.mkdir(parents=True,exist_ok=True)

        for toy in TOYS:
            name=f"toy_{toy}.webp"
            data=z.read(name)
            validate_image(data,(256,256),name)
            (common/name).write_bytes(data)

        for age in AGES:
            name=f"leopard_{age}_rope_play.webp"
            data=z.read(name)
            validate_image(data,(1280,256),name)
            dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
            dst.mkdir(parents=True,exist_ok=True)
            # Écrase le fallback généré en v0.7.2 par les 5 vrais sprites
            # issus du pack artistique fourni avec le cahier des charges.
            (dst/name).write_bytes(data)

    print("OK v0.8.0: vrais jouets transparents + corde 5 frames pour les quatre âges.")

if __name__=="__main__":
    main()
