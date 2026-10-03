package com.byw.monpetitleopard;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;

/** Runs fox and bear adoption, profiles and objects through the production activities. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class ForestIntegrationGraphicsTest {
    Context app;

    @Before public void clearProfiles() {
        app=RuntimeEnvironment.getApplication();
        app.getSharedPreferences("pet_profiles_v079",Context.MODE_PRIVATE).edit().clear().commit();
        app.getSharedPreferences("pet",Context.MODE_PRIVATE).edit().clear().commit();
        for(int slot=0;slot<PetProfileStore.MAX_PROFILES;slot++)
            app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).edit().clear().commit();
        PetProfileStore.ensureMigrated(app);
    }

    @Test public void foxCubActions(){actions(PetSpecies.FOX,MainActivity.PetStage.CUB);}
    @Test public void foxTeenActions(){actions(PetSpecies.FOX,MainActivity.PetStage.TEEN);}
    @Test public void foxAdultActions(){actions(PetSpecies.FOX,MainActivity.PetStage.ADULT);}
    @Test public void foxOldActions(){actions(PetSpecies.FOX,MainActivity.PetStage.OLD);}
    @Test public void bearCubActions(){actions(PetSpecies.BEAR,MainActivity.PetStage.CUB);}
    @Test public void bearTeenActions(){actions(PetSpecies.BEAR,MainActivity.PetStage.TEEN);}
    @Test public void bearAdultActions(){actions(PetSpecies.BEAR,MainActivity.PetStage.ADULT);}
    @Test public void bearOldActions(){actions(PetSpecies.BEAR,MainActivity.PetStage.OLD);}

    void actions(String species,MainActivity.PetStage stage) {
        AnimalActionGraphicsScenario.run(app,species,"female",stage,LionIntegrationGraphicsTest.ageAt(stage));
    }

    @Test @Config(sdk=34, qualifiers="w320dp-h480dp-mdpi")
    public void adoptsFemaleFoxWithSixReachableChoicesAndReopensProfile(){adoption(PetSpecies.FOX,"female","Renarde QA");}

    @Test @Config(sdk=34, qualifiers="w320dp-h480dp-mdpi")
    public void adoptsMaleBearWithSixReachableChoicesAndReopensProfile(){adoption(PetSpecies.BEAR,"male","Ours QA");}

    void adoption(String species,String sex,String selectedName) {
        ActivityController<PetChooserActivity> controller=Robolectric.buildActivity(PetChooserActivity.class)
                .create().start().resume().visible();
        PetChooserActivity chooser=controller.get();
        TigerIntegrationGraphicsTest.settleCompact(chooser);
        TigerIntegrationGraphicsTest.capture(chooser,species+"-six-choices-top-320x480");
        String[] allSpecies={PetSpecies.LEOPARD,PetSpecies.WOLF,PetSpecies.TIGER,PetSpecies.LION,PetSpecies.FOX,PetSpecies.BEAR};
        String[] descriptions={"Adopter un léopard","Adopter un loup","Adopter un tigre","Adopter un lion","Adopter un renard","Adopter un ours"};
        for(int index=0;index<allSpecies.length;index++) {
            View card=TigerIntegrationGraphicsTest.findDescription(chooser.root,descriptions[index]);
            assertNotNull("All six species remain available: "+descriptions[index],card);
            revealByTouch(chooser,card);
            ImageView portrait=LionIntegrationGraphicsTest.findPortrait(card,PetSpecies.iconRes(allSpecies[index],"male",0L));
            assertNotNull("Initial card uses its own cub portrait: "+allSpecies[index],portrait);
            assertTrue("Initial portrait remains readable",portrait.getWidth()>=72&&portrait.getHeight()>=72);
            assertEquals("Scrolling never adopts an animal",0,PetProfileStore.count(app));
            assertNull("Scrolling never launches another activity",Shadows.shadowOf(chooser).getNextStartedActivity());
        }
        TigerIntegrationGraphicsTest.capture(chooser,species+"-six-choices-bottom-320x480");
        View chosenCard=TigerIntegrationGraphicsTest.findDescription(chooser.root,
                PetSpecies.FOX.equals(species)?"Adopter un renard":"Adopter un ours");
        tapVisible(chooser,chosenCard);

        ImageView sexPreview=LionIntegrationGraphicsTest.findPortrait(chooser.root,PetSpecies.iconRes(species,sex,0L));
        assertNotNull("Sex screen keeps the selected species",sexPreview);
        revealByTouch(chooser,sexPreview);
        assertEquals("The two sexes use the same supplied artwork",PetSpecies.iconRes(species,"male",0L),PetSpecies.iconRes(species,"female",0L));
        Button sexButton=PromenadeLifecycleGraphicsTest.findButton(chooser.root,
                "female".equals(sex)?"♀  Femelle":"♂  Mâle");
        assertNotNull(sexButton);
        revealByTouch(chooser,sexButton);
        TigerIntegrationGraphicsTest.capture(chooser,species+"-sex-choice-320x480");
        tapVisible(chooser,sexButton);

        ImageView namePreview=LionIntegrationGraphicsTest.findPortrait(chooser.root,PetSpecies.iconRes(species,sex,0L));
        assertNotNull("Name screen keeps the selected species",namePreview);
        revealByTouch(chooser,namePreview);
        EditText name=TigerIntegrationGraphicsTest.findType(chooser.root,EditText.class);
        assertNotNull(name);
        name.setText(selectedName);
        Button adopt=PromenadeLifecycleGraphicsTest.findButton(chooser.root,
                PetSpecies.FOX.equals(species)?"Adopter ce renard":"Adopter cet ours");
        assertNotNull(adopt);
        revealByTouch(chooser,adopt);
        TigerIntegrationGraphicsTest.capture(chooser,species+"-name-choice-320x480");
        tapVisible(chooser,adopt);
        assertEquals(species,PetProfileStore.species(app,0));
        assertEquals(sex,PetProfileStore.sex(app,0));
        assertEquals(selectedName,PetProfileStore.name(app,0));
        assertEquals(PetSpecies.iconRes(species,sex,0L),PetProfileStore.iconRes(app,0));
        Intent launch=Shadows.shadowOf(chooser).getNextStartedActivity();
        assertNotNull(launch);
        assertEquals(MainActivity.class.getName(),launch.getComponent().getClassName());
        assertEquals(0,launch.getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
        controller.pause().stop().destroy();

        for(int opening=0;opening<2;opening++) {
            ActivityController<MainActivity> mainController=openMain(0);
            MainActivity main=mainController.get();
            assertEquals(species,main.petSpecies);
            assertEquals(sex,main.petSex);
            assertEquals(MainActivity.PetStage.CUB,main.petStage());
            AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
            TigerIntegrationGraphicsTest.closeMain(mainController);
        }
        controller=Robolectric.buildActivity(PetChooserActivity.class).create().start().resume().visible();
        chooser=controller.get();
        TigerIntegrationGraphicsTest.settleCompact(chooser);
        assertNotNull("Saved profile keeps its own portrait",LionIntegrationGraphicsTest.findPortrait(chooser.root,
                PetSpecies.iconRes(species,sex,0L)));
        TextView savedName=LionIntegrationGraphicsTest.findText(chooser.root,selectedName);
        assertNotNull(savedName);
        tapVisible(chooser,(View)savedName.getParent());
        Intent reopened=Shadows.shadowOf(chooser).getNextStartedActivity();
        assertNotNull(reopened);
        assertEquals(0,reopened.getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
        controller.pause().stop().destroy();
        System.out.println("NATIVE_FOREST_ADOPTION_OK species="+species+" sex="+sex+" compact=320x480 speciesChoices=6 touchScroll=true touchAdoption=true preview=true restart=true");
    }

    @Test public void sixSpeciesSwitchWithoutChangingInactiveSaves() {
        String[] species={PetSpecies.FOX,PetSpecies.BEAR,PetSpecies.LEOPARD,PetSpecies.WOLF,PetSpecies.TIGER,PetSpecies.LION};
        String[] sexes={"male","female","female","male","female","female"};
        MainActivity.PetStage[] stages={MainActivity.PetStage.CUB,MainActivity.PetStage.CUB,
                MainActivity.PetStage.TEEN,MainActivity.PetStage.OLD,MainActivity.PetStage.ADULT,MainActivity.PetStage.OLD};
        for(int slot=0;slot<6;slot++) {
            PetProfileStore.createAnimal(app,slot,species[slot],sexes[slot],"Compagnon forêt "+slot);
            setAge(slot,LionIntegrationGraphicsTest.ageAt(stages[slot]));
            app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).edit()
                    .putFloat("hunger",40f+slot).putInt("stars",10+slot).commit();
        }
        assertEquals(6,PetProfileStore.count(app));
        assertEquals(-1,PetProfileStore.firstEmpty(app));

        int visit=0;
        for(int slot:new int[]{0,1,0,1,5,4,3,2,0,1}) {
            List<Map<String,?>> before=profileSnapshots();
            ActivityController<PetChooserActivity> chooserController=Robolectric.buildActivity(PetChooserActivity.class)
                    .create().start().resume().visible();
            PetChooserActivity chooser=chooserController.get();
            PromenadeLifecycleGraphicsTest.settle(chooser);
            for(int profile=0;profile<6;profile++)
                assertNotNull("Profile uses its own current portrait: "+species[profile],
                        LionIntegrationGraphicsTest.findPortrait(chooser.root,PetProfileStore.iconRes(app,profile)));
            if(visit==0)TigerIntegrationGraphicsTest.capture(chooser,"forest-six-species-profiles");
            TextView label=LionIntegrationGraphicsTest.findText(chooser.root,"Compagnon forêt "+slot);
            assertNotNull(label);
            assertTrue(((View)label.getParent()).performClick());
            Intent launch=Shadows.shadowOf(chooser).getNextStartedActivity();
            assertNotNull(launch);
            assertEquals(slot,launch.getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
            chooserController.pause().stop().destroy();

            ActivityController<MainActivity> mainController=openMain(slot);
            MainActivity main=mainController.get();
            try {
                assertEquals(species[slot],main.petSpecies);
                assertEquals(sexes[slot],main.petSex);
                assertEquals(stages[slot],main.petStage());
                AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
                if(visit==2||visit==3) {
                    assertTrue(main.devGrowthBtn.performClick());
                    stages[slot]=MainActivity.PetStage.TEEN;
                    assertEquals(stages[slot],main.petStage());
                    PromenadeLifecycleGraphicsTest.settle(main);
                    AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
                }
            } finally { TigerIntegrationGraphicsTest.closeMain(mainController); }
            for(int other=0;other<6;other++) {
                if(other!=slot)assertEquals("Inactive save "+other+" unchanged while using "+slot,
                        before.get(other),profileSnapshot(other));
                assertEquals(species[other],PetProfileStore.species(app,other));
                assertEquals(sexes[other],PetProfileStore.sex(app,other));
                assertEquals("Compagnon forêt "+other,PetProfileStore.name(app,other));
            }
            visit++;
        }
        System.out.println("NATIVE_FOREST_PROFILE_SWITCH_OK profiles=6 visits=10 species=6 foxBearGrowth=true inactiveSavesUnchanged=true");
    }

    @Test public void foxOffspringKeepsSpeciesSexParentsAndOtherSaves(){offspring(PetSpecies.FOX,PetSpecies.BEAR);}
    @Test public void bearOffspringKeepsSpeciesSexParentsAndOtherSaves(){offspring(PetSpecies.BEAR,PetSpecies.FOX);}

    void offspring(String parentSpecies,String neighborSpecies) {
        String[] species={parentSpecies,parentSpecies,neighborSpecies,PetSpecies.LION,PetSpecies.TIGER};
        String[] sexes={"female","male","male","male","male"};
        for(int slot=0;slot<5;slot++) {
            PetProfileStore.createAnimal(app,slot,species[slot],sexes[slot],"Parent ou voisin "+slot);
            setAge(slot,LionIntegrationGraphicsTest.ageAt(MainActivity.PetStage.ADULT));
        }
        assertTrue(PetProfileStore.compatibleParents(app,0,1));
        for(int neighbor=2;neighbor<5;neighbor++)assertFalse(PetProfileStore.compatibleParents(app,0,neighbor));
        List<Map<String,?>> before=profileSnapshots();
        String childSex=PetProfileStore.createOffspring(app,5,0,1,"Petit compagnon QA");
        assertTrue("male".equals(childSex)||"female".equals(childSex));
        assertEquals(parentSpecies,PetProfileStore.species(app,5));
        assertEquals(childSex,PetProfileStore.sex(app,5));
        assertEquals(PetSpecies.iconRes(parentSpecies,childSex,0L),PetProfileStore.iconRes(app,5));
        assertEquals(6,PetProfileStore.count(app));
        assertEquals(-1,PetProfileStore.firstEmpty(app));
        for(int opening=0;opening<2;opening++) {
            ActivityController<MainActivity> controller=openMain(5);
            MainActivity main=controller.get();
            assertEquals(parentSpecies,main.petSpecies);
            assertEquals(childSex,main.petSex);
            assertEquals(MainActivity.PetStage.CUB,main.petStage());
            assertEquals(0,main.sp.getInt("parentA",-1));
            assertEquals(1,main.sp.getInt("parentB",-1));
            AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
            TigerIntegrationGraphicsTest.closeMain(controller);
        }
        for(int slot=0;slot<5;slot++)assertEquals("Parent and neighbor saves stay intact",before.get(slot),profileSnapshot(slot));
        System.out.println("NATIVE_FOREST_OFFSPRING_OK species="+parentSpecies+" childSex="+childSex+" ownPack=true parents=2 unchangedSaves=5 restart=true");
    }

    ActivityController<MainActivity> openMain(int slot) {
        Intent intent=new Intent(app,MainActivity.class).putExtra(PetProfileStore.EXTRA_SLOT,slot);
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,intent)
                .create().start().resume().visible();
        PromenadeLifecycleGraphicsTest.settle(controller.get());
        return controller;
    }

    void setAge(int slot,long age) {
        app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).edit()
                .putLong("born",System.currentTimeMillis()-age).commit();
    }

    Map<String,?> profileSnapshot(int slot) {
        return new HashMap<>(app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).getAll());
    }

    List<Map<String,?>> profileSnapshots() {
        List<Map<String,?>> snapshots=new ArrayList<>();
        for(int slot=0;slot<6;slot++)snapshots.add(profileSnapshot(slot));
        return snapshots;
    }

    /** Reaches a whole control by actual drag events, without scrolling or clicking it programmatically. */
    static void revealByTouch(PetChooserActivity activity,View target) {
        assertNotNull(target);
        TigerIntegrationGraphicsTest.settleCompact(activity);
        ScrollView scroll=TigerIntegrationGraphicsTest.findType(activity.getWindow().getDecorView(),ScrollView.class);
        assertNotNull(scroll);
        Rect viewport=new Rect();
        assertTrue(scroll.getGlobalVisibleRect(viewport));
        assertTrue(viewport.intersect(0,0,320,480));
        assertTrue("Whole target fits the viewport height",target.getHeight()<=viewport.height());
        for(int attempt=0;attempt<8;attempt++) {
            Rect visible=new Rect();
            if(target.getGlobalVisibleRect(visible)&&visible.width()==target.getWidth()
                    &&visible.height()==target.getHeight()&&viewport.contains(visible))return;
            int[] location=new int[2];target.getLocationOnScreen(location);
            boolean scrollDown=location[1]+target.getHeight()>viewport.bottom;
            int before=scroll.getScrollY();
            swipe(activity,viewport,scrollDown);
            TigerIntegrationGraphicsTest.settleCompact(activity);
            assertNotEquals("Real drag moves toward the clipped control",before,scroll.getScrollY());
        }
        fail("Whole control cannot be reached with real scrolling on 320x480");
    }

    static void tapVisible(PetChooserActivity activity,View target) {
        revealByTouch(activity,target);
        Rect bounds=new Rect();assertTrue(target.getGlobalVisibleRect(bounds));
        long start=SystemClock.uptimeMillis();
        touch(activity,start,start,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY());
        touch(activity,start,start+24L,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY());
        TigerIntegrationGraphicsTest.settleCompact(activity);
    }

    static void swipe(PetChooserActivity activity,Rect viewport,boolean scrollDown) {
        float x=viewport.exactCenterX();
        float top=viewport.top+viewport.height()*.25f,bottom=viewport.top+viewport.height()*.75f;
        float from=scrollDown?bottom:top,to=scrollDown?top:bottom;
        long start=SystemClock.uptimeMillis();
        touch(activity,start,start,MotionEvent.ACTION_DOWN,x,from);
        for(int step=1;step<=10;step++)
            touch(activity,start,start+step*32L,MotionEvent.ACTION_MOVE,x,from+(to-from)*step/10f);
        // A stationary end avoids inertial flings while retaining a complete DOWN/MOVE/UP gesture.
        touch(activity,start,start+520L,MotionEvent.ACTION_MOVE,x,to);
        touch(activity,start,start+552L,MotionEvent.ACTION_UP,x,to);
    }

    static void touch(PetChooserActivity activity,long downTime,long eventTime,int action,float x,float y) {
        MotionEvent event=MotionEvent.obtain(downTime,eventTime,action,x,y,0);
        try { assertTrue("Visible control accepts event "+action,activity.dispatchTouchEvent(event)); }
        finally { event.recycle(); }
    }
}
