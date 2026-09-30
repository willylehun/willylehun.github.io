#!/usr/bin/env python3
"""One-shot v0.5.3 -> v0.5.4 migration. No inter-age reassignment or saved-game changes."""
from pathlib import Path
import hashlib, json, re
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/byw/monpetitleopard'
p=JAVA/'MainActivity.java'
s=p.read_text()
assert "versionName '0.5.3'" in (ROOT/'app/build.gradle').read_text(), 'Expected audited v0.5.3'
def method(signature,replacement):
    global s
    start=s.index('    '+signature)
    end=s.index('\n    }',start)+6
    s=s[:start]+replacement.rstrip()+s[end:]
method('void releaseWalkFrames(){', '''    void releaseWalkFrames(){
        // ImageView / RenderThread may still reference these bitmaps: let GC reclaim them.
        currentWalkFrames=null;
        currentWalkStrip=null;
        currentWalkStripRes=0;
        loadedWalkStage=null;
        loadedWalkMode=null;
    }''')
method('void releaseCubFaceMoodFrames(){', '''    void releaseCubFaceMoodFrames(){
        cubFaceMoodFrames=null;
        cubFaceMoodStrip=null;
    }''')
s=s.replace('        PetStage previous=visualStage;\n        visualStage=now;', '''        PetStage previous=visualStage;
        visualStage=now;
        manualUntil=0;
        manualFrame=0;
        if(petView!=null){
            petView.setImageDrawable(null);
            petView.setRotation(0f);
            petView.setScaleX(1f);
            petView.setScaleY(1f);
        }''')
s=s.replace('    void ensurePetImage(){\n','    void ensurePetImage(){\n        syncVisualStage();\n')
s=s.replace('else if(!walking)res=emotionDrawable();', '''else if(!walking)res=System.currentTimeMillis()<manualUntil
                    ?poseDrawableForFrame(manualFrame):emotionDrawable();''')
s=s.replace('        petView.setVisibility(View.VISIBLE);\n        petView.bringToFront();', '''        petView.setVisibility(displayedPetStage==petStage()
                && petView.getDrawable()!=null?View.VISIBLE:View.INVISIBLE);
        petView.bringToFront();''')
method('void applyPose(int frame){', '''    int poseDrawableForFrame(int frame){
        if(sleeping || (frame==9 && stage()==Stage.ENDED))return sleepDrawable();
        if(frame==9 || frame==3)return tiredDrawable();
        if(frame==11)return happyDrawable();
        return emotionDrawable();
    }

    void applyPose(int frame){
        syncVisualStage();
        walking=false;
        walkMode=WalkMode.SIDE;
        int res=poseDrawableForFrame(frame);
        if(!setPetDrawableSafely(petStage(),res))return;
        currentPetRes=res;
        petView.setRotation(0f);
        updatePetPosition();
    }''')
s=s.replace('petView.setPivotY(size);','petView.setPivotY(size*(588f/640f));')
s=s.replace('float top=feet-petView.getHeight();','float top=feet-petView.getHeight()*(588f/640f);')
start=s.index('        // Mode figé pendant tout le segment')
end=s.index('\n        faceMoodUntil=0;',start)
s=s[:start]+'''        float[] rect=imageRect();
        int direction=SpriteMotion.direction(dx,dy,rect[2],rect[3]);
        travelDirection=TravelDirection.values()[direction];
        walkMode=direction==SpriteMotion.UP?WalkMode.BACK:
                direction==SpriteMotion.DOWN?WalkMode.FRONT:WalkMode.SIDE;
        walkDir=direction==SpriteMotion.LEFT?-1:direction==SpriteMotion.RIGHT?1:0;
'''+s[end:]
s=s.replace('            sign=travelDirection==TravelDirection.RIGHT?-1f:1f;',
            '            sign=SpriteMotion.mirror(travelDirection.ordinal());')
s=s.replace('            petNX+=dx/dist*speed;\n            petNY+=dy/dist*speed;',
            '            float step=Math.min(speed,dist);\n            petNX+=dx/dist*step;\n            petNY+=dy/dist*step;')
