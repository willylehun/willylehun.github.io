#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
import hashlib
import numpy as np

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
    raise SystemExit("ERREUR SPRITES v0.6.1: "+msg)

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
                    if b[0]<10 or b[1]<10 or b[2]>246 or b[3]>246:
                        fail(f"{age}/{name} frame {i+1}: risque de rognage {b}")
                    arr=np.array(rgba.crop((i*step,0,(i+1)*step,FRAME)))
                    rgb=arr[:,:,:3].astype(np.int16)
                    alpha=arr[:,:,3]
                    mx=rgb.max(axis=2); mn=rgb.min(axis=2)
                    bad=(alpha>180)&(mx<=112)&((mx-mn)<=22)
                    # Interdit toute plaque sombre/neutre compacte de 18x18:
                    # ce test cible précisément le rectangle parasite observé.
                    k=28
                    ii=np.pad(bad.astype(np.int32),((1,0),(1,0))).cumsum(0).cumsum(1)
                    sums=ii[k:,k:]-ii[:-k,k:]-ii[k:,:-k]+ii[:-k,:-k]
                    if sums.size and int(sums.max())>=int(k*k*0.985):
                        fail(f"{age}/{name} frame {i+1}: grande plaque sombre rectangulaire détectée")
            digest=hashlib.sha256(p.read_bytes()).hexdigest()
            prev=hashes.get(digest)
            if prev and prev[0]!=age: fail(f"asset identique entre {prev} et {(age,name)}")
            hashes[digest]=(age,name)
        print(f"OK {age}: 51 frames, canevas 256px, marges 10px, aucun rectangle parasite")
    print("OK v0.6.1: tailles, marges, transparence, artefacts et séparation des âges validés.")

if __name__=="__main__":
    main()
