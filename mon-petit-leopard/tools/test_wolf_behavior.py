"""Execute production Java rules for both species without an Android emulator.

The Android UI is replaced by recording stubs. Profile storage, sprite registries,
preferences, the object catalogue/routing and the relevant MainActivity methods
are compiled from the current production source, never reimplemented as a model.
"""

from pathlib import Path
import re
import subprocess
import tempfile

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/byw/monpetitleopard"


def java_block(source, signature):
    """Return a declaration and its body, ignoring braces inside Java literals."""
    start = source.index(signature)
    opening = source.index("{", start)
    depth, state, index = 1, "code", opening + 1
    while index < len(source):
        char = source[index]
        nxt = source[index:index + 2]
        if state == "line":
            if char == "\n":
                state = "code"
        elif state == "comment":
            if nxt == "*/":
                state = "code"
                index += 1
        elif state in ('"', "'"):
            if char == "\\":
                index += 1
            elif char == state:
                state = "code"
        elif nxt == "//":
            state = "line"
            index += 1
        elif nxt == "/*":
            state = "comment"
            index += 1
        elif char in ('"', "'"):
            state = char
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
        index += 1
    raise AssertionError(f"Unclosed production Java block: {signature}")


SHARED_PREFERENCES = """package android.content;
import java.util.Map;
import java.util.Set;
public interface SharedPreferences {
    Map<String,?> getAll();
    String getString(String key,String fallback);
    Set<String> getStringSet(String key,Set<String> fallback);
    boolean getBoolean(String key,boolean fallback);
    long getLong(String key,long fallback);
    int getInt(String key,int fallback);
    float getFloat(String key,float fallback);
    Editor edit();
    interface Editor {
        Editor putString(String key,String value);
        Editor putStringSet(String key,Set<String> value);
        Editor putBoolean(String key,boolean value);
        Editor putLong(String key,long value);
        Editor putInt(String key,int value);
        Editor putFloat(String key,float value);
        Editor remove(String key);
        Editor clear();
        void apply();
    }
}
"""

CONTEXT = """package android.content;
public abstract class Context {
    public static final int MODE_PRIVATE=0;
    public abstract SharedPreferences getSharedPreferences(String name,int mode);
}
"""

INTENT = """package android.content;
public final class Intent {
    public int slot=-1;
    public final Class<?> target;
    public Intent(Context context,Class<?> target){this.target=target;}
    public Intent putExtra(String key,int value){slot=value;return this;}
}
"""