s=s.replace('        if(h<=0 || w<h || w%h!=0){','        if(h!=640 || w!=2560){')
s=s.replace('        if(sourceCount<count){','        if(sourceCount!=count){')
s=s.replace('    void maybeShowFaceMood(long now){\n', '''    void maybeShowFaceMood(long now){
        if(!CharacterSprites.FACE_ATLAS_REVIEWED)return;
''')
s=s.replace('    void showCubFaceMoodNow(int index,int duration){\n', '''    void showCubFaceMoodNow(int index,int duration){
        if(!CharacterSprites.FACE_ATLAS_REVIEWED){
            showAction(strongEmotion()?3:11,duration);
            return;
        }
''')
method('void showTopMenu(){', '''    void showTopMenu(){
        String[] entries={"📜 Historique","🔎 Vérifier les quatre packs"};
        new AlertDialog.Builder(this).setTitle("Menu").setItems(entries,(d,w)->{
            if(w==0)showHistory(); else showSpritePackPicker();
        }).show();
    }

    void showSpritePackPicker(){
        String[] names={"Léopardeau / CUB","Ado / TEEN","Adulte / ADULT","Vieux / OLD"};
        new AlertDialog.Builder(this).setTitle("Packs séparés — diagnostic")
            .setItems(names,(d,w)->showSpritePack(PetStage.values()[w],0,0)).show();
    }

    void showSpritePack(PetStage age,int item,int frame){
        CharacterSprites.Pack pack=CharacterSprites.forStage(age);
        int[] ids={pack.idle,pack.happy,pack.tired,pack.sleep,
            pack.walkSide,pack.walkFront,pack.walkBack};
        String[] names={"idle","happy","tired","sleep","walk_side","walk_front","walk_back"};
        int k=Math.floorMod(item,ids.length);
        int f=Math.floorMod(frame,4);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView note=text(12,false);
        note.setText("Version 0.5.4 • "+pack.zone+"\\n"
            +"Contrôle technique ≠ validation du dessin ou de son âge.\\n"
            +names[k]+(k>=4?" • frame "+(f+1)+"/4":""));
        note.setPadding(dp(14),dp(8),dp(14),dp(8));
        box.addView(note);
        ImageView image=new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap b=BitmapFactory.decodeResource(getResources(),ids[k]);
        if(b!=null && k>=4 && b.getWidth()==2560 && b.getHeight()==640)
            b=Bitmap.createBitmap(b,f*640,0,640,640);
        if(b!=null)image.setImageBitmap(b);
        box.addView(image,new LinearLayout.LayoutParams(-1,dp(230)));
        if(k>=4){
            Button next=button("Frame suivante");
            box.addView(next);
            final AlertDialog[] dialog=new AlertDialog[1];
            next.setOnClickListener(v->{dialog[0].dismiss();showSpritePack(age,k,f+1);});
            dialog[0]=new AlertDialog.Builder(this).setTitle("Pack "+age)
                .setView(box).setNegativeButton("Précédent",(d,w)->showSpritePack(age,k-1,0))
                .setPositiveButton("Suivant",(d,w)->showSpritePack(age,k+1,0))
                .setNeutralButton("Fermer",null).create();
            dialog[0].show();
        }else{
            new AlertDialog.Builder(this).setTitle("Pack "+age).setView(box)
                .setNegativeButton("Précédent",(d,w)->showSpritePack(age,k-1,0))
                .setPositiveButton("Suivant",(d,w)->showSpritePack(age,k+1,0))
                .setNeutralButton("Fermer",null).show();
        }
    }''')
