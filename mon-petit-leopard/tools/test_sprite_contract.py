from pathlib import Path
import re, subprocess, tempfile, hashlib
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/byw/monpetitleopard'
main=(JAVA/'MainActivity.java').read_text()

assert 'MIN_SLEEP_MS=90000L' in main
assert 'MOOD_DURATION_MS=4000L' in main
assert 'MIN_IDLE_DOWN_SHARE=.30f' in main
assert 'CharacterSprites.FRAME_SIZE' in main
assert 'startMoodExitUp()' in main
assert 'ActionAnim' in main

for age in ['cub','teen','adult','old']:
    folder=ROOT/f'app/src/main/res-{age}/drawable-nodpi'
    expected={
        f'leopard_{age}_idle_down.png':(256,256),
        f'leopard_{age}_idle_left.png':(256,256),
        f'leopard_{age}_idle_right.png':(256,256),
        f'leopard_{age}_idle_up.png':(256,256),
        f'leopard_{age}_walk_down.webp':(1536,256),
        f'leopard_{age}_walk_left.webp':(1536,256),
        f'leopard_{age}_walk_right.webp':(1536,256),
        f'leopard_{age}_walk_up.webp':(1536,256),
        f'leopard_{age}_jump.webp':(1280,256),
        f'leopard_{age}_eat.webp':(768,256),
        f'leopard_{age}_sleep.webp':(768,256),
        f'leopard_{age}_moods.webp':(3072,256),
    }
    assert {p.name for p in folder.glob('leopard_*')}==set(expected)
    for name,size in expected.items():
        with Image.open(folder/name) as im:
            im.load()
            assert im.size==size,(name,im.size,size)

with tempfile.TemporaryDirectory() as temp:
    p=Path(temp)
    chars=(JAVA/'CharacterSprites.java').read_text()
    (p/'CharacterSprites.java').write_text(chars)
    names=sorted(set(re.findall(r'R.drawable.(leopard_\w+)',chars)))
    (p/'R.java').write_text('package com.byw.monpetitleopard; final class R { static class drawable {'+
        ''.join('static final int '+n+'='+str(i+1)+';' for i,n in enumerate(names))+'}}')
    (p/'MainActivity.java').write_text(
        'package com.byw.monpetitleopard; class MainActivity {'+
        ' enum PetStage {CUB,TEEN,ADULT,OLD} enum TravelDirection {LEFT,RIGHT,UP,DOWN} }')
    (p/'ContractTest.java').write_text("""package com.byw.monpetitleopard;
class ContractTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  java.util.HashSet<Integer> seen=new java.util.HashSet<>();
  for(MainActivity.PetStage age:MainActivity.PetStage.values()){
   CharacterSprites.Pack p=CharacterSprites.forStage(age);
   check(p.stage==age);
   check(p.expectedWidth(p.idleDown)==256);
   check(p.expectedWidth(p.walkDown)==1536);
   check(p.expectedWidth(p.jump)==1280);
   check(p.expectedWidth(p.eat)==768);
   check(p.expectedWidth(p.sleep)==768);
   check(p.expectedWidth(p.moods)==3072);
   for(int id:p.allResources())check(id!=0&&seen.add(id));
  }
  System.out.println("Sprite registry v0.6.7: PASS");
 }
}""")
    subprocess.run(['javac','-d',str(p),*[str(f) for f in p.glob('*.java')]],check=True)
    subprocess.run(['java','-cp',str(p),'com.byw.monpetitleopard.ContractTest'],check=True)

print('Sprite contract v0.6.7: PASS')


# Décors HD v0.6.7 : dimensions natives 4:3 et contrôle du contenu exact.
expected_backgrounds={
    'room_kitchen_hd.webp':'089b81eaa7abf3c691b4e9a9e885c31a8c751f0eb3fbd68a98fa022f10179723',
    'room_garden_hd.webp':'9965028f7f921c410fb70397f1c35492d88910578380feb2a506c1f2eae6fe41',
}
for bg,expected_sha in expected_backgrounds.items():
    p=ROOT/'app/src/main/res/drawable-nodpi'/bg
    with Image.open(p) as im:
        im.load()
        assert im.size==(1536,1152),(bg,im.size)
    assert hashlib.sha256(p.read_bytes()).hexdigest()==expected_sha, bg


# Régression v0.6.7 : oreilles intactes + walk-up sans rognage.
for name in ['leopard_cub_walk_right.webp','leopard_cub_walk_up.webp']:
    p=ROOT/'app/src/main/res-cub/drawable-nodpi'/name
    with Image.open(p) as strip:
        strip=strip.convert('RGBA')
        assert strip.size==(1536,256),(name,strip.size)
        for i in range(6):
            frame=strip.crop((i*256,0,(i+1)*256,256))
            b=frame.getchannel('A').getbbox()
            assert b is not None,(name,i,'vide')
            assert b[0]>=16 and b[1]>=16 and b[2]<=240 and b[3]<=240,(name,i,b)

# Régression v0.6.7 : normalisation globale d'échelle, âge par âge.
def visible_bbox(im,threshold=20):
    a=im.getchannel('A')
    import numpy as _np
    arr=_np.array(a)
    ys,xs=_np.nonzero(arr>threshold)
    if len(xs)==0:
        return None
    return (int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1))

def frame_extent(frame):
    b=visible_bbox(frame)
    assert b is not None
    return max(b[2]-b[0],b[3]-b[1]),b

frame_counts={
    'idle_down':1,'idle_left':1,'idle_right':1,'idle_up':1,
    'walk_down':6,'walk_left':6,'walk_right':6,'walk_up':6,
    'jump':5,'eat':3,'sleep':3,'moods':12,
}
for age in ['cub','teen','adult','old']:
    folder=ROOT/f'app/src/main/res-{age}/drawable-nodpi'
    idle_extents=[]
    for key in ['idle_down','idle_left','idle_right','idle_up']:
        with Image.open(folder/f'leopard_{age}_{key}.png') as im:
            e,b=frame_extent(im.convert('RGBA'))
            idle_extents.append(e)
    target=round(sum(idle_extents)/len(idle_extents))
    assert max(idle_extents)-min(idle_extents)<=2,(age,'idle incoherent',idle_extents)

    for key,count in frame_counts.items():
        ext='png' if key.startswith('idle_') else 'webp'
        with Image.open(folder/f'leopard_{age}_{key}.{ext}') as strip:
            strip=strip.convert('RGBA')
            assert strip.size==(256*count,256),(age,key,strip.size)
            for i in range(count):
                frame=strip.crop((i*256,0,(i+1)*256,256))
                extent,b=frame_extent(frame)
                assert abs(extent-target)<=2,(age,key,i,extent,target,b)
                assert b[0]>=16 and b[1]>=16 and b[2]<=240 and b[3]<=240,(age,key,i,b)

print('Normalisation globale d\'échelle v0.6.7: PASS')
