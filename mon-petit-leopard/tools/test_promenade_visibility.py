"""Exercise the production house-rendering methods during an ongoing promenade.

Only Android's view, bitmap and scheduler boundaries are replaced by recording
stubs. Visibility decisions, return handling, pose methods, animation dispatch,
sprite ownership and the promenade timer are copied from the production Java.
This catches an animal reappearing between the one-second needs refreshes,
including layout callbacks and direct pose writers in the two game classes.

Use --source-ref <git ref> to run the same assertions against an earlier tree.
That mode reproduces the v0.8.7 failure without changing the working tree.
Actual bitmap alpha and Android rendering are outside this headless contract.
"""

import argparse
from pathlib import Path
import re
import subprocess
import tempfile

from test_wolf_behavior import java_block


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/byw/monpetitleopard"


SUPPORT = r"""package com.byw.monpetitleopard;
import java.util.*;

class View {
    static final int VISIBLE=0,INVISIBLE=4,GONE=8;
    int visibility=VISIBLE,width=256,height=256;
    float x,y,alpha=1,rotation,scaleX=1,scaleY=1;
    void setVisibility(int value){visibility=value;}
    int getVisibility(){return visibility;}
    int getWidth(){return width;}
    int getHeight(){return height;}
    void setX(float value){x=value;}
    void setY(float value){y=value;}
    void setAlpha(float value){alpha=value;}
    void setScaleX(float value){scaleX=value;}
    void setScaleY(float value){scaleY=value;}
    void setRotation(float value){rotation=value;}
    void bringToFront(){}
}
class ImageView extends View {
    Object drawable;
    void setImageResource(int res){drawable=Integer.valueOf(res);}
    void setImageBitmap(Bitmap bitmap){drawable=bitmap;}
    void setImageDrawable(Object value){drawable=value;}
    Object getDrawable(){return drawable;}
}
class TextView extends View {}
class Button extends TextView {}
class ProgressBar extends View {}
class FrameLayout extends View {}
class LinearLayout extends View {}
class Space extends View {}
class Looper {static Looper getMainLooper(){return new Looper();}}
class Handler {
    final List<Runnable> queued=new ArrayList<>();
    Handler(Looper value){}
    void removeCallbacks(Runnable value){queued.removeIf(item->item==value);}
    void post(Runnable value){queued.add(value);}
    void postDelayed(Runnable value,long delay){queued.add(value);}
}
class Activity {
    protected void onResume(){}
    protected void onPause(){}
    Object getResources(){return this;}
}
class Intent {
    Intent(Object context,Class<?> type){}
    Intent putExtra(String key,int value){return this;}
}
class Bitmap {
    final int resource,width,height;
    Bitmap(int resource,int width,int height){this.resource=resource;this.width=width;this.height=height;}
    int getWidth(){return width;}
    int getHeight(){return height;}
    static Bitmap createBitmap(Bitmap source,int x,int y,int width,int height){
        if(x<0||y<0||x+width>source.width||y+height>source.height)
            throw new IllegalArgumentException("crop outside recording bitmap");
        return new Bitmap(source.resource,width,height);
    }
}
class BitmapFactory {
    static Bitmap decodeResource(Object resources,int res){
        for(String species:new String[]{SPECIES})
            for(MainActivity.PetStage age:MainActivity.PetStage.values()){
                int width=CharacterSprites.forStage(species,age).expectedWidth(res);
                if(width<0)width=GameSprites.forStage(species,age).expectedWidth(res);
                if(width>0)return new Bitmap(res,width,256);
            }
        return new Bitmap(res,256,256);
    }
}
class SharedPreferences {
    final Map<String,Object> values=new HashMap<>();
    long getLong(String key,long fallback){return (Long)values.getOrDefault(key,fallback);}
    boolean getBoolean(String key,boolean fallback){return (Boolean)values.getOrDefault(key,fallback);}
    Editor edit(){return new Editor();}
    final class Editor {
        final Map<String,Object> pending=new HashMap<>();
        Editor putLong(String key,long value){pending.put(key,value);return this;}
        Editor putBoolean(String key,boolean value){pending.put(key,value);return this;}
        void apply(){values.putAll(pending);}
    }
}
class ObjectSystem {static class Item {}}
class PetProfileStore {static final String EXTRA_SLOT="slot";}
class PromenadeActivity {DURATION}
"""