method('void validateCharacterAssets(){', '''    void validateCharacterAssets(){
        invalidCharacterAssets.clear();
        assetErrorShown=false;
        HashMap<Integer,PetStage> owners=new HashMap<>();
        for(PetStage age:PetStage.values()){
            CharacterSprites.Pack pack=CharacterSprites.forStage(age);
            for(int res:pack.allResources()){
                if(res==0)continue;
                PetStage other=owners.put(res,age);
                if(other!=null && other!=age){
                    markCharacterAssetInvalid(res,age+" PACK","partage inter-âge interdit");
                    continue;
                }
                try{
                    String name=getResources().getResourceEntryName(res);
                    if(!name.startsWith("leopard_"+age.name().toLowerCase(Locale.ROOT)+"_"))
                        throw new IllegalArgumentException("mauvais préfixe : "+name);
                    BitmapFactory.Options opts=new BitmapFactory.Options();
                    opts.inJustDecodeBounds=true;
                    opts.inScaled=false;
                    BitmapFactory.decodeResource(getResources(),res,opts);
                    int width=pack.ownsMood(res)?640:res==pack.faceMoods?pack.faceFrameSize*pack.faceFrameCount:2560;
                    int height=res==pack.faceMoods?pack.faceFrameSize:640;
                    if(opts.outWidth!=width || opts.outHeight!=height)
                        throw new IllegalArgumentException(opts.outWidth+"x"+opts.outHeight+" au lieu de "+width+"x"+height);
                }catch(RuntimeException error){
                    markCharacterAssetInvalid(res,age+" PACK",error.getMessage());
                }
            }
        }
        if(!invalidCharacterAssets.isEmpty())showAssetErrorOnce();
    }''')
p.write_text(s)
pack=JAVA/'CharacterSprites.java'
pack.write_text(pack.read_text().replace('R.drawable.leopard_cub_face_moods,80,12,','0,0,0,').replace('final class CharacterSprites {', '''final class CharacterSprites {
    // 7x7 user reference; previous 8x8 annotations are invalid.
    // Truncated portrait atlas archived, never borrowed by another age.
    static final boolean FACE_ATLAS_REVIEWED=false;'''))
(JAVA/'SpriteMotion.java').write_text('''package com.byw.monpetitleopard;
final class SpriteMotion {
    static final int LEFT=0,RIGHT=1,UP=2,DOWN=3;
    static int direction(float dx,float dy,float width,float height){
        float x=dx*Math.max(1f,width), y=dy*Math.max(1f,height);
        if(Math.abs(y)>Math.abs(x))return y<0?UP:DOWN;
        return x<0?LEFT:RIGHT;
    }
    static float mirror(int direction){return direction==RIGHT?-1f:1f;}
    private SpriteMotion(){}
}
''')
manifest={'version':'0.5.4','visual_review':'PENDING; dimensions do not establish age or repair clipped ears','packs':{}}
for age in ['cub','teen','adult','old']:
    folder=ROOT/f'app/src/main/res-{age}/drawable-nodpi'
    files={}
    for path in sorted(folder.glob('leopard_*')):
        assert path.name.startswith('leopard_'+age+'_')
        raw=path.read_bytes()
        if '_face_moods' in path.stem:
            assert len(raw)==14955 and int.from_bytes(raw[4:8],'little')+8==30542
            target=ROOT/'archive/invalid-assets'/path.name
            target.parent.mkdir(parents=True,exist_ok=True)
            path.rename(target)
            continue
        image=Image.open(path).convert('RGBA')
        source_size=list(image.size)
        count=4 if '_walk_' in path.stem else 1
        assert image.height==640 and image.width>=640*count
        frames=[]
        for i in range(count):
            frame=image.crop((i*640,0,(i+1)*640,640)); bbox=frame.getbbox()
            assert bbox is not None
            # Common ground line, no independent rescale and no inter-age source.
            dy=588-bbox[3]
            assert bbox[1]+dy>=24 and bbox[3]+dy<=616
            aligned=Image.new('RGBA',(640,640))
            aligned.alpha_composite(frame,(0,dy));frames.append(aligned)
        image=Image.new('RGBA',(640*count,640))
        for i,frame in enumerate(frames):image.alpha_composite(frame,(640*i,0))
        if path.suffix=='.webp':image.save(path,lossless=True,method=6)
        else:image.save(path,optimize=True)
        files[path.name]={'source_sha256':hashlib.sha256(raw).hexdigest(),
            'source_size':source_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
            'size':list(image.size),'frame_count':len(frames),
            'frame_bounds':[list(f.getbbox()) for f in frames]}
    manifest['packs'][age]=files
