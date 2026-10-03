"""Execute the developer growth control, persistence and real click handler.

Compiles production Java with recording UI stubs. Boundary cases use a fixed
clock; profile cases exercise the actual six-slot store and both species.
"""

import argparse
from pathlib import Path
import re
import subprocess
import tempfile

from test_wolf_behavior import CONTEXT, SHARED_PREFERENCES, TEST, java_block


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/byw/monpetitleopard"

MEMORY = "\n".join(java_block(TEST, signature) for signature in (
    "static final class MemoryPreferences", "static final class MemoryContext"))

CONTRACT = r"""package com.byw.monpetitleopard;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

final class DevGrowthContract {
    MEMORY
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void equal(Object actual,Object expected,String message){
        check(Objects.equals(actual,expected),message+": "+actual+" != "+expected);
    }

    static void boundaries(){
        long now=2000000000000L;
        long cub=MainActivity.CUB,adult=cub+MainActivity.TEEN;
        long old=adult+MainActivity.ADULT;
        long[] ages={-10000L,0L,1L,cub-1,cub,cub+1,adult-1,adult,adult+1,old-1,
            old,old+1,MainActivity.LIFE-1,MainActivity.LIFE,MainActivity.LIFE+1};
        MainActivity.PetStage[] expected={
            MainActivity.PetStage.TEEN,MainActivity.PetStage.TEEN,
            MainActivity.PetStage.TEEN,MainActivity.PetStage.TEEN,
            MainActivity.PetStage.ADULT,MainActivity.PetStage.ADULT,
            MainActivity.PetStage.ADULT,MainActivity.PetStage.OLD,
            MainActivity.PetStage.OLD,MainActivity.PetStage.OLD,null,null,null,null,null};
        for(int i=0;i<ages.length;i++){
            long born=now-ages[i];
            MemoryPreferences profile=new MemoryPreferences();
            profile.edit().putLong("born",born).putFloat("energy",61f)
                .putLong("last",now-900L).putBoolean("sleeping",true)
                .putLong("sleepEndAt",now+90000L).putBoolean("promenadeActive",true)
                .putLong("promenadeStart",now-30000L).apply();
            Map<String,Object> snapshot=new HashMap<>(profile.getAll());
            equal(DevGrowth.nextStage(born,now),expected[i],"next stage at age "+ages[i]);
            long updated=DevGrowth.advance(profile,now);
            long target=expected[i]==MainActivity.PetStage.TEEN?cub:
                expected[i]==MainActivity.PetStage.ADULT?adult:old;
            long expectedBorn=expected[i]==null?born:now-target;
            equal(updated,expectedBorn,"single boundary jump at age "+ages[i]);
            snapshot.put("born",expectedBorn);
            equal(profile.getAll(),snapshot,"only birth time may change at age "+ages[i]);
        }
        System.out.println("Growth boundaries: 15 cases, exact stage starts and no OLD/ENDED advance: PASS");
    }

    static MemoryContext populated(){
        MemoryContext context=new MemoryContext();
        PetProfileStore.ensureMigrated(context);
        long now=System.currentTimeMillis();
        for(int slot=0;slot<PetProfileStore.MAX_PROFILES;slot++){
            PetProfileStore.createAnimal(context,slot,slot%2==0?"leopard":"wolf",
                slot%2==0?"male":"female","Test "+slot);
            context.pet(slot).edit().putLong("born",now-1000L)
                .putLong("last",now-1234L).putFloat("hunger",72f)
                .putFloat("thirst",68f).putFloat("clean",91f)
                .putFloat("energy",61f).putInt("stars",19)
                .putLong("sleepEndAt",now+70000L).putBoolean("sleeping",true)
                .putBoolean("promenadeActive",true).putLong("promenadeStart",now-30000L)
                .putInt("repeatCount",3).putLong("nextMischiefAt",now+55555L).apply();
        }
        return context;
    }

    static void profilesAndClickHandler(){
        MemoryContext context=populated();
        for(int slot=0;slot<PetProfileStore.MAX_PROFILES;slot++){
            MainActivity activity=new MainActivity(context,slot);
            for(MainActivity.PetStage expected:new MainActivity.PetStage[]{
                    MainActivity.PetStage.TEEN,MainActivity.PetStage.ADULT,MainActivity.PetStage.OLD}){
                Map<String,Map<String,?>> snapshot=context.snapshot();
                activity.refreshDevGrowthButton();
                check(activity.devGrowthBtn.enabled,"young animal has an enabled button");
                check(activity.devGrowthBtn.label.contains(PetSpecies.stageLabel(activity.petSpecies,expected)),
                    "button names the actual next age for the selected species");
                int previousSync=activity.syncCalls,previousRefresh=activity.refreshCalls;
                activity.advanceGrowthForTesting();
                equal(activity.petStage(),expected,"one click advances exactly one age");
                equal(activity.syncCalls,previousSync+1,"click invokes existing visual transition");
                equal(activity.refreshCalls,previousRefresh+1,"click refreshes the screen immediately");
                equal(activity.stageWhenSynced,expected,"birth time changes before visual reload");
                equal(activity.sp.getLong("born",0L),activity.born,"clicked age is saved immediately");
                Map<String,Object> changed=new HashMap<>(snapshot.get(PetProfileStore.petPrefsName(slot)));
                changed.put("born",activity.born);
                changed.put("historyLog",activity.historyLog);
                check(activity.historyLog.contains("Test développeur : "),"manual growth is recorded in history");
                snapshot.put(PetProfileStore.petPrefsName(slot),changed);
                equal(context.snapshot(),snapshot,"click preserves needs, walk, sleep, identity and other slots");

                // Reopen the stored profile: the age and chooser portrait survive recreation.
                MainActivity reopened=new MainActivity(new MemoryContext(context),slot);
                equal(reopened.petStage(),expected,"saved age survives reopening");
                int expectedIcon=PetSpecies.iconRes(activity.petSpecies,
                    System.currentTimeMillis()-activity.born);
                equal(PetProfileStore.iconRes(context,slot),expectedIcon,"chooser uses the grown portrait");
                check(PetProfileStore.reproductionAgeEligible(context,slot),"grown profile unlocks normal age rules");
                long target=expected==MainActivity.PetStage.TEEN?MainActivity.TEEN:
                    expected==MainActivity.PetStage.ADULT?MainActivity.ADULT:MainActivity.OLD;
                check(reopened.remain()<=target&&reopened.remain()>target-10000L,
                    "new stage starts with its normal full duration");
            }
            check(!activity.devGrowthBtn.enabled,"button is disabled on old age");
            check(activity.devGrowthBtn.label.contains("maximum"),"old state explains the limit");
            Map<String,Map<String,?>> snapshot=context.snapshot();
            int previousSync=activity.syncCalls;
            for(int click=0;click<20;click++)activity.advanceGrowthForTesting();
            equal(context.snapshot(),snapshot,"repeated old-age clicks do not end life or alter saves");
            equal(activity.syncCalls,previousSync,"no visual restart for rejected clicks");
        }

        MainActivity ended=new MainActivity(context,0);
        ended.born=System.currentTimeMillis()-MainActivity.LIFE-1000L;
        ended.sp.edit().putLong("born",ended.born).apply();
        ended.refreshDevGrowthButton();
        check(!ended.devGrowthBtn.enabled,"completed lifecycle disables growth");
        check(ended.devGrowthBtn.label.contains("Cycle terminé"),"ended label is explicit");
        Map<String,Map<String,?>> snapshot=context.snapshot();
        ended.advanceGrowthForTesting();
        equal(context.snapshot(),snapshot,"completed profile is never revived");
        equal(ended.syncCalls,0,"completed profile is not visually restarted");
        System.out.println("Growth clicks: six mixed profiles, 18 transitions, persistence/isolation and 120 bounded taps: PASS");
    }

    static void disabledControl(){
        MemoryContext context=populated();
        MainActivity activity=new MainActivity(context,0);
        Map<String,Map<String,?>> snapshot=context.snapshot();
        check(DevGrowth.nextStage(activity.born,System.currentTimeMillis())==null,
            "disabled feature exposes no next stage");
        DevGrowth.advance(activity.sp,System.currentTimeMillis());
        activity.advanceGrowthForTesting();
        equal(context.snapshot(),snapshot,"central flag disables writes through either entry point");
        equal(activity.syncCalls,0,"disabled feature leaves rendering alone");
        System.out.println("Growth removal flag: no age mutation or UI transition when disabled: PASS");
    }

    public static void main(String[] args){
        if(DevGrowth.ENABLED){boundaries();profilesAndClickHandler();}
        else disabledControl();
    }
}
""".replace("MEMORY", MEMORY)