TEST = r"""package com.byw.monpetitleopard;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

final class WolfBehaviorContract {
    static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> values=new HashMap<>();
        int bornReads;
        int changeBornOnRead;
        long changedBorn;
        public Map<String,?> getAll(){return new HashMap<>(values);}
        public String getString(String k,String f){return (String)values.getOrDefault(k,f);}
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String k,Set<String> f){
            return (Set<String>)values.getOrDefault(k,f);
        }
        public boolean getBoolean(String k,boolean f){return (Boolean)values.getOrDefault(k,f);}
        public int getInt(String k,int f){return (Integer)values.getOrDefault(k,f);}
        public float getFloat(String k,float f){return (Float)values.getOrDefault(k,f);}
        public long getLong(String k,long f){
            if("born".equals(k)&&++bornReads==changeBornOnRead)values.put(k,changedBorn);
            return (Long)values.getOrDefault(k,f);
        }
        public Editor edit(){
            return new Editor(){
                final Map<String,Object> pending=new HashMap<>();
                final Set<String> removed=new HashSet<>();
                boolean clear;
                public Editor putString(String k,String v){pending.put(k,v);return this;}
                public Editor putStringSet(String k,Set<String> v){pending.put(k,new HashSet<>(v));return this;}
                public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
                public Editor putLong(String k,long v){pending.put(k,v);return this;}
                public Editor putInt(String k,int v){pending.put(k,v);return this;}
                public Editor putFloat(String k,float v){pending.put(k,v);return this;}
                public Editor remove(String k){removed.add(k);return this;}
                public Editor clear(){clear=true;return this;}
                public void apply(){
                    if(clear)values.clear();
                    for(String key:removed)values.remove(key);
                    values.putAll(pending);
                }
            };
        }
    }

    static final class MemoryContext extends Context {
        final Map<String,MemoryPreferences> files;
        MemoryContext(){files=new HashMap<>();}
        MemoryContext(MemoryContext previous){files=previous.files;}
        public MemoryPreferences getSharedPreferences(String name,int mode){
            return files.computeIfAbsent(name,key->new MemoryPreferences());
        }
        MemoryPreferences pet(int slot){return getSharedPreferences(PetProfileStore.petPrefsName(slot),0);}
        Map<String,Map<String,?>> snapshot(){
            Map<String,Map<String,?>> out=new HashMap<>();
            for(Map.Entry<String,MemoryPreferences> entry:files.entrySet())
                out.put(entry.getKey(),entry.getValue().getAll());
            return out;
        }
    }

    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void equal(Object actual,Object expected,String message){
        check(Objects.equals(actual,expected),message+": "+actual+" != "+expected);
    }
    static void near(float actual,float expected,String message){
        check(Math.abs(actual-expected)<.001f,message+": "+actual+" != "+expected);
    }
    static void rejected(Class<? extends RuntimeException> type,Runnable action,String message){
        try{action.run();}catch(RuntimeException error){
            check(type.isInstance(error),message+": wrong exception "+error);return;
        }
        throw new AssertionError(message+": operation unexpectedly succeeded");
    }
    static long[] ages(){
        return new long[]{1000L,MainActivity.CUB+5000L,
            MainActivity.CUB+MainActivity.TEEN+5000L,
            MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+5000L,
            MainActivity.LIFE+5000L};
    }
    static void setAge(MemoryContext c,int slot,long age){
        c.pet(slot).edit().putLong("born",System.currentTimeMillis()-age).apply();
    }
    static void create(MemoryContext c,int slot,String species,String sex,long age){
        PetProfileStore.createAnimal(c,slot,species,sex,"Animal "+slot);
        setAge(c,slot,age);
    }

    static void migrationAndSaveIsolation(){
        MemoryContext c=new MemoryContext();
        MemoryPreferences legacy=c.getSharedPreferences("pet",0);
        legacy.edit().putString("name","Léo historique").putBoolean("named",true)
            .putLong("born",System.currentTimeMillis()-MainActivity.CUB-5000L)
            .putLong("last",1770000000000L).putFloat("hunger",41.25f)
            .putFloat("thirst",27.5f).putFloat("clean",63.75f)
            .putFloat("energy",11.5f).putInt("stars",179).putInt("generation",3)
            .putBoolean("promenadeActive",true).putLong("promenadeStart",System.currentTimeMillis())
            .putString("room","jardin").putString("history","Souvenir conservé")
            .putStringSet("achievements",new HashSet<>(Arrays.asList("first_walk","friend")))
            .putBoolean("personality_v080_ready",true)
            .putString("personality_food_likes","milk,kibble,fish")
            .putString("personality_food_dislikes","banana,apple,berries")
            .putString("personality_toy_likes","mouse,rope")
            .putString("personality_toy_dislikes","plush,tennis").apply();
        Map<String,?> before=legacy.getAll();
        PetProfileStore.ensureMigrated(c);
        equal(PetProfileStore.count(c),1,"legacy animal is migrated exactly once");
        equal(PetProfileStore.species(c,0),"leopard","legacy species defaults to leopard");
        equal(PetProfileStore.name(c,0),"Léo historique","legacy name survives");
        equal(c.pet(0).getAll(),before,"all legacy values and types survive");
        equal(legacy.getAll(),before,"legacy save remains untouched");
        PetProfileStore.setSex(c,0,"male");
        PetProfileStore.updateName(c,0,"Léo retrouvé");
        PetProfileStore.createAnimal(c,1,"wolf","female","Luna");
        Map<String,Map<String,?>> migrated=c.snapshot();
        PetProfileStore.ensureMigrated(c);
        equal(c.snapshot(),migrated,"migration is idempotent after new wolf adoption");

        MemoryContext resumed=new MemoryContext(c);
        PetProfileStore.ensureMigrated(resumed);
        equal(resumed.snapshot(),migrated,"reopening profiles does not reset either animal");
        equal(PetProfileStore.species(resumed,1),"wolf","wolf species survives reopening");
        equal(PetProfileStore.iconRes(resumed,0),R.drawable.leopard_teen_idle_down,
            "migrated leopard keeps its age and portrait");
        check(c.pet(0)!=c.pet(1),"each animal has its own preferences file");
        c.pet(1).edit().putFloat("hunger",9f).putString("room","bain").apply();
        near(c.pet(0).getFloat("hunger",0),41.25f,"wolf needs do not overwrite leopard needs");
        equal(c.pet(0).getString("room",""),"jardin","room state is isolated");
        equal(c.pet(0).getInt("stars",0),179,"legacy rewards survive new adoption");
        System.out.println("Legacy migration and isolated leopard/wolf saves: PASS");
    }

    static void adoptionSlotsAndGrowth(){
        MemoryContext c=new MemoryContext();
        PetProfileStore.ensureMigrated(c);
        equal(PetProfileStore.count(c),0,"empty install has no preselected animal");
        check(!PetProfileStore.exists(c,-1)&&!PetProfileStore.exists(c,6),"slots are bounded");
        for(int slot=0;slot<6;slot++){
            equal(PetProfileStore.firstEmpty(c),slot,"next available slot");
            String species=slot%2==0?"leopard":"wolf";
            String sex=slot%2==0?"male":"female";
            create(c,slot,species,sex,ages()[slot%4]);
            equal(PetProfileStore.species(c,slot),species,"adoption keeps chosen species");
            equal(PetProfileStore.sex(c,slot),sex,"adoption keeps chosen sex");
        }
        equal(PetProfileStore.count(c),6,"all six mixed animal slots work");
        equal(PetProfileStore.firstEmpty(c),-1,"full profile store has no empty slot");
        Map<String,Map<String,?>> full=c.snapshot();
        rejected(IllegalStateException.class,
            ()->PetProfileStore.createAnimal(c,0,"wolf","female","Replacement"),
            "occupied slot rejects adoption");
        rejected(IllegalArgumentException.class,
            ()->PetProfileStore.createAnimal(c,6,"wolf","male","Overflow"),
            "seventh profile is rejected");
        rejected(IllegalArgumentException.class,
            ()->PetProfileStore.createAnimal(c,-1,"leopard","male","Negative"),
            "negative slot is rejected");
        equal(c.snapshot(),full,"failed adoptions never clear any saved data");

        for(String species:new String[]{"leopard","wolf"}){
            int slot="wolf".equals(species)?1:0;
            for(int stage=0;stage<4;stage++){
                setAge(c,slot,ages()[stage]);
                int expected=CharacterSprites.forStage(species,MainActivity.PetStage.values()[stage]).idleDown;
                equal(PetProfileStore.iconRes(c,slot),expected,"portrait matches species and growing age");
                int token=PetSpecies.promenadeTokenRes(species,ages()[stage]);
                String tokenName="wolf".equals(species)?
                    "wolf_"+MainActivity.PetStage.values()[stage].name().toLowerCase(Locale.ROOT)+"_promenade_token":
                    "promenade_token";
                equal(ResourceNames.NAMES[token],tokenName,"promenade token matches species and growing age");
            }
            long[] boundaries={0L,MainActivity.CUB,MainActivity.CUB+MainActivity.TEEN,
                MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT};
            for(int stage=0;stage<4;stage++){
                int expected=CharacterSprites.forStage(species,MainActivity.PetStage.values()[stage]).idleDown;
                equal(PetSpecies.iconRes(species,boundaries[stage]),expected,"portrait switches exactly at stage boundary");
                if(stage>0)equal(PetSpecies.iconRes(species,boundaries[stage]-1),
                    CharacterSprites.forStage(species,MainActivity.PetStage.values()[stage-1]).idleDown,
                    "portrait retains previous age immediately before boundary");
            }
        }
        MemoryContext defaults=new MemoryContext();
        PetProfileStore.createLeopard(defaults,0,"male",null);
        PetProfileStore.createAnimal(defaults,1,"wolf","female","  ");
        equal(PetProfileStore.name(defaults,0),"Léo","legacy adoption API remains leopard");
        equal(PetProfileStore.name(defaults,1),"Lou","wolf has its own default name");
        equal(PetProfileStore.speciesLabel(defaults,1),"Loup","wolf is named correctly in profile UI");
        equal(PetSpecies.stageLabel("wolf",MainActivity.PetStage.CUB),"Louveteau","cub label is species specific");
        check(!PetProfileStore.reproductionAgeEligible(defaults,0)
            &&!PetProfileStore.reproductionAgeEligible(defaults,1),"new animals are cubs");
        System.out.println("Six mixed profiles, protected saves and species-specific growth: PASS");
    }

    static void reproduction(){
        for(String species:new String[]{"leopard","wolf"}){
            for(int firstAge=0;firstAge<5;firstAge++)for(int secondAge=0;secondAge<5;secondAge++){
                MemoryContext c=new MemoryContext();
                create(c,0,species,"male",ages()[firstAge]);
                create(c,1,species,"female",ages()[secondAge]);
                boolean expected=firstAge>0&&firstAge<4&&secondAge>0&&secondAge<4;
                equal(PetProfileStore.compatibleParents(c,0,1),expected,
                    species+" both parents must be teen/adult/old: "+firstAge+"/"+secondAge);
                equal(PetProfileStore.compatibleParents(c,1,0),expected,"parent order does not change eligibility");
                if(expected){
                    String sex=PetProfileStore.createOffspring(c,2,0,1,"Petit");
                    check("male".equals(sex)||"female".equals(sex),"birth chooses a valid sex");
                    equal(PetProfileStore.sex(c,2),sex,"chosen newborn sex is saved");
                    equal(PetProfileStore.species(c,2),species,"newborn inherits parents' species");
                    equal(c.pet(2).getInt("parentA",-1),0,"first parent is saved");
                    equal(c.pet(2).getInt("parentB",-1),1,"second parent is saved");
                    check(!PetProfileStore.reproductionAgeEligible(c,2),"newborn cannot reproduce");
                    equal(PetProfileStore.iconRes(c,2),CharacterSprites.forStage(species,MainActivity.PetStage.CUB).idleDown,
                        "newborn uses the correct cub sprite");
                }else{
                    rejected(IllegalArgumentException.class,
                        ()->PetProfileStore.createOffspring(c,2,0,1,"Forbidden"),
                        "ineligible parent blocks actual birth");
                    equal(PetProfileStore.count(c),2,"rejected birth creates no profile");
                }
            }

            MemoryContext c=new MemoryContext();
            create(c,0,species,"male",ages()[1]);
            create(c,1,species,"female",ages()[3]);
            check(!PetProfileStore.compatibleParents(c,0,0),"one animal cannot be both parents");
            check(!PetProfileStore.compatibleParents(c,0,5),"missing parent is rejected");
            PetProfileStore.setSex(c,1,"male");
            check(!PetProfileStore.compatibleParents(c,0,1),"same sex is rejected");
            PetProfileStore.setSex(c,1,"");
            check(!PetProfileStore.compatibleParents(c,0,1),"unknown sex is rejected");
            PetProfileStore.setSex(c,1,"female");
            check(PetProfileStore.compatibleParents(c,0,1),"chosen pair is initially eligible");
            setAge(c,1,0L);
            rejected(IllegalArgumentException.class,
                ()->PetProfileStore.createOffspring(c,2,0,1,"Too late"),
                "age is rechecked after parent selection");
            setAge(c,1,ages()[3]);
            Map<String,Map<String,?>> occupied=c.snapshot();
            rejected(IllegalStateException.class,
                ()->PetProfileStore.createOffspring(c,0,0,1,"Overwrite"),
                "birth cannot overwrite a parent or another animal");
            equal(c.snapshot(),occupied,"rejected birth leaves existing saves intact");
            for(int slot=2;slot<6;slot++)create(c,slot,species,"female",ages()[2]);
            equal(PetProfileStore.firstEmpty(c),-1,"full store offers no birth slot");
            Map<String,Map<String,?>> full=c.snapshot();
            rejected(IllegalArgumentException.class,
                ()->PetProfileStore.createOffspring(c,6,0,1,"Seventh"),
                "birth cannot exceed the six-profile capacity");
            equal(c.snapshot(),full,"full-store birth leaves every animal intact");

            // Mutate a parent's age between the public check and the final write.
            // This exercises the second validation inside createProfile itself.
            for(int changedParent=0;changedParent<2;changedParent++){
                MemoryContext race=new MemoryContext();
                create(race,0,species,"male",ages()[2]);
                create(race,1,species,"female",ages()[2]);
                MemoryPreferences changing=race.pet(changedParent);
                changing.bornReads=0;
                changing.changeBornOnRead=2;
                changing.changedBorn=System.currentTimeMillis();
                rejected(IllegalArgumentException.class,
                    ()->PetProfileStore.createOffspring(race,2,0,1,"Race"),
                    "both parent ages are revalidated immediately before saving birth");
                check(changing.bornReads>=2,"the parent was checked again at birth");
                check(!PetProfileStore.exists(race,2),"no newborn save appears after failed final validation");
            }
        }
        MemoryContext mixed=new MemoryContext();
        create(mixed,0,"leopard","male",ages()[2]);
        create(mixed,1,"wolf","female",ages()[2]);
        check(!PetProfileStore.compatibleParents(mixed,0,1),"cross-species reproduction stays forbidden");
        rejected(IllegalArgumentException.class,
            ()->PetProfileStore.createOffspring(mixed,2,0,1,"Hybrid"),"birth rechecks species");
        System.out.println("Reproduction: 50 age pairs, species, sexes, cub bans and final birth validation: PASS");
    }

    static void resource(int id,String species,MainActivity.PetStage stage,String action,int frames,Set<Integer> used){
        check(id>0&&id<ResourceNames.NAMES.length,"resource exists");
        equal(ResourceNames.NAMES[id],species+"_"+stage.name().toLowerCase(Locale.ROOT)+"_"+action,
            "resource belongs to its exact species, age and action");
        equal(ResourceNames.WIDTHS[id],frames*256,"real file width matches the loader frame count");
        equal(ResourceNames.HEIGHTS[id],256,"real file height matches the common frame canvas");
        check(used.add(id),"resource is not reused by another age, species or action");
    }
    static void sprites(){
        Set<Integer> seen=new HashSet<>();
        for(String species:new String[]{"leopard","wolf"})for(MainActivity.PetStage stage:MainActivity.PetStage.values()){
            CharacterSprites.Pack p=CharacterSprites.forStage(species,stage);
            GameSprites.Pack g=GameSprites.forStage(species,stage);
            CareSprites.Pack c=CareSprites.forStage(species,stage);
            GardenSprites.Pack garden=GardenSprites.forStage(species,stage);
            equal(p.species,species,"character registry species");equal(p.stage,stage,"character registry age");
            equal(g.species,species,"game registry species");equal(g.stage,stage,"game registry age");
            equal(c.species,species,"care registry species");equal(c.stage,stage,"care registry age");
            equal(garden.species,species,"garden registry species");equal(garden.stage,stage,"garden registry age");
            for(MainActivity.TravelDirection direction:MainActivity.TravelDirection.values()){
                String suffix=direction.name().toLowerCase(Locale.ROOT);
                resource(p.idle(direction),species,stage,"idle_"+suffix,1,seen);
                resource(p.walk(direction),species,stage,"walk_"+suffix,6,seen);
                resource(g.run(direction),species,stage,"run_"+suffix,6,seen);
                equal(p.expectedWidth(p.idle(direction)),256,"idle is a single frame");
                equal(p.expectedWidth(p.walk(direction)),1536,"walk has six frames");
                equal(g.expectedWidth(g.run(direction)),1536,"run has six frames");
            }
            int[] motions={p.jump,p.eat,p.sleep,p.moods};
            String[] motionNames={"jump","eat","sleep","moods"};
            int[] motionFrames={5,3,3,12};
            for(int i=0;i<motions.length;i++){
                resource(motions[i],species,stage,motionNames[i],motionFrames[i],seen);
                equal(p.expectedWidth(motions[i]),motionFrames[i]*256,"motion strip width");
            }
            for(String toy:new String[]{"ball","tennis","yarn","mouse","plush"}){
                resource(g.fetch(toy),species,stage,"fetch_"+toy,1,seen);
                equal(g.expectedWidth(g.fetch(toy)),256,"fetch uses a whole pose");
            }
            int ropeFrames="wolf".equals(species)?1:5;
            int scratcherFrames="wolf".equals(species)?1:2;
            resource(g.ropePlay,species,stage,"rope_play",ropeFrames,seen);
            equal(g.ropeFrames,ropeFrames,"rope frame count reflects supplied source");
            equal(g.expectedWidth(g.ropePlay),ropeFrames*256,"rope loader never crops the wrong frame count");
            resource(garden.scratcherPlay,species,stage,"scratcher_play",scratcherFrames,seen);
            equal(garden.scratcherFrames,scratcherFrames,"scratcher frame count reflects supplied source");
            equal(garden.expectedWidth(garden.scratcherPlay),scratcherFrames*256,"scratcher strip dimensions");
            for(String action:new String[]{"groom_foam","soap","comb","towel"}){
                resource(c.action(action),species,stage,action,1,seen);
                equal(c.expectedWidth(c.action(action)),256,"care pose width");
            }
            int bottle=CareSprites.bottle(species,stage);
            if(stage==MainActivity.PetStage.CUB)resource(bottle,species,stage,"bottle",1,seen);
            else equal(bottle,0,"bottle pose is only available for cubs");
            equal(p.expectedWidth(0),-1,"character rejects unrelated resource");
            equal(g.expectedWidth(0),-1,"games reject unrelated resource");
            equal(c.expectedWidth(0),-1,"care rejects unrelated resource");
            equal(garden.expectedWidth(0),-1,"garden rejects unrelated resource");
            if("leopard".equals(species)){
                check(CharacterSprites.forStage(stage)==p,"legacy character API remains leopard");
                check(GameSprites.forStage(stage)==g,"legacy game API remains leopard");
                check(CareSprites.forStage(stage)==c,"legacy care API remains leopard");
                check(GardenSprites.forStage(stage)==garden,"legacy garden API remains leopard");
                equal(CareSprites.bottle(stage),bottle,"legacy cub bottle remains leopard");
            }
        }
        equal(seen.size(),218,"109 independent resources per species");
        System.out.println("Eight sprite packs: 218 isolated resources, directions and frame contracts: PASS");
    }

    static MainActivity actor(String species,int age){
        MainActivity a=new MainActivity();
        a.petSpecies=species;
        a.profileSlot=2;
        a.pet="Même personnalité";
        a.born=System.currentTimeMillis()-ages()[age];
        return a;
    }
    static void sameNeeds(MainActivity leopard,MainActivity wolf,String message){
        float[] a={leopard.hunger,leopard.thirst,leopard.clean,leopard.affection,leopard.happy,leopard.energy,
            leopard.skillCare,leopard.skillClean,leopard.waterBowl};
        float[] b={wolf.hunger,wolf.thirst,wolf.clean,wolf.affection,wolf.happy,wolf.energy,
            wolf.skillCare,wolf.skillClean,wolf.waterBowl};
        for(int i=0;i<a.length;i++){
            near(a[i],b[i],message+" need "+i);
            check(a[i]>=0&&a[i]<=100,message+" stays within valid need bounds");
        }
        equal(leopard.stars,wolf.stars,message+" rewards");
        equal(leopard.lastMood,wolf.lastMood,message+" emotional reaction");
    }
    static String expectedRoute(ObjectSystem.Item i){
        if("water".equals(i.id))return "water";
        if("walk".equals(i.id))return "promenade";
        if("scratch".equals(i.id))return "scratcher";
        if("rope".equals(i.kind))return "rope";
        if("rest".equals(i.kind))return "sleep";
        if("toy".equals(i.kind))return "fetch";
        if("bottle".equals(i.id))return "bottle";
        if("groom".equals(i.id))return "groom_foam";
        if("soap".equals(i.id)||"comb".equals(i.id)||"towel".equals(i.id))return i.id;
        return "eat";
    }
    static void gameplayParity(){
        int effectCases=0,routingCases=0;
        for(int age=0;age<4;age++){
            MainActivity catalogue=actor("leopard",age);
            ObjectSystem objects=new ObjectSystem(catalogue);
            Set<String> rooms=new HashSet<>();
            for(ObjectSystem.Item item:objects.items){
                rooms.add(item.room);
                for(float factor:new float[]{0f,.5f,1f}){
                    MainActivity leopard=actor("leopard",age),wolf=actor("wolf",age);
                    for(int repetition=0;repetition<6;repetition++){
                        leopard.applyItemEffects(item,factor);
                        wolf.applyItemEffects(item,factor);
                        sameNeeds(leopard,wolf,age+" "+item.id+" repeated "+repetition+" at "+factor);
                        effectCases++;
                    }
                }
                MainActivity leopard=actor("leopard",age),wolf=actor("wolf",age);
                ObjectSystem lo=new ObjectSystem(leopard),wo=new ObjectSystem(wolf);
                boolean allowed=lo.allowed(item);
                equal(wo.allowed(item),allowed,"same objects unlocked at the same age");
                lo.use(item);wo.use(item);
                equal(wolf.lastAction,leopard.lastAction,"same action route for "+item.id);
                equal(wolf.lastAction,allowed?expectedRoute(item):"","correct route or age lock for "+item.id);
                sameNeeds(leopard,wolf,"using "+item.id);
                routingCases++;
            }
            equal(rooms,new HashSet<>(Arrays.asList("cuisine","salon","bain","jardin")),
                "both animals have all four rooms");
        }
        for(String species:new String[]{"leopard","wolf"}){
            MainActivity cub=actor(species,0);
            for(String food:new String[]{"bottle","milk","junior"})
                equal(PetPreferences.food(cub,food),PetPreferences.NEUTRAL,"cub food remains neutral for "+species);
            MainActivity adult=actor(species,2);
            equal(PetPreferences.food(adult,"bottle"),PetPreferences.NEUTRAL,"bottle remains neutral");
        }
        System.out.println("Shared gameplay parity: "+effectCases+" item/repetition effects and "+routingCases+" age/action routes: PASS");
    }

    static void promenades(){
        for(String species:new String[]{"leopard","wolf"})for(int age=0;age<4;age++){
            MainActivity a=actor(species,age);
            ObjectSystem.Item walk=new ObjectSystem(a).findById("walk","jardin");
            check(!a.promenadeActive(),"no walk before departure");
            a.startPromenade(walk);
            check(a.promenadeActive(),"walk starts for each species and age");
            equal(a.startedIntent.slot,a.profileSlot,"walk keeps selected profile slot");
            check(a.startedIntent.target==PromenadeActivity.class,"walk opens shared promenade activity");
            long started=a.sp.getLong("promenadeStart",0L);
            float clean=a.clean,energy=a.energy,joy=a.happy;
            int stars=a.stars;
            a.startPromenade(walk);
            equal(a.sp.getLong("promenadeStart",0L),started,"reopening walk does not restart its timer");
            near(a.clean,clean,"reopening walk does not duplicate dirt");
            near(a.energy,energy,"reopening walk does not duplicate fatigue");
            near(a.happy,joy,"reopening walk does not duplicate rewards");
            equal(a.stars,stars,"walk awards stars only once");
            MainActivity resumed=actor(species,age);
            resumed.sp=a.sp;
            check(resumed.promenadeActive(),"walk continues after reopening activity");
            MainActivity other=actor(species,age);
            check(!other.promenadeActive(),"another profile remains at home");
            a.sp.edit().putLong("promenadeStart",System.currentTimeMillis()-PromenadeActivity.DURATION_MS-1000L).apply();
            check(!resumed.promenadeActive(),"walk ends after the real duration");
            check(!a.sp.getBoolean("promenadeActive",true),"return is persisted");
        }
        equal(PromenadeActivity.DURATION_MS,180000L,"walk remains three real minutes");
        System.out.println("Eight promenade life stages: departure, resume, profile isolation and return: PASS");
    }

    public static void main(String[] args){
        migrationAndSaveIsolation();
        adoptionSlotsAndGrowth();
        reproduction();
        sprites();
        gameplayParity();
        promenades();
        System.out.println("Wolf integration behavior contracts: PASS");
    }
}
"""