(ROOT/'sprite-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
v=ROOT/'tools/validate_sprite_packs.py'
t=v.read_text().replace('(3200, 640)','(2560, 640)').replace('            "leopard_cub_face_moods.webp": (960, 80),\n','')
t=t.replace('    print("OK: aucun mélange inter-âge détecté")','    print("OK technique: dimensions et rangement. Validation graphique des âges: EN ATTENTE.")')
v.write_text(t)
legacy=ROOT/'tools/prepare_sprite_assets.py'
if legacy.exists():
    target=ROOT/'archive/legacy-tools/prepare_sprite_assets_v047.py'
    target.parent.mkdir(parents=True,exist_ok=True)
    legacy.rename(target)
(ROOT/'VISUAL_REVIEW.md').write_text('''# Revue visuelle à terminer — ne pas déclarer les dessins validés

La planche utilisateur 1000006717.png est une grille de **7 colonnes × 7 lignes**.
L'ancienne annotation cub_annotated.png en 8×8 est erronée et ne doit pas servir au découpage.
Numérotation correcte, ligne par ligne : humeurs 1–11 +13, idle 13, tired 15,
sleep 42, happy 48, walk_front 23–26, walk_back 27–30, walk_side 31–34.
Les références originales ado/adulte/vieux ne sont pas présentes dans cette reprise.
La ressemblance entre la planche CUB fournie et certains fichiers TEEN doit être résolue
à partir des planches originales : ne pas permuter les deux packs par supposition.

Certaines oreilles sont déjà tronquées dans les images happy et side existantes.
Une marge de transparence ne reconstitue pas ces pixels. Les sprites gardent leur âge
actuellement attribué en attendant validation graphique. Aucun échange entre packs.
L'ancien atlas de portraits CUB de 80px est tronqué : 14955 octets présents pour
30542 octets annoncés par RIFF. Échec confirmé par Pillow et ImageMagick.
Il est archivé hors des ressources Android et désactivé pour éviter qu'une extraction
non vérifiée remplace le corps du léopard. Les réactions utilisent provisoirement
les poses happy/tired du même pack.

v0.5.4 corrige la mécanique de rendu, pas ces dessins manquants ou ambigus.
''')
(ROOT/'tools/test_sprite_contract.py').write_text('''from pathlib import Path
import re, subprocess, tempfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/byw/monpetitleopard'
main=(JAVA/'MainActivity.java').read_text()
assert 'float step=Math.min(speed,dist);' in main
assert 'enum TravelDirection {LEFT,RIGHT,UP,DOWN}' in main
for name in ['releaseWalkFrames','releaseCubFaceMoodFrames']:
    block=main.split('void '+name+'(){',1)[1].split('\\n    }',1)[0]
    assert '.recycle(' not in block, name
assert 'manualUntil=0;' in main.split('void syncVisualStage(){',1)[1].split('PetStage petStage()',1)[0]
assert 'sx*=' not in main.split('void applyPose(int frame){',1)[1].split('void showAction(',1)[0]
for age in ['cub','teen','adult','old']:
    for f in (ROOT/f'app/src/main/res-{age}/drawable-nodpi').glob('*'):
        with Image.open(f) as im:
            im.load()
            count=4 if '_walk_' in f.stem else 1
            assert im.size==(640*count,640),f
            for i in range(count):
                b=im.crop((640*i,0,640*(i+1),640)).getbbox()
                assert b and b[1]>=24 and b[3]==588,(f,b)
with tempfile.TemporaryDirectory() as temp:
    p=Path(temp)
    for name in ['CharacterSprites.java','SpriteMotion.java']:(p/name).write_text((JAVA/name).read_text())
    names=sorted(set(re.findall(r'R.drawable.(leopard_\\w+)',(JAVA/'CharacterSprites.java').read_text())))
    (p/'R.java').write_text('package com.byw.monpetitleopard; final class R { static class drawable {'+''.join('static final int '+n+'='+str(i+1)+';' for i,n in enumerate(names))+'}}')
    (p/'MainActivity.java').write_text('package com.byw.monpetitleopard; class MainActivity { enum PetStage {CUB,TEEN,ADULT,OLD} enum PetMood {IDLE,HAPPY,TIRED,SLEEP} enum WalkMode {SIDE,FRONT,BACK} }')
    (p/'ContractTest.java').write_text(\"\"\"package com.byw.monpetitleopard;
class ContractTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  java.util.HashSet<Integer> seen=new java.util.HashSet<>();
  for(MainActivity.PetStage age:MainActivity.PetStage.values()){
   CharacterSprites.Pack p=CharacterSprites.forStage(age);check(p.stage==age);
   for(int id:p.allResources())check(id!=0 && seen.add(id));
   for(MainActivity.WalkMode m:MainActivity.WalkMode.values())check(p.frameCount(m)==4);
   for(MainActivity.PetMood m:MainActivity.PetMood.values())check(p.ownsMood(p.mood(m)));
  }
  check(SpriteMotion.direction(0,-1,600,450)==SpriteMotion.UP);
  check(SpriteMotion.direction(0,1,600,450)==SpriteMotion.DOWN);
  check(SpriteMotion.direction(-1,0,600,450)==SpriteMotion.LEFT);
  check(SpriteMotion.direction(1,0,600,450)==SpriteMotion.RIGHT);
  check(SpriteMotion.direction(.1f,-.2f,600,450)==SpriteMotion.UP);
  check(SpriteMotion.direction(.2f,-.1f,600,450)==SpriteMotion.RIGHT);
  check(SpriteMotion.mirror(SpriteMotion.LEFT)==1f);
  check(SpriteMotion.mirror(SpriteMotion.RIGHT)==-1f);
  check(SpriteMotion.mirror(SpriteMotion.UP)==1f && SpriteMotion.mirror(SpriteMotion.DOWN)==1f);
  System.out.println(\"Java registry and direction tests: PASS\");
 }
}\"\"\")
    subprocess.run(['javac','-d',str(p),*[str(f) for f in p.glob('*.java')]],check=True)
    subprocess.run(['java','-cp',str(p),'com.byw.monpetitleopard.ContractTest'],check=True)
print('Image decode, dimensions, anchors and source guards: PASS. No claim of visual age validation.')
''')
b=ROOT/'app/build.gradle'
b.write_text(b.read_text().replace('versionCode 18','versionCode 19').replace('0.5.3','0.5.4'))
i=ROOT/'index.html';i.write_text(i.read_text().replace('0.5.3','0.5.4'))
d=ROOT/'SPRITE_PACKS.md'
d.write_text(d.read_text().replace("Le strip SIDE source peut conserver une 5e frame, mais elle n'est jamais utilisée",'Les strips SIDE, FRONT et BACK exportés contiennent chacun 4 frames')+'\n## Limites de validation\nVoir VISUAL_REVIEW.md : seuls les contrôles techniques sont validés. L’atlas de portraits est archivé car tronqué, non utilisé.\n')
w=ROOT.parent/'.github/workflows/mon-petit-leopard-android.yml'
if w.exists():
    text=w.read_text().replace('0.5.3','0.5.4')
    text=text.replace('      - name: Validate sprite packs', '''      - name: Install image test dependency
        run: python -m pip install Pillow==11.3.0

      - name: Sprite contract tests
        run: python mon-petit-leopard/tools/test_sprite_contract.py

      - name: Validate sprite packs''')
    text=re.sub(r'--notes ".*"', '--notes "v0.5.4 : correctifs techniques des sprites, caches, ancrage et directions ; quatre packs isolés. Validation graphique des âges et oreilles tronquées encore à terminer, voir VISUAL_REVIEW.md."',text)
    w.write_text(text)
print('Migration v0.5.4 applied; visual review still pending.')
