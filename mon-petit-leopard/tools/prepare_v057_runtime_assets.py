#!/usr/bin/env python3
from pathlib import Path
from collections import deque
from PIL import Image, ImageFilter
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"source-assets"/"v056"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")

def white_to_alpha(im):
    im=im.convert("RGBA")
    original_alpha=np.asarray(im.getchannel("A"),dtype=np.uint8)
    rgb=np.asarray(im.convert("RGB"))
    h,w,_=rgb.shape
    mn=rgb.min(axis=2); mx=rgb.max(axis=2)
    cand=((mn>188)&((mx-mn)<40)) | (mn>235)
    bg=np.zeros((h,w),dtype=np.bool_)
    q=deque()
    for x in range(w):
        if cand[0,x]: bg[0,x]=True;q.append((x,0))
        if cand[h-1,x] and not bg[h-1,x]: bg[h-1,x]=True;q.append((x,h-1))
    for y in range(h):
        if cand[y,0] and not bg[y,0]: bg[y,0]=True;q.append((0,y))
        if cand[y,w-1] and not bg[y,w-1]: bg[y,w-1]=True;q.append((w-1,y))
    while q:
        x,y=q.popleft()
        if x>0 and cand[y,x-1] and not bg[y,x-1]: bg[y,x-1]=True;q.append((x-1,y))
        if x+1<w and cand[y,x+1] and not bg[y,x+1]: bg[y,x+1]=True;q.append((x+1,y))
        if y>0 and cand[y-1,x] and not bg[y-1,x]: bg[y-1,x]=True;q.append((x,y-1))
        if y+1<h and cand[y+1,x] and not bg[y+1,x]: bg[y+1,x]=True;q.append((x,y+1))
    cleaned=np.where(bg,0,original_alpha).astype("uint8")
    alpha=Image.fromarray(cleaned,"L").filter(ImageFilter.GaussianBlur(.55))
    im.putalpha(alpha)
    return im

def pad_frame(im,size=640,maxw=500,maxh=470,bottom=588):
    im=white_to_alpha(im)
    bbox=im.getchannel("A").getbbox()
    if not bbox: raise RuntimeError("image vide")
    sub=im.crop(bbox)
    scale=min(maxw/sub.width,maxh/sub.height)
    sub=sub.resize((max(1,round(sub.width*scale)),max(1,round(sub.height*scale))),Image.Resampling.LANCZOS)
    out=Image.new("RGBA",(size,size),(0,0,0,0))
    x=(size-sub.width)//2
    y=max(54,min(size-sub.height-30,bottom-sub.height))
    out.alpha_composite(sub,(x,y))
    return out

