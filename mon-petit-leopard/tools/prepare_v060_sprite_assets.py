#!/usr/bin/env python3
from __future__ import annotations

import base64
import io
import zipfile
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
BUNDLE=ROOT/"source-assets"/"v060-bundle"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")
FRAME=256
COLS=9
ROWS=6

OFFSETS={
    "idle_down":0,"idle_left":1,"idle_right":2,"idle_up":3,
    "walk_down":4,"walk_left":10,"walk_right":16,"walk_up":22,
    "jump":28,"eat":33,"sleep":36,"moods":39,
}
COUNTS={
    "idle_down":1,"idle_left":1,"idle_right":1,"idle_up":1,
    "walk_down":6,"walk_left":6,"walk_right":6,"walk_up":6,
    "jump":5,"eat":3,"sleep":3,"moods":12,
}

def load_bundle():
    parts=sorted(BUNDLE.glob("v060_assets.b64.part*"))
    if len(parts)!=10:
        raise RuntimeError(f"bundle v0.6.0 incomplet: {len(parts)} parties")
    payload="".join(p.read_text().strip() for p in parts)
    raw=base64.b64decode(payload,validate=True)
    return zipfile.ZipFile(io.BytesIO(raw),"r")

def alpha_bounds(im:Image.Image):
    return im.getchannel("A").getbbox()

def cell(atlas:Image.Image,index:int)->Image.Image:
    x=(index%COLS)*FRAME
    y=(index//COLS)*FRAME
    out=atlas.crop((x,y,x+FRAME,y+FRAME)).convert("RGBA")
    b=alpha_bounds(out)
    if not b:
        raise RuntimeError(f"frame {index} vide")
    # Marge de sécurité : aucune partie du personnage ne doit toucher le canevas.
    if b[0]<6 or b[1]<6 or b[2]>FRAME-6 or b[3]>FRAME-6:
        raise RuntimeError(f"frame {index} trop proche du bord: {b}")
    return out

def strip(atlas:Image.Image,start:int,count:int)->Image.Image:
    out=Image.new("RGBA",(FRAME*count,FRAME),(0,0,0,0))
    for i in range(count):
        out.alpha_composite(cell(atlas,start+i),(i*FRAME,0))
    return out

def save_png(im,path):
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path,"PNG",optimize=False)

def save_webp(im,path):
    path.parent.mkdir(parents=True,exist_ok=True)
    # Lossless: aucune perte supplémentaire par rapport aux planches validées.
    im.save(path,"WEBP",lossless=True,method=6,exact=True)

def prepare_age(z,age):
    raw=z.read(f"{age}/leopard_{age}_atlas.webp")
    atlas=Image.open(io.BytesIO(raw)).convert("RGBA")
    if atlas.size!=(FRAME*COLS,FRAME*ROWS):
        raise RuntimeError(f"{age}: atlas {atlas.size}, attendu {(FRAME*COLS,FRAME*ROWS)}")

    dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
    dst.mkdir(parents=True,exist_ok=True)
    for old in dst.glob("leopard_*"):
        old.unlink()

    for key in ("idle_down","idle_left","idle_right","idle_up"):
        save_png(cell(atlas,OFFSETS[key]),dst/f"leopard_{age}_{key}.png")

    for key in ("walk_down","walk_left","walk_right","walk_up"):
        save_webp(strip(atlas,OFFSETS[key],COUNTS[key]),dst/f"leopard_{age}_{key}.webp")

    save_webp(strip(atlas,OFFSETS["jump"],5),dst/f"leopard_{age}_jump.webp")
    save_webp(strip(atlas,OFFSETS["eat"],3),dst/f"leopard_{age}_eat.webp")
    save_webp(strip(atlas,OFFSETS["sleep"],3),dst/f"leopard_{age}_sleep.webp")
    save_webp(strip(atlas,OFFSETS["moods"],12),dst/f"leopard_{age}_moods.webp")

def main():
    with load_bundle() as z:
        for age in AGES:
            prepare_age(z,age)
    print("v0.6.0: 4 packs générés en 256px natif, lossless, sans rognage.")
    print("Chaque âge: 4 idle, 4x6 walk, 5 jump, 3 eat, 3 sleep, 12 moods idle-down.")

if __name__=="__main__":
    main()
