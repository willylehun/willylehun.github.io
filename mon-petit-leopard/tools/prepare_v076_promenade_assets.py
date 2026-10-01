#!/usr/bin/env python3
from __future__ import annotations

import base64
import io
import zipfile
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source-assets"/"v076-promenade-bundle"
RUNTIME=ROOT/"app"/"src"/"main"/"res"/"drawable-nodpi"

def load_bundle():
    parts=sorted(SOURCE.glob("promenade_assets_v076.b64.part*"))
    if len(parts)!=9:
        raise RuntimeError(f"bundle promenade v0.7.6 incomplet: {len(parts)} parties")
    payload="".join(p.read_text().strip() for p in parts)
    raw=base64.b64decode(payload,validate=True)
    return zipfile.ZipFile(io.BytesIO(raw),"r")

def main():
    expected={"promenade_map.webp","promenade_token.webp"}
    RUNTIME.mkdir(parents=True,exist_ok=True)
    with load_bundle() as z:
        names=set(z.namelist())
        if names!=expected:
            raise RuntimeError(f"bundle promenade inattendu: {sorted(names)}")
        for name in sorted(names):
            raw=z.read(name)
            with Image.open(io.BytesIO(raw)) as im:
                im.load()
                if name=="promenade_map.webp" and im.size!=(1448,1086):
                    raise RuntimeError(f"carte: {im.size}, attendu 1448x1086")
                if name=="promenade_token.webp":
                    if im.size!=(128,128):
                        raise RuntimeError(f"jeton: {im.size}, attendu 128x128")
                    if im.convert("RGBA").getchannel("A").getbbox() is None:
                        raise RuntimeError("jeton vide")
            (RUNTIME/name).write_bytes(raw)
    print("OK v0.7.6: carte promenade 1448x1086 + jeton 128x128 préparés.")

if __name__=="__main__":
    main()
