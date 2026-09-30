from pathlib import Path
import re, subprocess, tempfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/byw/monpetitleopard'
main=(JAVA/'MainActivity.java').read_text()
assert 'float step=Math.min(speed,dist);' in main
assert 'enum TravelDirection {LEFT,RIGHT,UP,DOWN}' in main
for name in ['releaseWalkFrames','releaseCubFaceMoodFrames']:
    block=main.split('void '+name+'(){',1)[1].split('\n    }',1)[0]
    assert '.recycle(' not in block, name
assert 'manualUntil=0;' in main.split('void syncVisualStage(){',1)[1].split('PetStage petStage()',1)[0]
assert 'sx*=' not in main.split('void applyPose(int frame){',1)[1].split('void showAction(',1)[0]
for age in ['cub','teen','adult','old']:
    for f in (ROOT/f'app/src/main/res-{age}/drawable-nodpi').glob('*'):
        with Image.open(f) as im:
            im.load()
            if f.name == 'leopard_cub_face_moods.webp':
                assert age == 'cub'
                assert im.size == (960,80), f
                for i in range(12):
                    b=im.crop((80*i,0,80*(i+1),80)).getbbox()
                    assert b, (f,i)
                continue
            count=4 if '_walk_' in f.stem else 1
            assert im.size==(640*count,640),f
            for i in range(count):
                b=im.crop((640*i,0,640*(i+1),640)).getbbox()
                assert b and b[1]>=24 and b[3]==588,(f,b)
with tempfile.TemporaryDirectory() as temp:
    p=Path(temp)
    for name in ['CharacterSprites.java','SpriteMotion.java']:(p/name).write_text((JAVA/name).read_text())
    names=sorted(set(re.findall(r'R.drawable.(leopard_\w+)',(JAVA/'CharacterSprites.java').read_text())))
    (p/'R.java').write_text('package com.byw.monpetitleopard; final class R { static class drawable {'+''.join('static final int '+n+'='+str(i+1)+';' for i,n in enumerate(names))+'}}')
    (p/'MainActivity.java').write_text('package com.byw.monpetitleopard; class MainActivity { enum PetStage {CUB,TEEN,ADULT,OLD} enum PetMood {IDLE,HAPPY,TIRED,SLEEP} enum WalkMode {SIDE,FRONT,BACK} }')
    (p/'ContractTest.java').write_text("""package com.byw.monpetitleopard;
class ContractTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  java.util.HashSet<Integer> seen=new java.util.HashSet<>();
  for(MainActivity.PetStage age:MainActivity.PetStage.values()){
   CharacterSprites.Pack p=CharacterSprites.forStage(age);check(p.stage==age);
   for(int id:p.allResources())check(id!=0 && seen.add(id));
   for(MainActivity.WalkMode m:MainActivity.WalkMode.values())check(p.frameCount(m)==4);
   for(MainActivity.PetMood m:MainActivity.PetMood.values())check(p.ownsMood(p.mood(m)));
   if(age==MainActivity.PetStage.CUB){
    check(p.hasFaceMoods()); check(p.faceFrameSize==80); check(p.faceFrameCount==12);
   } else check(!p.hasFaceMoods());
  }
  check(CharacterSprites.FACE_ATLAS_REVIEWED);
  check(SpriteMotion.direction(0,-1,600,450)==SpriteMotion.UP);
  check(SpriteMotion.direction(0,1,600,450)==SpriteMotion.DOWN);
  check(SpriteMotion.direction(-1,0,600,450)==SpriteMotion.LEFT);
  check(SpriteMotion.direction(1,0,600,450)==SpriteMotion.RIGHT);
  // Les coordonnées de déplacement sont normalisées (0..1) : la direction
  // se décide dans ce même espace, sans biais lié au ratio largeur/hauteur de la pièce.
  check(SpriteMotion.direction(.1f,-.2f,600,450)==SpriteMotion.UP);
  check(SpriteMotion.direction(.2f,-.1f,600,450)==SpriteMotion.RIGHT);
  check(SpriteMotion.direction(.10f,-.11f,1000,300)==SpriteMotion.UP);
  check(SpriteMotion.direction(.11f,-.10f,300,1000)==SpriteMotion.RIGHT);
  check(SpriteMotion.mirror(SpriteMotion.LEFT)==1f);
  check(SpriteMotion.mirror(SpriteMotion.RIGHT)==-1f);
  check(SpriteMotion.mirror(SpriteMotion.UP)==1f && SpriteMotion.mirror(SpriteMotion.DOWN)==1f);
  System.out.println("Java registry and direction tests: PASS");
 }
}""")
    subprocess.run(['javac','-d',str(p),*[str(f) for f in p.glob('*.java')]],check=True)
    subprocess.run(['java','-cp',str(p),'com.byw.monpetitleopard.ContractTest'],check=True)
print('Image decode, dimensions, anchors and source guards: PASS. No claim of visual age validation.')
