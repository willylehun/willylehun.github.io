#!/usr/bin/env python3
from __future__ import annotations

import base64
import io
import zipfile
import hashlib
from pathlib import Path
from collections import deque

import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCE_BUNDLE=ROOT/"source-assets"/"v060-bundle"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")
FRAME=256
COLS=9
ROWS=6
SAFE_MARGIN=16

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

EXPECTED_BUNDLE_SHA256="cad88d994fe493d26454df9b3ffb4218d1df235848ba3b5cc87190187d14ce3c"

def load_bundle():
    parts=sorted(SOURCE_BUNDLE.glob("v060_assets.b64.part*"))
    if len(parts)!=10:
        raise RuntimeError(f"bundle source v0.6.5 incomplet: {len(parts)} parties")
    payload="".join(p.read_text().strip() for p in parts)
    raw=base64.b64decode(payload,validate=True)
    digest=hashlib.sha256(raw).hexdigest()
    if digest!=EXPECTED_BUNDLE_SHA256:
        raise RuntimeError(f"mauvais bundle sprites: {digest}")
    return zipfile.ZipFile(io.BytesIO(raw),"r")

def _neighbors4(y:int,x:int,h:int,w:int):
    if y: yield y-1,x
    if y+1<h: yield y+1,x
    if x: yield y,x-1
    if x+1<w: yield y,x+1

def _flat_dark_mask(arr:np.ndarray)->np.ndarray:
    """
    Repère uniquement les plaques sombres/neutres et presque uniformes.
    C'est le défaut rectangulaire observé derrière certains sprites.
    Le masque reste volontairement conservateur pour ne pas attaquer le pelage.
    """
    rgb=arr[:,:,:3].astype(np.int16)
    a=arr[:,:,3]
    mx=rgb.max(axis=2)
    mn=rgb.min(axis=2)
    neutral=(mx-mn)<=22
    dark=mx<=112

    # Variation locale : une vraie zone parasite est plate, contrairement
    # aux contours/rosettes/ombres du léopard.
    diffs=[]
    diffs.append(np.abs(rgb-np.roll(rgb,1,axis=0)).max(axis=2))
    diffs.append(np.abs(rgb-np.roll(rgb,-1,axis=0)).max(axis=2))
    diffs.append(np.abs(rgb-np.roll(rgb,1,axis=1)).max(axis=2))
    diffs.append(np.abs(rgb-np.roll(rgb,-1,axis=1)).max(axis=2))
    grad=np.minimum.reduce(diffs)
    cand=(a>24)&neutral&dark&(grad<=9)
    cand[[0,-1],:]=False
    cand[:,[0,-1]]=False
    return cand

def _remove_rectangular_dark_artifacts(im:Image.Image)->Image.Image:
    arr=np.array(im.convert("RGBA"))
    cand=_flat_dark_mask(arr)
    h,w=cand.shape
    seen=np.zeros((h,w),dtype=bool)
    remove=np.zeros((h,w),dtype=bool)

    ys,xs=np.nonzero(cand)
    for sy,sx in zip(ys.tolist(),xs.tolist()):
        if seen[sy,sx]:
            continue
        q=deque([(sy,sx)])
        seen[sy,sx]=True
        pts=[]
        miny=maxy=sy
        minx=maxx=sx
        while q:
            y,x=q.popleft()
            pts.append((y,x))
            miny=min(miny,y); maxy=max(maxy,y)
            minx=min(minx,x); maxx=max(maxx,x)
            for ny,nx in _neighbors4(y,x,h,w):
                if cand[ny,nx] and not seen[ny,nx]:
                    seen[ny,nx]=True
                    q.append((ny,nx))

        area=len(pts)
        bh=maxy-miny+1
        bw=maxx-minx+1
        fill=area/float(max(1,bh*bw))

        # Plaque parasite: surface notable + géométrie compacte/rectangulaire.
        # On épargne les petits détails noirs du personnage.
        aspect=max(bw,bh)/float(max(1,min(bw,bh)))
        if area>=600 and bw>=20 and bh>=20 and fill>=0.78 and (aspect>=1.30 or area>=1200):
            for y,x in pts:
                remove[y,x]=True

    if remove.any():
        # Dilatation 1 px pour supprimer le liseré de la plaque, mais seulement
        # sur des pixels eux-mêmes sombres/neutres afin de préserver le sujet.
        grown=remove.copy()
        grown[1:,:] |= remove[:-1,:]
        grown[:-1,:] |= remove[1:,:]
        grown[:,1:] |= remove[:,:-1]
        grown[:,:-1] |= remove[:,1:]
        rgb=arr[:,:,:3].astype(np.int16)
        mx=rgb.max(axis=2); mn=rgb.min(axis=2)
        grown &= ((mx-mn)<=30)&(mx<=128)
        arr[grown,3]=0
        arr[grown,0:3]=0

    return Image.fromarray(arr,"RGBA")

