#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
import hashlib

ROOT=Path(__file__).resolve().parents[1]/"app"/"src"/"main"
AGES=("cub","teen","adult","old")
FRAME=256
EXPECTED={
    "idle_down.png":(256,256),"idle_left.png":(256,256),
    "idle_right.png":(256,256),"idle_up.png":(256,256),
    "walk_down.webp":(1536,256),"walk_left.webp":(1536,256),
    "walk_right.webp":(1536,256),"walk_up.webp":(1536,256),
    "jump.webp":(1280,256),"eat.webp":(768,256),
    "sleep.webp":(768,256),"moods.webp":(3072,256),
}

def fail(msg):
    raise SystemExit("ERREUR SPRITES v0.6.0: "+msg)

def main():
    common=ROOT/"res"/"drawable-nodpi"
    leftovers=list(common.glob("leopard_*")) if common.exists() else []
    if leftovers: fail("sprites dans la zone commune: "+", ".join(p.name for p in leftovers))

    hashes={}
    for age in AGES:
        folder=ROOT/f"res-{age}"/"drawable-nodpi"
        expected={f"leopard_{age}_{tail}":size for tail,size in EXPECTED.items()}
        actual={p.name for p in folder.glob("leopard_*") if p.is_file()}
        if actual!=set(expected):
            fail(f"{age}: manquants={sorted(set(expected)-actual)}, en trop={sorted(actual-set(expected))}")
        for name,size in expected.items():
            p=folder/name
            with Image.open(p) as im:
                im.load()
                if im.size!=size: fail(f"{age}/{name}: {im.size}, attendu {size}")
                rgba=im.convert("RGBA")
                step=FRAME
                count=im.width//FRAME
                for i in range(count):
                    b=rgba.crop((i*step,0,(i+1)*step,FRAME)).getchannel("A").getbbox()
                    if not b: fail(f"{age}/{name} frame {i+1}: vide")
                    if b[0]<6 or b[1]<6 or b[2]>250 or b[3]>250:
                        fail(f"{age}/{name} frame {i+1}: risque de rognage {b}")
            digest=hashlib.sha256(p.read_bytes()).hexdigest()
            prev=hashes.get(digest)
            if prev and prev[0]!=age: fail(f"asset identique entre {prev} et {(age,name)}")
            hashes[digest]=(age,name)
        print(f"OK {age}: 51 frames, canevas 256px, aucune frame rognée")
    print("OK v0.6.0: tailles, marges, transparence et séparation des âges validées.")

if __name__=="__main__":
    main()
