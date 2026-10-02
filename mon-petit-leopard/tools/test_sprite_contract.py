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
        f'leopard_{age}_rope_play.png':(1280,256),
        f'leopard_{age}_groom_foam.webp':(256,256),
        f'leopard_{age}_soap.webp':(256,256),
        f'leopard_{age}_comb.webp':(256,256),
        f'leopard_{age}_towel.webp':(256,256),
        f'leopard_{age}_scratcher_play.webp':(512,256),
    }
    if age=='cub':
        expected[f'leopard_{age}_bottle.webp']=(256,256)
    assert {p.name for p in folder.glob('leopard_*')}==set(expected)
    for name,size in expected.items():
        with Image.open(folder/name) as im:
            im.load()
            assert im.size==size,(name,im.size,size)

with tempfile.TemporaryDirectory() as temp:
    p=Path(temp)
    chars=(JAVA/'CharacterSprites.java').read_text()
    games_java=(JAVA/'GameSprites.java').read_text()
    care_java=(JAVA/'CareSprites.java').read_text()
    garden_java=(JAVA/'GardenSprites.java').read_text()
    (p/'CharacterSprites.java').write_text(chars)
    (p/'GameSprites.java').write_text(games_java)
    (p/'CareSprites.java').write_text(care_java)
    (p/'GardenSprites.java').write_text(garden_java)
    (p/'SpriteMotion.java').write_text((JAVA/'SpriteMotion.java').read_text())
    names=sorted(set(re.findall(r'R.drawable.(leopard_\w+)',chars+games_java+care_java+garden_java)))
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
   check(g.expectedWidth(g.ropePlay)==1280);
   for(int id:g.allResources())check(id!=0&&seen.add(id));

   CareSprites.Pack care=CareSprites.forStage(age);
   check(care.stage==age);
   check(care.expectedWidth(care.groomFoam)==256);
   check(care.expectedWidth(care.soap)==256);
   check(care.expectedWidth(care.comb)==256);
   check(care.expectedWidth(care.towel)==256);
   for(int id:care.allResources())check(id!=0&&seen.add(id));
   if(age==MainActivity.PetStage.CUB){
    int bottle=CareSprites.bottle(age);
    check(bottle!=0&&seen.add(bottle));
   }else{
    check(CareSprites.bottle(age)==0);
   }

   GardenSprites.Pack garden=GardenSprites.forStage(age);
   check(garden.stage==age);
   check(garden.expectedWidth(garden.scratcherPlay)==512);
   for(int id:garden.allResources())check(id!=0&&seen.add(id));
  }
  check(SpriteMotion.direction(-.10f,.25f,1f,1f)==SpriteMotion.LEFT);
  check(SpriteMotion.direction(.10f,.25f,1f,1f)==SpriteMotion.RIGHT);
  check(SpriteMotion.direction(-.04f,-.25f,1f,1f)==SpriteMotion.UP);
  check(SpriteMotion.direction(.04f,.25f,1f,1f)==SpriteMotion.DOWN);
  check(SpriteMotion.direction(0f,-.25f,1f,1f)==SpriteMotion.UP);
  check(SpriteMotion.direction(0f,.25f,1f,1f)==SpriteMotion.DOWN);
  System.out.println("Sprite registry v0.8.0: PASS");
 }
}""")
    subprocess.run(['javac','-d',str(p),*[str(f) for f in p.glob('*.java')]],check=True)
    subprocess.run(['java','-cp',str(p),'com.byw.monpetitleopard.ContractTest'],check=True)

print('Sprite contract v0.8.0: PASS')


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
    'rope_play':5,
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
                if key.startswith('fetch_'):
                    assert .80<=ratio<=1.20,(age,key,i,area,target,ratio,b)
                elif key=='rope_play':
                    assert .50<=ratio<=1.60,(age,key,i,area,target,ratio,b)
                else:
                    assert .95<=ratio<=1.05,(age,key,i,area,target,ratio,b)
                full=frame.getchannel('A').getbbox()
                assert full is not None
                assert full[0]>=16 and full[1]>=16 and full[2]<=240 and full[3]<=240,(age,key,i,full)

print("Normalisation de masse visuelle v0.7.1: PASS")

# Soins v0.8.0 : même canevas et masse visuelle propre à chaque tranche d'âge.
for age in ['cub','teen','adult','old']:
    folder=ROOT/f'app/src/main/res-{age}/drawable-nodpi'
    idle_areas=[]
    for key in ['idle_down','idle_left','idle_right','idle_up']:
        with Image.open(folder/f'leopard_{age}_{key}.png') as im:
            area,b=largest_component(im.convert('RGBA'))
            assert area>0 and b is not None
            idle_areas.append(area)
    target=float(np.median(idle_areas))

    for key in ['groom_foam','soap','comb','towel']:
        with Image.open(folder/f'leopard_{age}_{key}.webp') as im:
            frame=im.convert('RGBA')
            assert frame.size==(256,256),(age,key,frame.size)
            area,b=largest_component(frame)
            assert area>0 and b is not None,(age,key,'vide')
            ratio=area/target
            assert .95<=ratio<=1.10,(age,key,area,target,ratio,b)
            full=frame.getchannel('A').getbbox()
            assert full is not None
            assert full[0]>=16 and full[1]>=16 and full[2]<=240 and full[3]<=240,(age,key,full)

with Image.open(ROOT/'app/src/main/res-cub/drawable-nodpi/leopard_cub_bottle.webp') as im:
    frame=im.convert('RGBA')
    assert frame.size==(256,256),frame.size
    cub_folder=ROOT/'app/src/main/res-cub/drawable-nodpi'
    cub_idle=[]
    for key in ['idle_down','idle_left','idle_right','idle_up']:
        with Image.open(cub_folder/f'leopard_cub_{key}.png') as idle:
            area,_=largest_component(idle.convert('RGBA'))
            cub_idle.append(area)
    target=float(np.median(cub_idle))
    area,b=largest_component(frame)
    ratio=area/target
    assert .95<=ratio<=1.10,('cub','bottle',area,target,ratio,b)
    full=frame.getchannel('A').getbbox()
    assert full is not None
    assert full[0]>=16 and full[1]>=16 and full[2]<=240 and full[3]<=240,('cub','bottle',full)

print("Assets biberon et soins v0.8.0: PASS")


toy_visuals={'tennis':(96,96),'yarn':(96,96),'mouse':(96,96),'plush':(96,96),'rope':(96,64)}
for toy,size in toy_visuals.items():
    p=ROOT/'app/src/main/res/drawable-nodpi'/f'toy_{toy}_art.png'
    with Image.open(p) as im:
        rgba=im.convert('RGBA')
        assert rgba.size==size,(toy,rgba.size,size)
        alpha=np.array(rgba.getchannel('A'))
        assert alpha.min()==0 and alpha.max()==255,(toy,'alpha')

for age in ['cub','teen','adult','old']:
    p=ROOT/'source-assets/v072-rope'/f'leopard_{age}_rope_play_source.png'
    with Image.open(p) as strip:
        strip=strip.convert('RGBA')
        assert strip.size==(640,128),(age,strip.size)
        for i in range(5):
            assert strip.crop((i*128,0,(i+1)*128,128)).getchannel('A').getbbox() is not None,(age,i,'frame corde vide')

print("Visuels PNG et sources corde v0.8.0: PASS")

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

for toy in ['"tennis"','"yarn"','"mouse"','"plush"']:
    assert toy in objects
assert '"fishToy"' not in objects
assert '"tunnel"' not in objects
assert '"rope","Corde","🪢","salon","Jouets","rope"' in objects
assert '"bed","Repos","🛏️","salon","Repos","rest"' in objects
assert '"Dressage"' not in objects
assert '"clicker"' not in objects
assert '"whistle"' not in objects
assert 'String[] groups={"Jouets","Repos"};' in objects
assert 'games.startFetch(i)' in objects
assert 'games.startRope(i)' in objects
assert 'beginAutoSleep();' in objects
assert 'if(i.id.equals("bottle"))animation="bottle";' in objects
assert 'else if(i.id.equals("groom"))animation="groom_foam";' in objects
assert 'else if(i.id.equals("soap"))animation="soap";' in objects
assert 'else if(i.id.equals("comb"))animation="comb";' in objects
assert 'else if(i.id.equals("towel"))animation="towel";' in objects

assert 'class LivingRoomGames' in games
assert 'THROW_READY' in games and 'RUN_TO_TOY' in games and 'RETURNING' in games
assert 'dy<-a.dp(48)' in games
assert 'ValueAnimator.ofFloat(0f,1f)' in games
assert 'ImageView toyView' in games
assert 'chooseLandingNode' in games
assert 'movePetToNode' in games
assert 'fastRun()' in games
assert 'ROPE_HOLD' in games
assert 'ValueAnimator.ofFloat(0f,1f)' in games
assert 'u*u*sy+2f*u*t*cy+t*t*target[1]' in games
assert 'toyMenuDrawable' in objects
assert 'toyDrawable' in games
assert 'positionToyNearPet();' not in games
for res in ['toy_tennis_art','toy_yarn_art','toy_mouse_art','toy_plush_art','toy_rope_art']:
    assert ('R.drawable.'+res) in (games+objects),res

assert '📣 Appeler' in main
assert 'void callLeopard()' in main
assert 'games.onPetArrived(now)' in main
assert 'games.fastRun()' in main
assert '{.30f,.74f}' not in main
assert '{.30f,.86f}' in main and '{.50f,.95f}' in main
assert 'speed*=1.85f' in main
assert 'CareSprites.forStage(petStage()).action(animation)' in main
assert 'CareSprites.bottle(petStage())' in main
assert 'startSpecialPose' in main

print('Jeux salon v0.8.0: PASS')


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
print('Assets gameplay salon v0.8.0: PASS')

prepare_care=(ROOT/'tools/prepare_v075_care_assets.py').read_text()
care_registry=(JAVA/'CareSprites.java').read_text()
assert 'v075-care-bundle' in prepare_care
assert 'leopard_{age}_{action}.webp' in prepare_care
assert 'leopard_cub_bottle.webp' in prepare_care
assert 'static Pack forStage' in care_registry
assert 'static int bottle' in care_registry
print('Assets biberon et soins v0.8.0: PASS')


# Promenade v0.8.0 : assets, durée réelle, départ/retour maison et branchement Jardin.
promenade_java=(JAVA/'PromenadeActivity.java').read_text()
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
prepare_walk=(ROOT/'tools/prepare_v076_promenade_assets.py').read_text()
objects=(JAVA/'ObjectSystem.java').read_text()

with Image.open(ROOT/'app/src/main/res/drawable-nodpi/promenade_map.webp') as im:
    im.load()
    assert im.size==(1448,1086),im.size

with Image.open(ROOT/'app/src/main/res/drawable-nodpi/promenade_token.webp') as im:
    rgba=im.convert('RGBA')
    assert rgba.size==(128,128),rgba.size
    alpha=np.array(rgba.getchannel('A'))
    assert alpha.min()==0 and alpha.max()==255,alpha.getextrema() if hasattr(alpha,'getextrema') else (alpha.min(),alpha.max())

assert 'DURATION_MS=3L*60L*1000L' in promenade_java
assert '{160,905}' in promenade_java
assert 'route[route.length-1]' in promenade_java
assert 'drawW*(58f/MAP_W)' in promenade_java
assert 'promenadeStart' in promenade_java and 'promenadeActive' in promenade_java
assert 'a.startPromenade(i);' in objects
assert 'void startPromenade(ObjectSystem.Item item)' in main
assert '.PromenadeActivity' in manifest
assert 'v076-promenade-bundle' in prepare_walk
print('Promenade v0.8.0: PASS')


# Jardin v0.8.0 : griffoir, animation 2 frames, jouets réutilisés, repos et simplification.
garden_games=(JAVA/'GardenGames.java').read_text()
garden_registry=(JAVA/'GardenSprites.java').read_text()
prepare_garden=(ROOT/'tools/prepare_v078_garden_assets.py').read_text()
objects=(JAVA/'ObjectSystem.java').read_text()
living=(JAVA/'LivingRoomGames.java').read_text()

with Image.open(ROOT/'app/src/main/res/drawable-nodpi/garden_scratcher.webp') as im:
    rgba=im.convert('RGBA')
    assert rgba.size==(256,256),rgba.size
    b=rgba.getchannel('A').getbbox()
    assert b is not None and b[0]>=10 and b[1]>=10 and b[2]<=246 and b[3]<=246,b

for age in ['cub','teen','adult','old']:
    p=ROOT/f'app/src/main/res-{age}/drawable-nodpi'/f'leopard_{age}_scratcher_play.webp'
    with Image.open(p) as strip:
        strip=strip.convert('RGBA')
        assert strip.size==(512,256),(age,strip.size)
        for i in range(2):
            b=strip.crop((i*256,0,(i+1)*256,256)).getchannel('A').getbbox()
            assert b is not None,(age,i)
            assert b[0]>=10 and b[1]>=10 and b[2]<=246 and b[3]<=246,(age,i,b)

assert 'static final int NONE=0,APPROACH=1,PLAYING=2' in garden_games
assert 'playUntil=now+4200L' in garden_games
assert 'GardenSprites.forStage(a.petStage()).scratcherPlay' in garden_games
assert 'garden_scratcher' in garden_games
assert 'state!=PLAYING?View.VISIBLE:View.GONE' not in garden_games
assert 'scratcherView.setVisibility("jardin".equals(a.room)?View.VISIBLE:View.GONE);' in garden_games
assert 'scratcherView.setVisibility(View.VISIBLE);' in garden_games
assert 'Promenade démarrée pour 3 minutes.' in main
assert 'a.gardenGames.startScratcher(i)' in objects
assert 'String[] groups={"Jouets","Jardin"};' in objects
assert '"tennis","Balle de tennis","🎾","jardin","Jouets","toy"' in objects
assert '"plush","Peluche","🧸","jardin","Jouets","toy"' in objects
assert '"rope","Corde","🪢","jardin","Jouets","rope"' in objects
assert '"scratch","Griffoir","🐾","jardin","Jardin","scratcher"' in objects
assert '"sun","Repos au soleil","☀️","jardin","Jardin","rest"' in objects
assert 'add("contest"' not in objects
assert 'add("feather"' not in objects
assert 'add("hoop"' not in objects
assert 'salon ou le jardin' in living
assert 'v078-garden-scratcher' in prepare_garden
assert 'GardenSprites.Pack garden=GardenSprites.forStage(age);' in main
print('Jardin griffoir et jouets v0.8.0: PASS')


# Profils animaux v0.8.0 : onboarding, 6 sauvegardes et sélection à chaque lancement.
profiles=(JAVA/'PetProfileStore.java').read_text()
chooser=(JAVA/'PetChooserActivity.java').read_text()
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
promenade=(JAVA/'PromenadeActivity.java').read_text()
main=(JAVA/'MainActivity.java').read_text()

assert 'MAX_PROFILES=6' in profiles
assert 'petPrefsName(int slot){return "pet_"+slot;}' in profiles
assert 'ensureMigrated(Context context)' in profiles
assert 'getSharedPreferences("pet",Context.MODE_PRIVATE)' in profiles
assert 'copyAll(legacy,target)' in profiles
assert 'createLeopard' in profiles
assert 'R.drawable.leopard_cub_idle_down' in profiles
assert 'R.drawable.leopard_teen_idle_down' in profiles
assert 'R.drawable.leopard_adult_idle_down' in profiles
assert 'R.drawable.leopard_old_idle_down' in profiles

assert 'Choisis ton animal' in chooser
assert 'Léopard' in chooser
assert 'Choisis le sexe de l’animal' in chooser
assert '♂  Mâle' in chooser and '♀  Femelle' in chooser
assert 'Quel est son nom ?' in chooser
assert 'De qui veux-tu t’occuper aujourd’hui ?' in chooser
assert 'for(int slot=0;slot<PetProfileStore.MAX_PROFILES;slot++)' in chooser
assert 'Nouvel animal' in chooser

assert 'android:name=".PetChooserActivity"' in manifest
assert '<action android:name="android.intent.action.MAIN" />' in manifest
assert 'android:name=".MainActivity"' in manifest
assert 'android:exported="false"' in manifest

assert 'profileSlot=getIntent().getIntExtra(PetProfileStore.EXTRA_SLOT,-1);' in main
assert 'getSharedPreferences(PetProfileStore.petPrefsName(profileSlot),MODE_PRIVATE)' in main
assert '🐾 Changer d’animal' in main
assert 'void openPetChooser()' in main
assert 'intent.putExtra(PetProfileStore.EXTRA_SLOT,profileSlot);' in main
assert 'resumeNeedsChooser' in main and 'internalTransition' in main
assert 'if(!internalTransition)resumeNeedsChooser=true;' in main

assert 'profileSlot=getIntent().getIntExtra(PetProfileStore.EXTRA_SLOT,-1);' in promenade
assert 'getSharedPreferences(PetProfileStore.petPrefsName(profileSlot),MODE_PRIVATE)' in promenade
assert 'resumeNeedsChooser' in promenade and 'internalReturn' in promenade
print('Profils animaux v0.8.0: PASS')


# Besoins, goûts, gamelle et lassitude v0.8.0.
behavior=(JAVA/'PetBehavior.java').read_text()
objects=(JAVA/'ObjectSystem.java').read_text()
games=(JAVA/'LivingRoomGames.java').read_text()
garden=(JAVA/'GardenGames.java').read_text()
water_system=(JAVA/'KitchenWaterSystem.java').read_text()
main=(JAVA/'MainActivity.java').read_text()

assert 'enum Preference {LOVE,NEUTRAL,DISLIKE}' in behavior
assert 'registerRepeat' in behavior
assert 'if(family==null||family.isEmpty()||"sleep".equals(family))return 1f;' in behavior
assert 'if(count==2)return .75f;' in behavior
assert 'if(count==3)return .40f;' in behavior
assert 'if(count==4)return .15f;' in behavior
assert 'return 0f;' in behavior
assert 'foodPreference(SharedPreferences sp,String id,boolean cub)' in behavior
assert 'cub&&("milk".equals(id)||"junior".equals(id))' in behavior
assert '"bottle".equals(id)' in behavior
assert 'toyPreference' in behavior

assert 'void performItemAction' in main
assert 'void applyToyRewards' in main
assert 'PetBehavior.Preference.DISLIKE' in main
assert 'queueFaceMood(1)' in main
assert 'h*=1.35f' in main and 'joy+=6f' in main
assert 'joy=-7f;' in main and 'joy=-6f*duration;' in main
assert 'c-=("snack".equals(item.kind)||"treat".equals(item.kind))?1.5f:3f;' in main
assert 'clean-=.16f*m;' in main
assert 'applyNeedDelta(0,0,0,8,0,0,0,callingEffectFactor)' in main
assert 'promenadeAway()' in main
assert 'item.clean-5f' in main
assert 'clean=clamp(clean-8);' in main
assert 'petView.setVisibility(View.INVISIBLE)' in main

assert 'waterBowl=sp.getFloat("waterBowl",0f)' in main
assert 'waterBowl=Math.max(0f,waterBowl-drink)' in main
assert 'KitchenWaterSystem kitchenWater' in main
assert 'a.waterBowl=100f;' in water_system
assert '"cuisine".equals(a.room)&&a.waterBowl>.05f' in water_system

assert 'if(a.kitchenWater!=null)a.kitchenWater.fill();' in objects
assert 'a.promenadeAway()' in objects
assert 'a.performItemAction(i,family,animation);' in objects
assert 'a.applyToyRewards(activeItem,activeRepeatFactor,factor);' in games
assert 'a.beginRepeatedAction("play:"+item.id)' in games
assert 'a.applyToyRewards(activeItem,activeRepeatFactor,1f);' in garden
assert 'a.beginRepeatedAction("play:scratch")' in garden
print('Besoins et lassitude v0.8.0: PASS')
