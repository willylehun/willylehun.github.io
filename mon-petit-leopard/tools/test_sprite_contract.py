from pathlib import Path
import re, subprocess, tempfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/byw/monpetitleopard'
main=(JAVA/'MainActivity.java').read_text()\nassert (ROOT/'tools/prepare_v057_runtime_assets.py').exists()
assert 'float step=Math.min(speed,dist);' in main
assert 'enum TravelDirection {LEFT,RIGHT,UP,DOWN}' in main
assert 'MIN_SLEEP_MS=90000L' in main
assert 'MIN_FACE_SHARE=.70f' in main
assert 'showFaceMoodNow' in main
assert 'energy=100f' in main
for name in ['releaseWalkFrames','releaseFaceMoodFrames']:
    block=main.split('void '+name+'(){',1)[1].split('\n    }',1)[0]
    assert '.recycle(' not in block, name
assert 'manualUntil=0;' in main.split('void syncVisualStage(){',1)[1].split('PetStage petStage()',1)[0]
assert 'sx*=' not in main.split('void applyPose(int frame){',1)[1].split('void showAction(',1)[0]
for age in ['cub','teen','adult','old']:
    expected_walk=5 if age=='cub' else 4
    expected_faces=12 if age=='cub' else 11
    for f in (ROOT/f'app/src/main/res-{age}/drawable-nodpi').glob('*'):
        with Image.open(f) as im:
            im.load()
            if '_face_moods' in f.stem:
                assert f.name == f'leopard_{age}_face_moods.webp', f
                assert im.size == (320*expected_faces,320), (f,im.size)
                for i in range(expected_faces):
                    b=im.crop((320*i,0,320*(i+1),320)).getbbox()
                    assert b, (f,i)
                continue
            count=expected_walk if '_walk_' in f.stem else 1
            assert im.size==(640*count,640),(f,im.size)
            for i in range(count):
                b=im.crop((640*i,0,640*(i+1),640)).getbbox()
                assert b and b[1]>=30 and b[3]<=588,(f,b)
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
   check(p.hasFaceMoods() && p.faceFrameSize==320);
   int expectedFrames=age==MainActivity.PetStage.CUB?5:4;
   int expectedFaces=age==MainActivity.PetStage.CUB?12:11;
   check(p.faceFrameCount==expectedFaces);
   for(MainActivity.WalkMode m:MainActivity.WalkMode.values())check(p.frameCount(m)==expectedFrames);
   for(MainActivity.PetMood m:MainActivity.PetMood.values())check(p.ownsMood(p.mood(m)));
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
  check(SpriteMotion.direction(.11f,-.10f,300,1000)==SpriteMotion.UP);
  check(SpriteMotion.direction(.20f,-.05f,300,1000)==SpriteMotion.RIGHT);
  check(SpriteMotion.mirror(SpriteMotion.LEFT)==1f);
  check(SpriteMotion.mirror(SpriteMotion.RIGHT)==-1f);
  check(SpriteMotion.mirror(SpriteMotion.UP)==1f && SpriteMotion.mirror(SpriteMotion.DOWN)==1f);
  System.out.println("Java registry and direction tests: PASS");
 }
}""")
    subprocess.run(['javac','-d',str(p),*[str(f) for f in p.glob('*.java')]],check=True)
    subprocess.run(['java','-cp',str(p),'com.byw.monpetitleopard.ContractTest'],check=True)
print('Image decode, dimensions, anchors and source guards: PASS. No claim of visual age validation.')