def _alpha_bbox(im:Image.Image):
    return im.getchannel("A").getbbox()

def _decontaminate_alpha_edges(im:Image.Image)->Image.Image:
    """Supprime les halos de détourage sans rogner le personnage."""
    arr=np.array(im.convert("RGBA")).copy()
    alpha=arr[:,:,3]
    arr[alpha<=2]=0
    alpha=arr[:,:,3]
    arr[:,:,3][alpha>=250]=255
    alpha=arr[:,:,3]

    solid=alpha>=235
    pending=(alpha>2)&(alpha<235)
    filled=solid.copy()
    h,w=alpha.shape

    for _ in range(8):
        sums=np.zeros((h,w,3),dtype=np.int32)
        counts=np.zeros((h,w),dtype=np.int16)
        for dy,dx in ((-1,0),(1,0),(0,-1),(0,1),(-1,-1),(-1,1),(1,-1),(1,1)):
            ys0=max(0,-dy); ys1=min(h,h-dy)
            xs0=max(0,-dx); xs1=min(w,w-dx)
            yd0=ys0+dy; yd1=ys1+dy
            xd0=xs0+dx; xd1=xs1+dx
            m=filled[ys0:ys1,xs0:xs1]
            if not m.any():
                continue
            rgb=arr[ys0:ys1,xs0:xs1,:3].astype(np.int32)
            sums[yd0:yd1,xd0:xd1]+=rgb*m[:,:,None]
            counts[yd0:yd1,xd0:xd1]+=m.astype(np.int16)
        take=pending & (~filled) & (counts>0)
        if not take.any():
            break
        arr[take,:3]=(sums[take]/counts[take,None]).astype(np.uint8)
        filled[take]=True

    arr[arr[:,:,3]==0,:3]=0
    return Image.fromarray(arr,"RGBA")

def _safe_fit(im:Image.Image)->Image.Image:
    """
    Garantit que le sujet complet reste à l'intérieur du canevas 256x256.
    Un sprite déjà bien marginé n'est pas ré-échantillonné.
    """
    im=im.convert("RGBA")
    b=_alpha_bbox(im)
    if not b:
        raise RuntimeError("frame vide")

    left,top,right,bottom=b
    if left>=SAFE_MARGIN and top>=SAFE_MARGIN and right<=FRAME-SAFE_MARGIN and bottom<=FRAME-SAFE_MARGIN:
        return im

    # Recadre le contenu existant puis le réduit juste assez pour récupérer
    # une marge de sécurité. Aucun crop du sujet n'est autorisé.
    pad=2
    x0=max(0,left-pad); y0=max(0,top-pad)
    x1=min(FRAME,right+pad); y1=min(FRAME,bottom+pad)
    obj=im.crop((x0,y0,x1,y1))
    ow,oh=obj.size
    maxw=FRAME-2*SAFE_MARGIN
    maxh=FRAME-2*SAFE_MARGIN
    scale=min(1.0,maxw/float(ow),maxh/float(oh))
    nw=max(1,int(round(ow*scale)))
    nh=max(1,int(round(oh*scale)))
    if (nw,nh)!=(ow,oh):
        obj=obj.resize((nw,nh),Image.Resampling.LANCZOS)

    out=Image.new("RGBA",(FRAME,FRAME),(0,0,0,0))
    x=(FRAME-nw)//2
    # Ancrage bas cohérent avec les packs précédents.
    y=FRAME-SAFE_MARGIN-nh
    if y<SAFE_MARGIN:
        y=SAFE_MARGIN
    out.alpha_composite(obj,(x,y))
    return out