def face_frame(im):
    im=white_to_alpha(im)
    bbox=im.getchannel("A").getbbox()
    if not bbox: raise RuntimeError("face vide")
    sub=im.crop(bbox)
    scale=min(270/sub.width,260/sub.height)
    sub=sub.resize((max(1,round(sub.width*scale)),max(1,round(sub.height*scale))),Image.Resampling.LANCZOS)
    out=Image.new("RGBA",(320,320),(0,0,0,0))
    out.alpha_composite(sub,((320-sub.width)//2,max(26,300-sub.height)))
    return out

def save_webp(im,path,quality=92):
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path,"WEBP",quality=quality,method=2,exact=True)

def prepare_age(age):
    src=SRC/f"res-{age}"/"drawable-nodpi"
    dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
    dst.mkdir(parents=True,exist_ok=True)

    # Poses : marge de sécurité en haut pour que les pointes d'oreilles ne touchent jamais le canevas.
    statics={}
    for mood in ("idle","happy","tired","sleep"):
        name=f"leopard_{age}_{mood}.png"
        frame=pad_frame(Image.open(src/name))
        frame.save(dst/name,"PNG",optimize=False)
        statics[mood]=frame

    # Marches : même taille, même ancrage des pieds, aucun mélange inter-âge.
    for mode in ("side","front","back"):
        name=f"leopard_{age}_walk_{mode}.webp"
        strip=Image.open(src/name).convert("RGBA")
        source_count=strip.width//640
        frames=[pad_frame(strip.crop((i*640,0,(i+1)*640,640))) for i in range(source_count)]
        if age=="cub":
            # 5 phases demandées. La 2e phase est reprise en fermeture de boucle,
            # plutôt que d'introduire une image d'une mauvaise orientation.
            order=[0,1,2,3,1]
        else:
            order=list(range(min(4,len(frames))))
        out=Image.new("RGBA",(640*len(order),640),(0,0,0,0))
        for i,src_i in enumerate(order): out.alpha_composite(frames[src_i],(i*640,0))
        save_webp(out,dst/name,92)

    # Humeurs face : fond blanc retiré, oreilles remarginées.
    face_name=f"leopard_{age}_face_moods.webp"
    atlas=Image.open(src/face_name).convert("RGBA")
    count=atlas.width//320
    faces=[face_frame(atlas.crop((i*320,0,(i+1)*320,320))) for i in range(count)]
    if age=="cub":
        # 11 premières humeurs + la pose assise/idle, soit 12 vues face.
        idle=statics["idle"].resize((320,320),Image.Resampling.LANCZOS)
        faces.append(idle)
    out=Image.new("RGBA",(320*len(faces),320),(0,0,0,0))
    for i,f in enumerate(faces): out.alpha_composite(f,(i*320,0))
    save_webp(out,dst/face_name,94)

def prepare_rooms():
    dst=RUNTIME/"res"/"drawable-nodpi"
    dst.mkdir(parents=True,exist_ok=True)
    # Salon : image source conservée ; la grille de déplacement évite totalement le guéridon.
    living=Image.open(SRC/"rooms"/"room_living_hd.webp").convert("RGB")
    living.save(dst/"room_living_hd.webp","WEBP",quality=90,method=2)

    # Cuisine : suppression complète de l'îlot/table et des tabourets.
    # La source v056 n'avait pas encore de copie dédiée de la cuisine :
    # on utilise alors l'asset runtime présent dans le dépôt comme source.
    kitchen_src=SRC/"rooms"/"room_kitchen_hd.webp"
    if not kitchen_src.exists():
        kitchen_src=dst/"room_kitchen_hd.webp"
    kitchen=Image.open(kitchen_src).convert("RGB")
    kw,kh=kitchen.size
    cut=round(kh*.55)
    floor_src=kitchen.crop((0,round(kh*.84),kw,kh))
    floor=floor_src.resize((kw,kh-cut),Image.Resampling.BICUBIC)
    kitchen.paste(floor,(0,cut))
    # léger fondu de raccord sous les meubles du fond
    seam=Image.new("RGB",(kw,40))
    for yy in range(40):
        a=yy/39
        upper=kitchen.crop((0,cut-20+yy,kw,cut-19+yy))
        lower=floor.crop((0,yy,kw,yy+1))
        seam.paste(Image.blend(upper,lower,a),(0,yy))
    kitchen.paste(seam,(0,cut-20))
    kitchen.save(dst/"room_kitchen_hd.webp","WEBP",quality=92,method=2)

    # Jardin : recadrage avant la barrière de premier plan, puis remise au format 4:3.
    garden=Image.open(SRC/"rooms"/"room_garden_hd.webp").convert("RGB")
    w,h=garden.size
    crop_h=round(h*.66)
    crop_w=round(crop_h*4/3)
    left=(w-crop_w)//2
    garden=garden.crop((left,0,left+crop_w,crop_h)).resize((1536,1152),Image.Resampling.LANCZOS)
    garden.save(dst/"room_garden_hd.webp","WEBP",quality=90,method=2)

def main():
    for age in AGES: prepare_age(age)
    prepare_rooms()
    print("v0.5.8 runtime assets prepared: ears padded, CUB 5-frame walks, face atlases cleaned, garden foreground fence removed.")

if __name__=="__main__":
    main()
