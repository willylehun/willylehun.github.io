#!/usr/bin/env python3
from __future__ import annotations

import base64
import io
import math
import shutil
import zipfile
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source-assets"/"v070-fetch-bundle"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")
TOYS=("ball","tennis","yarn","mouse","plush")
FRAME=256
SAFE=16

def _neighbors4(y,x,h,w):
    if y: yield y-1,x
    if y+1<h: yield y+1,x
    if x: yield y,x-1
    if x+1<w: yield y,x+1

def _largest_component(im:Image.Image,threshold:int=20):
    a=np.array(im.convert("RGBA").getchannel("A"))
    mask=a>threshold
    h,w=mask.shape
    seen=np.zeros((h,w),dtype=bool)
    best_area=0
    best_bbox=None
    ys,xs=np.nonzero(mask)
    for sy,sx in zip(ys.tolist(),xs.tolist()):
        if seen[sy,sx]:
            continue
        q=deque([(sy,sx)])
        seen[sy,sx]=True
        area=0
        minx=maxx=sx
        miny=maxy=sy
        while q:
            y,x=q.popleft()
            area+=1
            minx=min(minx,x);maxx=max(maxx,x)
            miny=min(miny,y);maxy=max(maxy,y)
            for ny,nx in _neighbors4(y,x,h,w):
                if mask[ny,nx] and not seen[ny,nx]:
                    seen[ny,nx]=True
                    q.append((ny,nx))
        if area>best_area:
            best_area=area
            best_bbox=(minx,miny,maxx+1,maxy+1)
    return best_area,best_bbox

def _normalize(im:Image.Image,target_area:int)->Image.Image:
    im=im.convert("RGBA")
    area,_=_largest_component(im)
    if area<=0:
        raise RuntimeError("asset jeu vide")
    bbox=im.getchannel("A").getbbox()
    if not bbox:
        raise RuntimeError("asset jeu sans alpha")

    obj=im.crop(bbox)
    scale=math.sqrt(target_area/float(area))
    nw=max(1,int(round(obj.width*scale)))
    nh=max(1,int(round(obj.height*scale)))

    max_dim=FRAME-2*SAFE
    if nw>max_dim or nh>max_dim:
        k=min(max_dim/float(nw),max_dim/float(nh))
        nw=max(1,int(round(nw*k)))
        nh=max(1,int(round(nh*k)))

    obj=obj.resize((nw,nh),Image.Resampling.LANCZOS)
    out=Image.new("RGBA",(FRAME,FRAME),(0,0,0,0))
    x=(FRAME-nw)//2
    y=FRAME-SAFE-nh
    y=max(SAFE,min(FRAME-SAFE-nh,y))
    out.alpha_composite(obj,(x,y))
    return out

def _load_bundle():
    parts=sorted(SOURCE.glob("fetch_assets_v070.b64.part*"))
    if len(parts)!=8:
        raise RuntimeError(f"bundle v070 incomplet: {len(parts)} parties")
    payload="".join(p.read_text().strip() for p in parts)
    raw=base64.b64decode(payload,validate=True)
    return zipfile.ZipFile(io.BytesIO(raw),"r")

def main():
    with _load_bundle() as z:
        for age in AGES:
            dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
            dst.mkdir(parents=True,exist_ok=True)

            idle=Image.open(dst/f"leopard_{age}_idle_down.png").convert("RGBA")
            target_area,_=_largest_component(idle)
            if target_area<=0:
                raise RuntimeError(f"{age}: idle_down illisible")

            # 4 ressources de course distinctes, copiées depuis les cycles déjà
            # normalisés afin de garantir exactement la même taille visuelle.
            for direction in ("down","left","right","up"):
                src=dst/f"leopard_{age}_walk_{direction}.webp"
                run=dst/f"leopard_{age}_run_{direction}.webp"
                shutil.copyfile(src,run)

            for toy in TOYS:
                raw=z.read(f"{age}/fetch_{age}_{toy}.png")
                frame=Image.open(io.BytesIO(raw)).convert("RGBA")
                frame=_normalize(frame,target_area)
                frame.save(dst/f"leopard_{age}_fetch_{toy}.png","PNG",optimize=False)

            # Fallback déterministe : fabrique un strip corde 5 frames à partir
            # de la pose pelote. Un pack artistique 5 frames déjà présent peut
            # remplacer ce fichier sans modifier le moteur.
            base=Image.open(dst/f"leopard_{age}_fetch_yarn.png").convert("RGBA")
            rope=Image.new("RGBA",(FRAME*5,FRAME),(0,0,0,0))
            transforms=((0,0,0),(-3,1,-2),(2,-1,2),(-2,0,-1),(3,1,1))
            for i,(dx,dy,angle) in enumerate(transforms):
                frame=base.rotate(angle,Image.Resampling.BICUBIC,expand=False)
                shifted=Image.new("RGBA",(FRAME,FRAME),(0,0,0,0))
                shifted.alpha_composite(frame,(dx,dy))
                rope.alpha_composite(shifted,(i*FRAME,0))
            rope.save(dst/f"leopard_{age}_rope_play.png","PNG",optimize=False)

            print(f"OK {age}: run 4 directions + 5 jouets + corde 5 frames, échelle alignée sur idle_down")

    print("OK v0.7.2: assets de jeu salon préparés pour les quatre âges.")

if __name__=="__main__":
    main()
