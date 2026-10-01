from pathlib import Path
import re, subprocess, tempfile, hashlib
from collections import deque
import numpy as np
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
assert 'return .90f+.14f*t;' in main

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
        f'leopard_{age}_run_down.webp':(1536,256),
        f'leopard_{age}_run_left.webp':(1536,256),
        f'leopard_{age}_run_right.webp':(1536,256),
        f'leopard_{age}_run_up.webp':(1536,256),
        f'leopard_{age}_fetch_ball.png':(256,256),
        f'leopard_{age}_fetch_tennis.png':(256,256),
        f'leopard_{age}_fetch_yarn.png':(256,256),
        f'leopard_{age}_fetch_mouse.png':(256,256),
        f'leopard_{age}_fetch_plush.png':(256,256),
        f'leopard_{age}_rope_play.png':(256,256),
    }
    assert {p.name for p in folder.glob('leopard_*')}==set(expected)
    for name,size in expected.items():
        with Image.open(folder/name) as im:
            im.load()
            assert im.size==size,(name,im.size,size)

with tempfile.TemporaryDirectory() as temp:
    p=Path(temp)
    chars=(JAVA/'CharacterSprites.java').read_text()
    games_java=(JAVA/'GameSprites.java').read_text()
    (p/'CharacterSprites.java').write_text(chars)
    (p/'GameSprites.java').write_text(games_java)
    (p/'SpriteMotion.java').write_text((JAVA/'SpriteMotion.java').read_text())
    names=sorted(set(re.findall(r'R.drawable.(leopard_\w+)',chars+games_java)))
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

   GameSprites.Pack g=GameSprites.forStage(age);
   check(g.stage==age);
   check(g.expectedWidth(g.runDown)==1536);
   check(g.expectedWidth(g.fetchBall)==256);
   check(g.expectedWidth(g.ropePlay)==256);
   for(int id:g.allResources())check(id!=0&&seen.add(id));
  }
  check(SpriteMotion.direction(-.10f,.25f,1f,1f)==SpriteMotion.LEFT);
  check(SpriteMotion.direction(.10f,.25f,1f,1f)==SpriteMotion.RIGHT);
  check(SpriteMotion.direction(-.04f,-.25f,1f,1f)==SpriteMotion.UP);
  check(SpriteMotion.direction(.04f,.25f,1f,1f)==SpriteMotion.DOWN);
  check(SpriteMotion.direction(0f,-.25f,1f,1f)==SpriteMotion.UP);
  check(SpriteMotion.direction(0f,.25f,1f,1f)==SpriteMotion.DOWN);
  System.out.println("Sprite registry v0.7.1: PASS");
 }
}""")
    subprocess.run(['javac','-d',str(p),*[str(f) for f in p.glob('*.java')]],check=True)
    subprocess.run(['java','-cp',str(p),'com.byw.monpetitleopard.ContractTest'],check=True)

print('Sprite contract v0.7.1: PASS')


# Décors HD v0.7.1 : dimensions natives 4:3 et contrôle du contenu exact.
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


# Régression v0.7.1 : oreilles intactes + walk-up sans rognage.
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

# Régression v0.7.1 : masse visuelle cohérente, âge par âge.
def largest_component(frame,threshold=20):
    arr=np.array(frame.getchannel('A'))
    mask=arr>threshold
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
            for dy,dx in ((-1,0),(1,0),(0,-1),(0,1)):
                ny,nx=y+dy,x+dx
                if 0<=ny<h and 0<=nx<w and mask[ny,nx] and not seen[ny,nx]:
                    seen[ny,nx]=True
                    q.append((ny,nx))
        if area>best_area:
            best_area=area
            best_bbox=(minx,miny,maxx+1,maxy+1)
    return best_area,best_bbox

frame_counts={
    'idle_down':1,'idle_left':1,'idle_right':1,'idle_up':1,
    'walk_down':6,'walk_left':6,'walk_right':6,'walk_up':6,
    'jump':5,'eat':3,'sleep':3,'moods':12,
    'run_down':6,'run_left':6,'run_right':6,'run_up':6,
    'fetch_ball':1,'fetch_tennis':1,'fetch_yarn':1,'fetch_mouse':1,'fetch_plush':1,
    'rope_play':1,
}
for age in ['cub','teen','adult','old']:
    folder=ROOT/f'app/src/main/res-{age}/drawable-nodpi'
    idle_areas=[]
    for key in ['idle_down','idle_left','idle_right','idle_up']:
        with Image.open(folder/f'leopard_{age}_{key}.png') as im:
            area,b=largest_component(im.convert('RGBA'))
            assert area>0 and b is not None
            idle_areas.append(area)
    target=float(np.median(idle_areas))

    for key,count in frame_counts.items():
        ext='png' if key.startswith('idle_') or key.startswith('fetch_') or key=='rope_play' else 'webp'
        with Image.open(folder/f'leopard_{age}_{key}.{ext}') as strip:
            strip=strip.convert('RGBA')
            assert strip.size==(256*count,256),(age,key,strip.size)
            for i in range(count):
                frame=strip.crop((i*256,0,(i+1)*256,256))
                area,b=largest_component(frame)
                assert area>0 and b is not None,(age,key,i,'vide')
                ratio=area/target
                if key.startswith('fetch_') or key=='rope_play':
                    # Les poses de jeu peuvent être plus horizontales/compactes,
                    # tout en restant visuellement proches du pack de l'âge.
                    assert .80<=ratio<=1.20,(age,key,i,area,target,ratio,b)
                else:
                    assert .95<=ratio<=1.05,(age,key,i,area,target,ratio,b)
                full=frame.getchannel('A').getbbox()
                assert full is not None
                assert full[0]>=16 and full[1]>=16 and full[2]<=240 and full[3]<=240,(age,key,i,full)

print("Normalisation de masse visuelle v0.7.1: PASS")

# Régression v0.7.1 : bêtises sans cercle, posées au sol, nettoyables au frottement direct.
assert 'incidentView.setBackground(null);' in main
assert 'incidentView.setBackground(incidentBg)' not in main
assert 'void chooseIncidentPosition()' in main
assert 'void positionIncident()' in main
assert 'incidentRoom' in main and 'incidentNX' in main and 'incidentNY' in main
assert 'chooseIncidentPosition();' in main
assert 'if(!cleaningMode){' in main
assert 'Retourne dans la pièce où la bêtise a été faite.' in main
print('Bêtises au sol et nettoyage direct v0.7.1: PASS')


# Gros changement salon v0.7.1.
objects=(JAVA/'ObjectSystem.java').read_text()
games=(JAVA/'LivingRoomGames.java').read_text()

for toy in ['"ball"','"tennis"','"yarn"','"mouse"','"plush"']:
    assert toy in objects
assert '"fishToy"' not in objects
assert '"tunnel"' not in objects
assert '"rope","Corde","🪢","salon","Jouets","rope"' in objects
assert '"bed","Repos","🛏️","salon","Repos","rest"' in objects
assert 'games.startFetch(i)' in objects
assert 'games.startRope(i)' in objects
assert 'beginAutoSleep();' in objects

assert 'class LivingRoomGames' in games
assert 'THROW_READY' in games and 'RUN_TO_TOY' in games and 'RETURNING' in games
assert 'dy<-a.dp(48)' in games
assert 'chooseLandingNode' in games
assert 'movePetToNode' in games
assert 'fastRun()' in games
assert 'ROPE_HOLD' in games

assert '📣 Appeler' in main
assert 'void callLeopard()' in main
assert 'games.onPetArrived(now)' in main
assert 'games.fastRun()' in main
assert '{.30f,.74f}' not in main
assert '{.30f,.86f}' in main and '{.50f,.95f}' in main
assert 'speed*=1.85f' in main

print('Jeux salon v0.7.1: PASS')


# Assets salon v0.7.1 : source, préparation et utilisation réelle.
prepare_game=(ROOT/'tools/prepare_v070_game_assets.py').read_text()
game_registry=(JAVA/'GameSprites.java').read_text()
assert 'v070-fetch-bundle' in prepare_game
assert 'leopard_{age}_run_' in prepare_game
assert 'leopard_{age}_fetch_' in prepare_game
assert 'static Pack forStage' in game_registry
assert 'GameSprites.forStage(stage)' in main
assert 'games.fastRun()' in main
assert 'showFetchPose()' in games
assert 'showRopePose()' in games
assert 'startActionAnimation(MainActivity.ActionAnim.JUMP,1350L)' not in games
print('Assets gameplay salon v0.7.1: PASS')