MAIN_STUBS = r"""
    final Runnable ticker=()->ensurePetImage();
    final Runnable animator=()->animateAuto();
    int tickNeedsCalls,saveCalls,startedActivities,assetErrors;
    void tickNeeds(){tickNeedsCalls++;}
    void save(){saveCalls++;}
    void refresh(){ensurePetImage();}
    void openPetChooser(){}
    void addHistory(String value){}
    void toast(String value){}
    void showAssetErrorOnce(){assetErrors++;}
    void markCharacterAssetInvalid(int res,String group,String message){invalidCharacterAssets.add(res);}
    void applyItemEffects(ObjectSystem.Item item,float factor){}
    void wakeForAction(){sleeping=false;}
    void startActivity(Intent intent){startedActivities++;}
    void maybeShowFaceMood(long now){}
    void startMoodExitUp(){activeFaceMood=-1;walking=false;}
    void startFacePresentation(){walking=false;}
    void chooseWalkTarget(){walking=true;targetNX=.7f;targetNY=.5f;}
    float effectFactor(String key,boolean sleepExempt){return 1f;}
    void fatigueNotice(float value){}
    float clamp(float value){return Math.max(0f,Math.min(100f,value));}
    float clamp01(float value){return Math.max(0f,Math.min(1f,value));}
    boolean loadFaceMoodFrames(){
        int res=faceMoodStripRes();
        faceMoodFrames=recordingFrames(res,12);
        return true;
    }
    boolean loadWalkFrames(PetStage expectedStage,WalkMode mode,int res){
        currentWalkStripRes=res;loadedWalkStage=expectedStage;loadedWalkMode=mode;
        currentWalkFrames=recordingFrames(res,6);return true;
    }
    boolean loadActionFrames(ActionAnim type){
        int res=actionResource(type);
        actionFrames=recordingFrames(res,actionFrameCount(type));return true;
    }
    Bitmap[] recordingFrames(int res,int count){
        Bitmap[] frames=new Bitmap[count];
        for(int i=0;i<count;i++)frames[i]=new Bitmap(res,256,256);
        return frames;
    }
"""


LIVING = r"""package com.byw.monpetitleopard;
class LivingRoomGames {
    final MainActivity a;
    ObjectSystem.Item activeItem;
    int beforeCalls,cancels,ropeFrameIndex;
    long ropeFrameAt;
    LivingRoomGames(MainActivity a){this.a=a;}
    boolean fastRun(){return false;}
    int walkFrameAdvance(){return 1;}
    boolean onPetArrived(long now){return false;}
    boolean beforeAnimate(long now){beforeCalls++;return false;}
    void cancel(){cancels++;}
    METHODS
}
"""


GARDEN = r"""package com.byw.monpetitleopard;
class GardenGames {
    final MainActivity a;
    Bitmap[] scratcherFrames;
    int frameIndex,beforeCalls,cancels;
    long frameAt;
    GardenGames(MainActivity a){this.a=a;}
    boolean onPetArrived(long now){return false;}
    boolean beforeAnimate(long now){beforeCalls++;return false;}
    void cancel(){cancels++;}
    METHODS
}
"""


