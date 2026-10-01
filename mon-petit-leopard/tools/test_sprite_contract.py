from pathlib import Path
import re, subprocess, tempfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/byw/monpetitleopard'
main=(JAVA/'MainActivity.java').read_text()

assert 'MIN_SLEEP_MS=90000L' in main
assert 'MOOD_DURATION_MS=90000L' in main
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
  System.out.println("Sprite registry v0.6.1: PASS");
 }
}""")
    subprocess.run(['javac','-d',str(p),*[str(f) for f in p.glob('*.java')]],check=True)
    subprocess.run(['java','-cp',str(p),'com.byw.monpetitleopard.ContractTest'],check=True)

print('Sprite contract v0.6.1: PASS')
