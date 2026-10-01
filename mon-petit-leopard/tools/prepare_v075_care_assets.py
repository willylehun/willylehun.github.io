#!/usr/bin/env python3
from __future__ import annotations

import base64
import io
import zipfile
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source-assets"/"v075-care-bundle"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")
ACTIONS=("groom_foam","soap","comb","towel")
FRAME=(256,256)

def load_bundle():
    parts=sorted(SOURCE.glob("care_assets_v075_q88.b64.part*"))
    if len(parts)!=7:
        raise RuntimeError(f"bundle soins v0.7.5 incomplet: {len(parts)} parties")
    payload="".join(p.read_text().strip() for p in parts)
    raw=base64.b64decode(payload,validate=True)
    return zipfile.ZipFile(io.BytesIO(raw),"r")

def main():
    expected={f"leopard_{age}_{action}.webp" for age in AGES for action in ACTIONS}
    expected.add("leopard_cub_bottle.webp")

    with load_bundle() as z:
        names=set(z.namelist())
        if names!=expected:
            raise RuntimeError(f"bundle soins inattendu: manquants={sorted(expected-names)}, en trop={sorted(names-expected)}")

        for name in sorted(names):
            age=name.split("_")[1]
            dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
            dst.mkdir(parents=True,exist_ok=True)
            raw=z.read(name)
            with Image.open(io.BytesIO(raw)) as im:
                im.load()
                if im.size!=FRAME:
                    raise RuntimeError(f"{name}: {im.size}, attendu {FRAME}")
                if im.convert("RGBA").getchannel("A").getbbox() is None:
                    raise RuntimeError(f"{name}: asset vide")
            (dst/name).write_bytes(raw)

    print("OK v0.7.5: biberon léopardeau + 16 assets de soins intégrés en 256x256.")
    print("Les tailles relatives restent gérées par le même ImageView et la même échelle de tranche d'âge.")

if __name__=="__main__":
    main()