TEST = r"""package com.byw.monpetitleopard;
import java.util.*;

final class PromenadeVisibilityContract {
    static int checks;
    static final List<String> failures=new ArrayList<>();
    static void check(boolean ok,String message){checks++;if(!ok)failures.add(message);}
    static void hidden(MainActivity a,String message){
        check(a.petView.getVisibility()==View.INVISIBLE,a.petSpecies+"/"+a.petStage()+": "+message);
    }
    static void visible(MainActivity a,String message){
        check(a.petView.getVisibility()==View.VISIBLE,a.petSpecies+"/"+a.petStage()+": "+message);
    }
    static MainActivity actor(String species,int stage){
        MainActivity a=new MainActivity();
        long[] ages={1000L,MainActivity.CUB+1000L,MainActivity.CUB+MainActivity.TEEN+1000L,
            MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+1000L};
        a.petSpecies=species;a.born=System.currentTimeMillis()-ages[stage];
        a.sp=new SharedPreferences();a.profileSlot=stage;
        a.petView=new ImageView();a.scene=new FrameLayout();
        a.scene.width=1536;a.scene.height=1152;
        a.incidentView=new TextView();a.moodLabel=new TextView();a.cleanHint=new TextView();
        a.games=new LivingRoomGames(a);a.gardenGames=new GardenGames(a);
        a.ensurePetImage();
        visible(a,"initial animal is visible at home");
        return a;
    }
    static void depart(MainActivity a){
        a.sp.edit().putBoolean("promenadeActive",true)
            .putLong("promenadeStart",System.currentTimeMillis()-1000L).apply();
        a.ensurePetImage();
        hidden(a,"house refresh hides animal during active promenade");
    }
    static void visibilityWriters(String species,int stage){
        MainActivity a=actor(species,stage);depart(a);
        a.updatePetPosition();hidden(a,"layout/placement cannot make animal reappear");
        for(String room:new String[]{"salon","cuisine","bain","jardin"}){
            a.room=room;a.resetPetForRoom(true);
            hidden(a,"room change to "+room+" keeps house empty");
        }
        a.showWalkFrame(MainActivity.WalkMode.FRONT);
        hidden(a,"direct walk-frame callback keeps house empty");
        a.startActionAnimation(MainActivity.ActionAnim.JUMP,5000L);
        a.showActionAnimationFrame(System.currentTimeMillis());
        hidden(a,"direct action-frame callback keeps house empty");
        a.actionAnim=MainActivity.ActionAnim.NONE;
        a.specialPoseRes=CareSprites.forStage(species,a.petStage()).soap;
        a.specialPoseStage=a.petStage();
        check(a.renderSpecialPose(),"care pose actually loads");
        hidden(a,"direct care pose keeps house empty");
        a.clearSpecialPose();
        a.setPetDrawableSafely(a.petStage(),a.idleDownDrawable());
        hidden(a,"direct idle writer keeps house empty");
        a.keepCurrentImageIfSameStage(a.petStage());
        hidden(a,"same-age fallback keeps house empty");
        a.applyPose(0);hidden(a,"manual pose keeps house empty");
        check(a.games.showGamePose(GameSprites.forStage(species,a.petStage()).fetch("tennis")),
            "fetch pose actually loads");
        hidden(a,"fetch pose keeps house empty");
        a.games.showRopePose();hidden(a,"rope pose keeps house empty");
        a.gardenGames.scratcherFrames=a.recordingFrames(
            GardenSprites.forStage(species,a.petStage()).scratcherPlay,1);
        a.gardenGames.showFrame(System.currentTimeMillis());
        hidden(a,"garden scratcher pose keeps house empty");
        check(a.assetErrors==0,"all direct pose paths execute without recording-resource errors");
    }
    static void animationAndResume(String species,int stage){
        for(String mode:new String[]{"idle","walking","sleeping","action","manual"}){
            MainActivity a=actor(species,stage);depart(a);
            if("walking".equals(mode)){a.walking=true;a.targetNX=.9f;a.targetNY=.8f;}
            if("sleeping".equals(mode))a.sleeping=true;
            if("action".equals(mode))a.startActionAnimation(MainActivity.ActionAnim.JUMP,5000L);
            if("manual".equals(mode))a.manualUntil=System.currentTimeMillis()+5000L;
            a.animateAuto();
            hidden(a,"animation tick during "+mode+" keeps house empty");
            check(a.games.beforeCalls==0&&a.gardenGames.beforeCalls==0,
                species+"/"+stage+": "+mode+" bypasses home-game hooks while away");
        }
        MainActivity a=actor(species,stage);
        a.sp.edit().putBoolean("promenadeActive",true)
            .putLong("promenadeStart",System.currentTimeMillis()-1000L).apply();
        a.onResume();
        hidden(a,"onResume hides before the first queued tick");
        check(a.handler.queued.size()==2,"resume schedules one needs tick and one animation tick");
        for(Runnable callback:new ArrayList<>(a.handler.queued))callback.run();
        hidden(a,"queued resume callbacks keep house empty");
        a.onPause();
        check(a.handler.queued.isEmpty(),"pause removes both queued ticks");
        a.onResume();
        hidden(a,"second internal resume keeps house empty");
        a.updatePetPosition();hidden(a,"post-resume layout cannot revive the animal");
    }
    static void departureAndReturn(String species,int stage){
        MainActivity a=actor(species,stage);
        a.walking=true;a.callingToForeground=true;
        a.manualUntil=System.currentTimeMillis()+5000L;
        a.specialPoseRes=CareSprites.forStage(species,a.petStage()).soap;
        a.specialPoseStage=a.petStage();
        a.actionAnim=MainActivity.ActionAnim.JUMP;
        a.startPromenade(new ObjectSystem.Item());
        hidden(a,"departure immediately empties the house");
        check(!a.walking&&!a.callingToForeground&&a.actionAnim==MainActivity.ActionAnim.NONE
            &&a.specialPoseRes==0&&a.manualUntil==0,
            species+"/"+stage+": departure clears pending movement, call and poses");
        check(a.games.cancels>0&&a.gardenGames.cancels>0,
            species+"/"+stage+": departure cancels both kinds of home games");
        long start=a.sp.getLong("promenadeStart",0L);
        a.startPromenade(new ObjectSystem.Item());
        check(a.sp.getLong("promenadeStart",0L)==start,"reopening preserves departure time");
        a.updatePetPosition();hidden(a,"returning to house before expiry stays invisible");
        MainActivity other=actor(species,stage);
        visible(other,"another profile's animal stays visible");
        a.sp.edit().putLong("promenadeStart",System.currentTimeMillis()-PromenadeActivity.DURATION_MS-1000L).apply();
        a.ensurePetImage();
        visible(a,"animal reappears when promenade duration expires");
        check(!a.sp.getBoolean("promenadeActive",true),"expired timer persists return");
        check(a.displayedPetStage==a.petStage(),"return uses current life stage");
        check(((Integer)a.petView.getDrawable()).intValue()==a.idleDownDrawable(),
            "return uses the current species and age's idle image");
        a.updatePetPosition();visible(a,"position update preserves visibility after return");
        a.petView.setImageDrawable(null);a.updatePetPosition();
        hidden(a,"missing drawable stays hidden at home");
        a.petView.setImageResource(a.idleDownDrawable());a.displayedPetStage=null;a.updatePetPosition();
        hidden(a,"wrong/unknown rendered stage stays hidden at home");
    }
    static void ageTransitionWhileAway(String species,int stage){
        if(stage==3)return;
        MainActivity a=actor(species,stage);depart(a);
        long start=a.sp.getLong("promenadeStart",0L);
        long[] boundaries={MainActivity.CUB,MainActivity.CUB+MainActivity.TEEN,
            MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT};
        a.born=System.currentTimeMillis()-boundaries[stage]-1000L;
        a.ensurePetImage();a.updatePetPosition();
        hidden(a,"growth while away keeps house empty");
        check(a.sp.getLong("promenadeStart",0L)==start,"growth preserves promenade timer");
        check(a.visualStage==MainActivity.PetStage.values()[stage+1],"growth switches the visual age");
        a.sp.edit().putLong("promenadeStart",System.currentTimeMillis()-PromenadeActivity.DURATION_MS-1000L).apply();
        a.ensurePetImage();
        visible(a,"grown animal returns after the original promenade duration");
        check(a.displayedPetStage==MainActivity.PetStage.values()[stage+1],"return uses grown age");
        check(((Integer)a.petView.getDrawable()).intValue()==a.idleDownDrawable(),
            "return loads grown species-specific idle image");
    }
    public static void main(String[] args){
        for(String species:new String[]{SPECIES})for(int stage=0;stage<4;stage++){
            visibilityWriters(species,stage);
            animationAndResume(species,stage);
            departureAndReturn(species,stage);
            ageTransitionWhileAway(species,stage);
        }
        if(!failures.isEmpty()){
            for(String failure:failures)System.err.println("FAIL: "+failure);
            throw new AssertionError(failures.size()+" of "+checks+" promenade visibility assertions failed");
        }
        System.out.println("Promenade visibility: "+checks+" production-method assertions, SPECIES_COUNT species × 4 ages: PASS");
        System.out.println("Departure, resume, animation, all pose writers, room/layout callbacks and timed return: PASS");
    }
}
"""