def run():
    main = (JAVA / "MainActivity.java").read_text()
    objects = (JAVA / "ObjectSystem.java").read_text()
    promenade = (JAVA / "PromenadeActivity.java").read_text()
    constant = re.search(r"static final long H=[^;]+;", main).group(0)
    duration = re.search(r"static final long DURATION_MS=[^;]+;", promenade).group(0)
    main_methods = "\n".join(java_block(main, declaration) for declaration in [
        "Stage stage()", "float clamp(float v)", "float effectFactor(",
        "void applyItemEffects(", "boolean promenadeActive()", "void startPromenade(",
        "void fillWaterBowl(",
    ])
    object_methods = "\n".join(java_block(objects, declaration) for declaration in [
        "static class Item", "ObjectSystem(MainActivity a)", "void add(",
        "Item findById(", "boolean allowed(", "ArrayList<Item> itemsFor(", "void use(",
    ])
    main_stub = """package com.byw.monpetitleopard;
import android.content.*;
import java.util.Locale;
class MainActivity extends Context {
    enum PetStage {CUB,TEEN,ADULT,OLD}
    enum TravelDirection {LEFT,RIGHT,UP,DOWN}
    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}
    CONSTANT
    static final long MOOD_DURATION_MS=4000L;
    String petSpecies=PetSpecies.LEOPARD,pet="Léo",repeatKey="",lastAction="";
    int profileSlot=0,repeatCount=0,stars=0,lastMood=-1;
    long born=System.currentTimeMillis(),repeatAt=0;
    float hunger=40,thirst=40,clean=40,affection=40,happy=40,energy=40;
    float skillCare=0,skillClean=0,waterBowl=0;
    boolean internalTransition;
    final WolfBehaviorContract.MemoryContext backing=new WolfBehaviorContract.MemoryContext();
    SharedPreferences sp=backing.getSharedPreferences("selected-profile",0);
    Intent startedIntent;
    final GameRecorder games=new GameRecorder();
    final GameRecorder gardenGames=games;
    public SharedPreferences getSharedPreferences(String key,int mode){return backing.getSharedPreferences(key,mode);}
    void save(){}
    void refresh(){}
    void addHistory(String value){}
    void toast(String value){}
    void wakeForAction(){}
    void fatigueNotice(float value){}
    void refreshWaterBowl(){lastAction="water";}
    void showFaceMoodNow(int mood,long duration){lastMood=mood;}
    void startActivity(Intent intent){startedIntent=intent;lastAction="promenade";}
    void beginAutoSleep(){lastAction="sleep";}
    void performItem(ObjectSystem.Item item,String animation){lastAction=animation;applyItemEffects(item,1f);}
    final class GameRecorder {
        void startFetch(ObjectSystem.Item item){lastAction="fetch";}
        void startRope(ObjectSystem.Item item){lastAction="rope";}
        void startScratcher(ObjectSystem.Item item){lastAction="scratcher";}
    }
    METHODS
}
""".replace("CONSTANT", constant).replace("METHODS", main_methods)
    object_stub = """package com.byw.monpetitleopard;
import java.util.ArrayList;
class ObjectSystem {
    final MainActivity a;
    final ArrayList<Item> items=new ArrayList<>();
    METHODS
}
""".replace("METHODS", object_methods)

    production = ["PetProfileStore", "PetSpecies", "PetBehavior", "PetPreferences",
                  "CharacterSprites", "GameSprites", "CareSprites", "GardenSprites"]
    all_sources = "\n".join((JAVA / f"{name}.java").read_text() for name in production)
    resources = sorted(set(re.findall(r"R\.drawable\.(\w+)", all_sources)))
    widths, heights = [0], [0]
    for name in resources:
        paths = list((ROOT / "app/src/main").glob(f"res*/drawable*/{name}.*"))
        assert len(paths) == 1, f"Resource {name} must resolve to exactly one file: {paths}"
        with Image.open(paths[0]) as bitmap:
            widths.append(bitmap.width)
            heights.append(bitmap.height)

    # The real activities must supply the saved species to every sprite lookup.
    # Otherwise a correct registry could still display leopard artwork for a wolf.
    assert "petSpecies=PetProfileStore.species(this,profileSlot);" in main
    for filename in ["MainActivity", "LivingRoomGames", "GardenGames"]:
        source = (JAVA / f"{filename}.java").read_text()
        calls = re.findall(r"(?:Character|Game|Care|Garden)Sprites\.(?:forStage|bottle)\(([^,)\n]+)", source)
        assert calls, f"No sprite routing found in {filename}"
        assert all(argument in {"petSpecies", "a.petSpecies"} for argument in calls), (
            filename, "sprite lookup bypasses the selected species", calls)
    assert "PetProfileStore.species(PromenadeActivity.this,profileSlot)" in promenade
    assert "PetSpecies.promenadeTokenRes(species,age)" in promenade
    with tempfile.TemporaryDirectory() as directory:
        directory = Path(directory)
        package = directory / "com/byw/monpetitleopard"
        package.mkdir(parents=True)
        android = directory / "android/content"
        android.mkdir(parents=True)
        for filename, source in [("SharedPreferences", SHARED_PREFERENCES), ("Context", CONTEXT), ("Intent", INTENT)]:
            (android / f"{filename}.java").write_text(source)
        for name in production:
            (package / f"{name}.java").write_text((JAVA / f"{name}.java").read_text())
        for name, source in [("MainActivity", main_stub), ("ObjectSystem", object_stub), ("WolfBehaviorContract", TEST)]:
            (package / f"{name}.java").write_text(source)
        (package / "PromenadeActivity.java").write_text(
            "package com.byw.monpetitleopard; final class PromenadeActivity {" + duration + "}")
        (package / "R.java").write_text(
            "package com.byw.monpetitleopard; final class R { static class drawable {" +
            "".join(f"static final int {name}={i};" for i, name in enumerate(resources, 1)) + "}}")
        (package / "ResourceNames.java").write_text(
            "package com.byw.monpetitleopard; final class ResourceNames {static final String[] NAMES={" +
            ",".join(f'"{name}"' for name in ["", *resources]) + "};" +
            "static final int[] WIDTHS={" + ",".join(map(str, widths)) + "};" +
            "static final int[] HEIGHTS={" + ",".join(map(str, heights)) + "};}")
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(directory),
                        *[str(path) for path in directory.rglob("*.java")]], check=True)
        subprocess.run(["java", "-cp", str(directory), "com.byw.monpetitleopard.WolfBehaviorContract"], check=True)


if __name__ == "__main__":
    run()
