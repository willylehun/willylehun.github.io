#!/usr/bin/env python3
from pathlib import Path
from collections import deque
from PIL import Image, ImageFilter

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"source-assets"/"v056"
RUNTIME=ROOT/"app"/"src"/"main"
AGES=("cub","teen","adult","old")

def white_to_alpha(im):
    im=im.convert("RGBA")
    a=im.getchannel("A")
    if a.getextrema()[0] < 250:
        return im
    rgb=im.convert("RGB")
    w,h=rgb.size
    px=rgb.load()
    cand=[[False]*w for _ in range(h)]
    for y in range(h):
        row=cand[y]
        for x in range(w):
            r,g,b=px[x,y]
            row[x]=(min(r,g,b)>188 and max(r,g,b)-min(r,g,b)<40) or min(r,g,b)>235
    bg=[[False]*w for _ in range(h)]
    q=deque()
    for x in range(w):
        for y in (0,h-1):
            if cand[y][x] and not bg[y][x]: bg[y][x]=True;q.append((x,y))
    for y in range(h):
        for x in (0,w-1):
            if cand[y][x] and not bg[y][x]: bg[y][x]=True;q.append((x,y))
    while q:
        x,y=q.popleft()
        for xx,yy in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
            if 0<=xx<w and 0<=yy<h and cand[yy][xx] and not bg[yy][xx]:
                bg[yy][xx]=True;q.append((xx,yy))
    alpha=Image.new("L",(w,h),255); ap=alpha.load()
    for y in range(h):
        for x in range(w):
            if bg[y][x]: ap[x,y]=0
    alpha=alpha.filter(ImageFilter.GaussianBlur(.7))
    im.putalpha(alpha)
    return im

def pad_frame(im,size=640,maxw=525,maxh=500,bottom=588):
    im=white_to_alpha(im)
    bbox=im.getchannel("A").getbbox()
    if not bbox: raise RuntimeError("image vide")
    sub=im.crop(bbox)
    scale=min(maxw/sub.width,maxh/sub.height)
    sub=sub.resize((max(1,round(sub.width*scale)),max(1,round(sub.height*scale))),Image.Resampling.LANCZOS)
    out=Image.new("RGBA",(size,size),(0,0,0,0))
    x=(size-sub.width)//2
    y=max(34,min(size-sub.height-24,bottom-sub.height))
    out.alpha_composite(sub,(x,y))
    return out

def face_frame(im):
    im=white_to_alpha(im)
    bbox=im.getchannel("A").getbbox()
    if not bbox: raise RuntimeError("face vide")
    sub=im.crop(bbox)
    scale=min(286/sub.width,286/sub.height)
    sub=sub.resize((max(1,round(sub.width*scale)),max(1,round(sub.height*scale))),Image.Resampling.LANCZOS)
    out=Image.new("RGBA",(320,320),(0,0,0,0))
    out.alpha_composite(sub,((320-sub.width)//2,max(10,306-sub.height)))
    return out

def save_webp(im,path,quality=92):
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path,"WEBP",quality=quality,method=6,exact=True)

def prepare_age(age):
    src=SRC/f"res-{age}"/"drawable-nodpi"
    dst=RUNTIME/f"res-{age}"/"drawable-nodpi"
    dst.mkdir(parents=True,exist_ok=True)

    # Poses : marge de sécurité en haut pour que les pointes d'oreilles ne touchent jamais le canevas.
    statics={}
    for mood in ("idle","happy","tired","sleep"):
        name=f"leopard_{age}_{mood}.png"
        frame=pad_frame(Image.open(src/name))
        frame.save(dst/name,"PNG",optimize=True)
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
    living.save(dst/"room_living_hd.webp","WEBP",quality=90,method=6)

    # Jardin : recadrage avant la barrière de premier plan, puis remise au format 4:3.
    garden=Image.open(SRC/"rooms"/"room_garden_hd.webp").convert("RGB")
    w,h=garden.size
    crop_h=round(h*.66)
    crop_w=round(crop_h*4/3)
    left=(w-crop_w)//2
    garden=garden.crop((left,0,left+crop_w,crop_h)).resize((1536,1152),Image.Resampling.LANCZOS)
    garden.save(dst/"room_garden_hd.webp","WEBP",quality=90,method=6)

def main():
    for age in AGES: prepare_age(age)
    prepare_rooms()
    print("v0.5.7 runtime assets prepared: ears padded, CUB 5-frame walks, face atlases cleaned, garden foreground fence removed.")

if __name__=="__main__":
    main()