def run(source_ref=None):
    def read(name):
        if source_ref:
            return subprocess.check_output(
                ["git", "show", f"{source_ref}:mon-petit-leopard/app/src/main/java/com/byw/monpetitleopard/{name}.java"],
                cwd=ROOT, text=True,
            )
        return (JAVA / f"{name}.java").read_text()

    main = read("MainActivity")
    methods = [
        "Stage stage()", "PetStage petStage()", "String petEmoji()", "boolean promenadeActive()",
        "void ensurePetImage()", "void syncVisualStage()", "void updatePetPosition()",
        "float[] imageRect()", "float depthScale()", "float[][] roomNodes()",
        "int defaultRoomNode()", "void resetPetForRoom(", "void animateAuto()",
        "void updateTravelDirection(", "void applyPose(", "void startPromenade(",
        "@Override protected void onResume()", "@Override protected void onPause()",
        "int idleDownDrawable()", "int idleDrawableForDirection(", "int faceMoodStripRes()",
        "int getWalkStrip(", "void releaseWalkFrames()", "void releaseFaceMoodFrames()",
        "void releaseActionFrames()", "void clearSpecialPose()", "void showWalkFrame(",
        "boolean renderSpecialPose()", "void showActionAnimationFrame(",
        "void startActionAnimation(", "int actionResource(", "int actionFrameCount(",
        "boolean isMoodResourceForStage(", "boolean keepCurrentImageIfSameStage(",
        "boolean setPetDrawableSafely(",
    ]
    for optional in ["void updatePetVisibility()", "void stopPetActivitiesForPromenade()"]:
        if optional in main:
            methods.append(optional)
    fields = main[main.index("public class MainActivity extends Activity {"):main.index("    @Override public void onCreate")]
    main_source = "package com.byw.monpetitleopard;\nimport java.util.*;\n" + fields
    main_source += "\n".join(java_block(main, method) for method in methods) + MAIN_STUBS + "}\n"
    living = LIVING.replace("METHODS", "\n".join(java_block(read("LivingRoomGames"), method)
        for method in ["boolean showGamePose(", "void showRopePose()"] ))
    garden = GARDEN.replace("METHODS", java_block(read("GardenGames"), "void showFrame("))
    production = {name: read(name) for name in [
        "CharacterSprites", "GameSprites", "CareSprites", "GardenSprites", "PetSpecies", "SpriteMotion",
    ]}
    # Older references predate tiger adoption. Keep the regression-reproduction
    # option exercising exactly the animals that existed in that source tree.
    species = [name for name in ("leopard", "wolf", "tiger")
               if f"R.drawable.{name}_" in production["CharacterSprites"]]
    species_literal = ",".join(f'"{name}"' for name in species)
    contract = TEST.replace("SPECIES_COUNT", str(len(species))).replace("SPECIES", species_literal)
    support = SUPPORT.replace("SPECIES", species_literal)
    resources = sorted(set(re.findall(r"R\.drawable\.(\w+)", "\n".join(production.values()))))
    duration = re.search(r"static final long DURATION_MS=[^;]+;", read("PromenadeActivity")).group(0)
    with tempfile.TemporaryDirectory(prefix="promenade-visibility-") as directory:
        directory = Path(directory)
        package = directory / "com/byw/monpetitleopard"
        package.mkdir(parents=True)
        sources = {
            **production, "MainActivity": main_source, "LivingRoomGames": living,
            "GardenGames": garden, "RecordingAndroid": support.replace("DURATION", duration),
            "PromenadeVisibilityContract": contract,
            "R": "package com.byw.monpetitleopard; class R {static class drawable {" +
                "".join(f"static final int {name}={index};" for index, name in enumerate(resources, 1)) + "}}",
        }
        for name, source in sources.items():
            (package / f"{name}.java").write_text(source)
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(directory),
                        *[str(path) for path in package.glob("*.java")]], check=True)
        subprocess.run(["java", "-cp", str(directory),
                        "com.byw.monpetitleopard.PromenadeVisibilityContract"], check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-ref", help="Read Java from this Git revision instead of the working tree")
    arguments = parser.parse_args()
    run(arguments.source_ref)
