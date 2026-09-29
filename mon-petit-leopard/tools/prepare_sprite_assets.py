from PIL import Image
from collections import deque
import os, math

ROOT=os.path.dirname(os.path.dirname(__file__))
RES=os.path.join(ROOT,"app","src","main","res","drawable-nodpi")
AGES=("cub","teen","adult","old")
STATES=("idle","happy","tired","sleep")
DIRECTIONS=("side","front","back")
CANVAS=320
PAD=26
BOTTOM=22

def rgb_metrics(px):
    r,g,b=px[:3]
    mx=max(r,g,b); mn=min(r,g,b)
    sat=mx-mn
    bright=(r+g+b)/3.0
    return r,g,b,sat,bright

def crop_subject(frame):
    im=frame.convert("RGBA")
    w,h=im.size
    pix=im.load()

    # 1) Find warm/saturated "leopard" anchor pixels and crop around them.
    anchors=[]
    for y in range(h):
        for x in range(w):
            r,g,b,a=pix[x,y]
            if a<16: continue
            mx=max(r,g,b); mn=min(r,g,b)
            sat=mx-mn
            # orange/brown/black fur seed
            if (sat>42 and r>65 and r>=g*0.92 and r>b*1.04) or (r<105 and g<95 and b<90 and sat>12):
                anchors.append((x,y))
    if anchors:
        xs=[p[0] for p in anchors]; ys=[p[1] for p in anchors]
        l=max(0,min(xs)-16); t=max(0,min(ys)-16)
        rr=min(w,max(xs)+17); bb=min(h,max(ys)+17)
        im=im.crop((l,t,rr,bb))

    # 2) Flood-fill neutral/white/dark background from the crop edges.
    w,h=im.size
    arr=im.load()
    bg=[[False]*w for _ in range(h)]
    q=deque()

    def is_bg_candidate(x,y):
        r,g,b,a=arr[x,y]
        if a<18: return True
        mx=max(r,g,b); mn=min(r,g,b)
        sat=mx-mn
        bright=(r+g+b)/3.0
        return (sat<52 and bright>145) or bright>246 or bright<30

    for x in range(w):
        if is_bg_candidate(x,0): q.append((x,0))
        if is_bg_candidate(x,h-1): q.append((x,h-1))
    for y in range(h):
        if is_bg_candidate(0,y): q.append((0,y))
        if is_bg_candidate(w-1,y): q.append((w-1,y))

    while q:
        x,y=q.popleft()
        if x<0 or y<0 or x>=w or y>=h or bg[y][x]: continue
        if not is_bg_candidate(x,y): continue
        bg[y][x]=True
        q.extend(((x+1,y),(x-1,y),(x,y+1),(x,y-1)))

    out=Image.new("RGBA",(w,h),(0,0,0,0))
    op=out.load()
    for y in range(h):
        for x in range(w):
            r,g,b,a=arr[x,y]
            if bg[y][x] or a<18:
                continue
            op[x,y]=(r,g,b,a)

    # 3) Keep only the connected component containing the most warm fur pixels.
    p=out.load()
    seen=[[False]*w for _ in range(h)]
    components=[]
    for sy in range(h):
        for sx in range(w):
            if seen[sy][sx] or p[sx,sy][3]<18: continue
            stack=[(sx,sy)]; seen[sy][sx]=True
            comp=[]; score=0
            while stack:
                x,y=stack.pop()
                comp.append((x,y))
                r,g,b,a=p[x,y]
                mx=max(r,g,b); mn=min(r,g,b)
                sat=mx-mn
                if (sat>42 and r>65 and r>=g*0.92 and r>b*1.04) or (r<105 and g<95 and b<90 and sat>12):
                    score+=1
                for nx,ny in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):
                    if 0<=nx<w and 0<=ny<h and not seen[ny][nx] and p[nx,ny][3]>=18:
                        seen[ny][nx]=True; stack.append((nx,ny))
            components.append((score,len(comp),comp))
    if components:
        components.sort(key=lambda z:(z[0],z[1]),reverse=True)
        keep=set(components[0][2])
        for y in range(h):
            for x in range(w):
                if (x,y) not in keep:
                    p[x,y]=(0,0,0,0)

    bbox=out.getbbox()
    if bbox:
        l,t,r,b=bbox
        l=max(0,l-3); t=max(0,t-3); r=min(w,r+3); b=min(h,b+3)
        out=out.crop((l,t,r,b))
    return out

def fit_frames(frames):
    maxw=max(im.width for im in frames); maxh=max(im.height for im in frames)
    scale=min((CANVAS-2*PAD)/maxw,(CANVAS-PAD-BOTTOM)/maxh)
    result=[]
    for im in frames:
        nw=max(1,round(im.width*scale)); nh=max(1,round(im.height*scale))
        rs=im.resize((nw,nh),Image.Resampling.LANCZOS)
        cv=Image.new("RGBA",(CANVAS,CANVAS),(0,0,0,0))
        x=(CANVAS-nw)//2
        y=max(PAD,CANVAS-BOTTOM-nh)
        cv.alpha_composite(rs,(x,y))
        result.append(cv)
    return result

def split_strip(im):
    h=im.height
    count=max(1,round(im.width/float(max(1,h))))
    fw=im.width//count
    return [im.crop((i*fw,0,(i+1)*fw,im.height)) for i in range(count)]

def prepare_static(src,dst):
    im=Image.open(src).convert("RGBA")
    clean=fit_frames([crop_subject(im)])[0]
    clean.save(dst,optimize=True)

def prepare_strip(src,dst):
    im=Image.open(src).convert("RGBA")
    frames=[crop_subject(f) for f in split_strip(im)]
    frames=fit_frames(frames)
    strip=Image.new("RGBA",(CANVAS*len(frames),CANVAS),(0,0,0,0))
    for i,f in enumerate(frames):
        strip.alpha_composite(f,(i*CANVAS,0))
    strip.save(dst,optimize=True)

for age in AGES:
    for state in STATES:
        src=os.path.join(RES,f"leopard_{age}_{state}.png")
        dst=os.path.join(RES,f"leopard_{age}_{state}_v2.png")
        if os.path.exists(src):
            prepare_static(src,dst)
            print("prepared",os.path.basename(dst))
    for direction in DIRECTIONS:
        src=os.path.join(RES,f"leopard_{age}_walk_{direction}.webp")
        dst=os.path.join(RES,f"leopard_{age}_walk_{direction}_v2.png")
        if os.path.exists(src):
            prepare_strip(src,dst)
            print("prepared",os.path.basename(dst))