def _normalize_cub_sleep_first_frame(im:Image.Image)->Image.Image:
    """Réduit uniquement la 1re frame de sommeil CUB pour supprimer le saut d'échelle."""
    im=im.convert("RGBA")
    b=_alpha_bbox(im)
    if not b:
        return im
    left,top,right,bottom=b
    width=right-left
    if width<=155:
        return im
    target_width=150
    scale=target_width/float(width)
    obj=im.crop(b)
    nw=max(1,int(round(obj.width*scale)))
    nh=max(1,int(round(obj.height*scale)))
    obj=obj.resize((nw,nh),Image.Resampling.LANCZOS)
    out=Image.new("RGBA",(FRAME,FRAME),(0,0,0,0))
    x=(FRAME-nw)//2
    anchor_bottom=min(bottom,FRAME-SAFE_MARGIN)
    y=max(SAFE_MARGIN,anchor_bottom-nh)
    out.alpha_composite(obj,(x,y))
    return _decontaminate_alpha_edges(out)

def _cub_sleep_transform(i:int,im:Image.Image)->Image.Image:
    return _normalize_cub_sleep_first_frame(im) if i==0 else im

def clean_cell(atlas:Image.Image,index:int)->Image.Image:
    x=(index%COLS)*FRAME
    y=(index//COLS)*FRAME
    out=atlas.crop((x,y,x+FRAME,y+FRAME)).convert("RGBA")
    out=_remove_rectangular_dark_artifacts(out)
    out=_decontaminate_alpha_edges(out)
    out=_safe_fit(out)
    out=_decontaminate_alpha_edges(out)
    b=_alpha_bbox(out)
    if not b:
        raise RuntimeError(f"frame {index} vide après nettoyage")
    if b[0]<SAFE_MARGIN or b[1]<SAFE_MARGIN or b[2]>FRAME-SAFE_MARGIN or b[3]>FRAME-SAFE_MARGIN:
        raise RuntimeError(f"frame {index} trop proche du bord après nettoyage: {b}")
    return out

def strip(atlas:Image.Image,start:int,count:int,transform=None)->Image.Image:
    out=Image.new("RGBA",(FRAME*count,FRAME),(0,0,0,0))
    for i in range(count):
        frame=clean_cell(atlas,start+i)
        if transform is not None:
            frame=transform(i,frame)
        out.alpha_composite(frame,(i*FRAME,0))
    return out

def save_png(im:Image.Image,path:Path):
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path,"PNG",optimize=False)

def save_webp(im:Image.Image,path:Path):
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path,"WEBP",lossless=True,method=6,exact=True)

def prepare_age(z:zipfile.ZipFile,age:str):
    raw=z.read(f"{age}/leopard_{age}_atlas.webp")
    atlas=Image.open(io.BytesIO(raw)).convert("RGBA")
    if atlas.size!=(FRAME*COLS,FRAME*ROWS):
        raise RuntimeError(f"{age}: atlas {atlas.size}, attendu {(FRAME*COLS,FRAME*ROWS)}")

    dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
    dst.mkdir(parents=True,exist_ok=True)
    for old in dst.glob("leopard_*"):
        old.unlink()

    for key in ("idle_down","idle_left","idle_right","idle_up"):
        save_png(clean_cell(atlas,OFFSETS[key]),dst/f"leopard_{age}_{key}.png")

    for key in ("walk_down","walk_left","walk_right","walk_up"):
        save_webp(strip(atlas,OFFSETS[key],COUNTS[key]),dst/f"leopard_{age}_{key}.webp")

    save_webp(strip(atlas,OFFSETS["jump"],5),dst/f"leopard_{age}_jump.webp")
    save_webp(strip(atlas,OFFSETS["eat"],3),dst/f"leopard_{age}_eat.webp")
    sleep_transform=_cub_sleep_transform if age=="cub" else None
    save_webp(strip(atlas,OFFSETS["sleep"],3,sleep_transform),dst/f"leopard_{age}_sleep.webp")
    save_webp(strip(atlas,OFFSETS["moods"],12),dst/f"leopard_{age}_moods.webp")

def main():
    with load_bundle() as z:
        for age in AGES:
            prepare_age(z,age)
    print("v0.6.6: packs réintégrés, sommeil CUB normalisé, contours nettoyés.")
    print("Marge 16px, sommeil CUB sans saut de taille, suppression des halos et rognages.")

if __name__=="__main__":
    main()
