#!/usr/bin/env python3
from __future__ import annotations

import base64
import io
import zipfile
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source-assets"/"v078-garden-scratcher"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")

def load_bundle():
    parts=sorted(SOURCE.glob("garden_scratcher_assets_v078.b64.part*"))
    if len(parts)!=5:
        raise RuntimeError(f"bundle griffoir v0.7.8 incomplet: {len(parts)} parties")
    payload="".join(p.read_text().strip() for p in parts)
    raw=base64.b64decode(payload,validate=True)
    return zipfile.ZipFile(io.BytesIO(raw),"r")

def main():
    expected={f"leopard_{age}_scratcher_play.webp" for age in AGES}
    expected.add("garden_scratcher.webp")
    with load_bundle() as z:
        names=set(z.namelist())
        if names!=expected:
            raise RuntimeError(f"bundle griffoir inattendu: manquants={sorted(expected-names)}, en trop={sorted(names-expected)}")
        for name in sorted(names):
            raw=z.read(name)
            with Image.open(io.BytesIO(raw)) as im:
                im.load()
                rgba=im.convert("RGBA")
                if name=="garden_scratcher.webp":
                    if im.size!=(256,256):
                        raise RuntimeError(f"{name}: {im.size}, attendu 256x256")
                    dst=RUNTIME/"res"/"drawable-nodpi"
                else:
                    if im.size!=(512,256):
                        raise RuntimeError(f"{name}: {im.size}, attendu 512x256")
                    age=name.split("_")[1]
                    dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
                    # Deux frames strictement séparées, sans morceau du griffoir.
                    for i in range(2):
                        frame=rgba.crop((i*256,0,(i+1)*256,256))
                        b=frame.getchannel("A").getbbox()
                        if b is None:
                            raise RuntimeError(f"{name} frame {i}: vide")
                        if b[0]<10 or b[1]<10 or b[2]>246 or b[3]>246:
                            raise RuntimeError(f"{name} frame {i}: risque de rognage {b}")
                if rgba.getchannel("A").getbbox() is None:
                    raise RuntimeError(f"{name}: asset vide")
            dst.mkdir(parents=True,exist_ok=True)
            (dst/name).write_bytes(raw)
    print("OK v0.7.8: griffoir fixe 256x256 + léopards seuls en strips 2 frames 512x256.")

if __name__=="__main__":
    main()