def run(main_path):
    main = main_path.read_text()
    constant = re.search(r"static final long H=[^;]+;", main).group(0)
    methods = "\n".join(java_block(main, signature) for signature in (
        "Stage stage()", "PetStage petStage()", "String stageName()", "long remain()",
        "void advanceGrowthForTesting()", "void refreshDevGrowthButton()",
        "void addHistory(", "List<String> splitLog(", "String joinLog(", "String historyTime()"))
    stub = """package com.byw.monpetitleopard;
import android.content.*;
import java.util.*;
class MainActivity extends Context {
    enum PetStage {CUB,TEEN,ADULT,OLD}
    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}
    CONSTANT
    final DevGrowthContract.MemoryContext context;
    final SharedPreferences sp;
    final int profileSlot;
    final String petSpecies,pet;
    String historyLog;
    long born;
    int syncCalls,refreshCalls;
    PetStage stageWhenSynced;
    final RecordingButton devGrowthBtn=new RecordingButton();
    MainActivity(DevGrowthContract.MemoryContext context,int slot){
        this.context=context;profileSlot=slot;
        sp=context.pet(slot);born=sp.getLong("born",0L);
        historyLog=sp.getString("historyLog","");
        petSpecies=PetProfileStore.species(context,slot);pet=PetProfileStore.name(context,slot);
    }
    public SharedPreferences getSharedPreferences(String name,int mode){return context.getSharedPreferences(name,mode);}
    void syncVisualStage(){syncCalls++;stageWhenSynced=petStage();}
    void refresh(){refreshCalls++;refreshDevGrowthButton();}
    void toast(String message){}
    void tickNeeds(){throw new AssertionError("growth must not simulate need decay");}
    void save(){throw new AssertionError("growth must persist only born directly");}
    static final class RecordingButton {
        boolean enabled;
        String label,description;
        float alpha;
        void setEnabled(boolean value){enabled=value;}
        void setAlpha(float value){alpha=value;}
        void setText(String value){label=value;}
        void setContentDescription(String value){description=value;}
    }
    METHODS
}
""".replace("CONSTANT", constant).replace("METHODS", methods)
    production = {name: (JAVA / f"{name}.java").read_text() for name in
                  ("DevGrowth", "PetProfileStore", "PetSpecies", "PetBehavior")}
    resources = sorted(set(re.findall(r"R\.drawable\.(\w+)", "\n".join(production.values()))))
    with tempfile.TemporaryDirectory() as directory:
        directory = Path(directory)
        package = directory / "com/byw/monpetitleopard"
        package.mkdir(parents=True)
        android = directory / "android/content"
        android.mkdir(parents=True)
        (android / "SharedPreferences.java").write_text(SHARED_PREFERENCES)
        (android / "Context.java").write_text(CONTEXT)
        for name, source in production.items():
            (package / f"{name}.java").write_text(source)
        (package / "MainActivity.java").write_text(stub)
        (package / "DevGrowthContract.java").write_text(CONTRACT)
        (package / "R.java").write_text(
            "package com.byw.monpetitleopard; final class R { static class drawable {" +
            "".join(f"static final int {name}={index};" for index, name in enumerate(resources, 1)) + "}}")
        for enabled in (True, False):
            growth = production["DevGrowth"]
            if not enabled:
                # Exercise the same one-line switch used to retire this temporary UI.
                growth = growth.replace("static final boolean ENABLED=true;", "static final boolean ENABLED=false;")
            (package / "DevGrowth.java").write_text(growth)
            subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(directory),
                            *map(str, directory.rglob("*.java"))], check=True)
            subprocess.run(["java", "-cp", str(directory),
                            "com.byw.monpetitleopard.DevGrowthContract"], check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--main-source", type=Path, default=JAVA / "MainActivity.java")
    run(parser.parse_args().main_source)
