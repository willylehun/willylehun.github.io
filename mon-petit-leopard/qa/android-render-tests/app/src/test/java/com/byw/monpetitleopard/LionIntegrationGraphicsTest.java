package com.byw.monpetitleopard;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

/** Exercises the two lion appearances through real activities, profiles and objects. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class LionIntegrationGraphicsTest {
    Context app;

    @Before public void clearProfiles() {
        app=RuntimeEnvironment.getApplication();
        app.getSharedPreferences("pet_profiles_v079",Context.MODE_PRIVATE).edit().clear().commit();
        app.getSharedPreferences("pet",Context.MODE_PRIVATE).edit().clear().commit();
        for(int slot=0;slot<PetProfileStore.MAX_PROFILES;slot++)
            app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).edit().clear().commit();
        PetProfileStore.ensureMigrated(app);
    }

    @Test public void maleCubActions(){actions("male",MainActivity.PetStage.CUB);}
    @Test public void maleTeenActions(){actions("male",MainActivity.PetStage.TEEN);}
    @Test public void maleAdultActions(){actions("male",MainActivity.PetStage.ADULT);}
    @Test public void maleOldActions(){actions("male",MainActivity.PetStage.OLD);}
    @Test public void femaleCubActions(){actions("female",MainActivity.PetStage.CUB);}
    @Test public void femaleTeenActions(){actions("female",MainActivity.PetStage.TEEN);}
    @Test public void femaleAdultActions(){actions("female",MainActivity.PetStage.ADULT);}
    @Test public void femaleOldActions(){actions("female",MainActivity.PetStage.OLD);}

    void actions(String sex,MainActivity.PetStage stage) {
        AnimalActionGraphicsScenario.run(app,PetSpecies.LION,sex,stage,ageAt(stage));
    }

    @Test @Config(sdk=34, qualifiers="w320dp-h480dp-mdpi")
    public void adoptsMaleLionWithOwnPreviewOnCompactScreen(){adoption("male");}

    @Test @Config(sdk=34, qualifiers="w320dp-h480dp-mdpi")
    public void adoptsFemaleLionWithOwnPreviewOnCompactScreen(){adoption("female");}

    void adoption(String sex) {
        ActivityController<PetChooserActivity> controller=Robolectric.buildActivity(PetChooserActivity.class)
                .create().start().resume().visible();
        PetChooserActivity chooser=controller.get();
        TigerIntegrationGraphicsTest.settleCompact(chooser);
        for(String label:new String[]{"Adopter un léopard","Adopter un loup","Adopter un tigre","Adopter un lion"}) {
            View card=TigerIntegrationGraphicsTest.findDescription(chooser.root,label);
            assertNotNull("All four species remain available: "+label,card);
            revealCompact(chooser,card);
        }
        TigerIntegrationGraphicsTest.capture(chooser,"lion-initial-choice-320x480");
        View lion=TigerIntegrationGraphicsTest.findDescription(chooser.root,"Adopter un lion");
        tapCompact(chooser,lion);
        TigerIntegrationGraphicsTest.settleCompact(chooser);
        for(String previewSex:new String[]{"male","female"}) {
            ImageView preview=findPortrait(chooser.root,PetSpecies.iconRes(PetSpecies.LION,previewSex,0L));
            assertNotNull("Cub portrait is shown before choosing sex: "+previewSex,preview);
            assertTrue("Portrait is large enough to distinguish the variants",preview.getWidth()>=72&&preview.getHeight()>=72);
            revealCompact(chooser,preview);
        }
        Button choice=PromenadeLifecycleGraphicsTest.findButton(chooser.root,
                "female".equals(sex)?"♀  Femelle":"♂  Mâle");
        assertNotNull(choice);
        revealCompact(chooser,choice);
        TigerIntegrationGraphicsTest.capture(chooser,"lion-"+sex+"-sex-choice-320x480");
        tapCompact(chooser,choice);
        TigerIntegrationGraphicsTest.settleCompact(chooser);
        ImageView chosen=findPortrait(chooser.root,PetSpecies.iconRes(PetSpecies.LION,sex,0L));
        assertNotNull("Name screen shows the chosen sex",chosen);
        String opposite="female".equals(sex)?"male":"female";
        assertNull("Name screen has no portrait from the other sex",findPortrait(chooser.root,
                PetSpecies.iconRes(PetSpecies.LION,opposite,0L)));
        EditText name=TigerIntegrationGraphicsTest.findType(chooser.root,EditText.class);
        assertNotNull(name);
        String selectedName="female".equals(sex)?"Nala QA":"Simba QA";
        name.setText(selectedName);
        Button adopt=PromenadeLifecycleGraphicsTest.findButton(chooser.root,
                "female".equals(sex)?"Adopter cette lionne":"Adopter ce lion");
        assertNotNull(adopt);
        revealCompact(chooser,adopt);
        TigerIntegrationGraphicsTest.capture(chooser,"lion-"+sex+"-name-choice-320x480");
        tapCompact(chooser,adopt);
        assertEquals(PetSpecies.LION,PetProfileStore.species(app,0));
        assertEquals(sex,PetProfileStore.sex(app,0));
        assertEquals(selectedName,PetProfileStore.name(app,0));
        assertEquals(PetSpecies.iconRes(PetSpecies.LION,sex,0L),PetProfileStore.iconRes(app,0));
        Intent launch=Shadows.shadowOf(chooser).getNextStartedActivity();
        assertNotNull(launch);
        assertEquals(MainActivity.class.getName(),launch.getComponent().getClassName());
        assertEquals(0,launch.getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
        controller.pause().stop().destroy();

        for(int opening=0;opening<2;opening++) {
            ActivityController<MainActivity> mainController=openMain(0);
            MainActivity main=mainController.get();
            assertEquals(PetSpecies.LION,main.petSpecies);
            assertEquals(sex,main.petSex);
            assertEquals(MainActivity.PetStage.CUB,main.petStage());
            AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
            TigerIntegrationGraphicsTest.closeMain(mainController);
        }
        controller=Robolectric.buildActivity(PetChooserActivity.class).create().start().resume().visible();
        chooser=controller.get();
        assertNotNull("Saved profile uses its chosen sex portrait",findPortrait(chooser.root,
                PetSpecies.iconRes(PetSpecies.LION,sex,0L)));
        chooser.selectExisting(0);
        assertEquals(0,Shadows.shadowOf(chooser).getNextStartedActivity().getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
        controller.pause().stop().destroy();
        System.out.println("NATIVE_LION_ADOPTION_OK sex="+sex+" compact=320x480 species=4 preview=true restart=true");
    }

    @Test public void sixMixedProfilesSwitchWithoutChangingOtherSaves() {
        String[] species={PetSpecies.LION,PetSpecies.LION,PetSpecies.LEOPARD,PetSpecies.WOLF,PetSpecies.TIGER,PetSpecies.LION};
        String[] sexes={"male","female","female","male","female","female"};
        MainActivity.PetStage[] stages={MainActivity.PetStage.CUB,MainActivity.PetStage.CUB,
                MainActivity.PetStage.TEEN,MainActivity.PetStage.OLD,MainActivity.PetStage.ADULT,MainActivity.PetStage.OLD};
        for(int slot=0;slot<6;slot++) {
            PetProfileStore.createAnimal(app,slot,species[slot],sexes[slot],"Compagnon "+slot);
            setAge(slot,ageAt(stages[slot]));
            app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).edit()
                    .putFloat("hunger",40f+slot).putInt("stars",10+slot).commit();
        }
        assertEquals(6,PetProfileStore.count(app));
        assertEquals(-1,PetProfileStore.firstEmpty(app));

        int visit=0;
        for(int slot:new int[]{0,1,0,5,1,4,3,2,0}) {
            List<Map<String,?>> before=profileSnapshots();
            ActivityController<PetChooserActivity> chooserController=Robolectric.buildActivity(PetChooserActivity.class)
                    .create().start().resume().visible();
            PetChooserActivity chooser=chooserController.get();
            PromenadeLifecycleGraphicsTest.settle(chooser);
            for(int profile=0;profile<6;profile++)
                assertNotNull("Profile portrait "+profile,findPortrait(chooser.root,PetProfileStore.iconRes(app,profile)));
            if(visit==0)TigerIntegrationGraphicsTest.capture(chooser,"lion-six-mixed-profiles");
            TextView label=findText(chooser.root,"Compagnon "+slot);
            assertNotNull(label);
            assertTrue(((View)label.getParent()).performClick());
            Intent launch=Shadows.shadowOf(chooser).getNextStartedActivity();
            assertEquals(slot,launch.getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
            chooserController.pause().stop().destroy();

            ActivityController<MainActivity> mainController=openMain(slot);
            MainActivity main=mainController.get();
            try {
                assertEquals(species[slot],main.petSpecies);
                assertEquals(sexes[slot],main.petSex);
                assertEquals(stages[slot],main.petStage());
                AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
                if(visit==2||visit==4) {
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
                assertEquals("Compagnon "+other,PetProfileStore.name(app,other));
            }
            visit++;
        }
        System.out.println("NATIVE_LION_PROFILE_SWITCH_OK profiles=6 visits=9 species=4 lionSexes=2 activeGrowthOnly=true");
    }

    @Test public void offspringUsesPersistedLionSexAndKeepsParentsAndOtherSpecies() {
        String[] species={PetSpecies.LION,PetSpecies.LION,PetSpecies.LEOPARD,PetSpecies.WOLF,PetSpecies.TIGER};
        String[] sexes={"female","male","male","male","male"};
        for(int slot=0;slot<5;slot++) {
            PetProfileStore.createAnimal(app,slot,species[slot],sexes[slot],"Parent ou voisin "+slot);
            setAge(slot,ageAt(MainActivity.PetStage.ADULT));
        }
        assertTrue(PetProfileStore.compatibleParents(app,0,1));
        for(int neighbor=2;neighbor<5;neighbor++)assertFalse(PetProfileStore.compatibleParents(app,0,neighbor));
        List<Map<String,?>> before=profileSnapshots();
        String childSex=PetProfileStore.createOffspring(app,5,0,1,"Petit lion QA");
        assertTrue("male".equals(childSex)||"female".equals(childSex));
        assertEquals(PetSpecies.LION,PetProfileStore.species(app,5));
        assertEquals(childSex,PetProfileStore.sex(app,5));
        assertEquals(childSex,app.getSharedPreferences(PetProfileStore.petPrefsName(5),Context.MODE_PRIVATE).getString("sex",null));
        assertEquals(PetSpecies.iconRes(PetSpecies.LION,childSex,0L),PetProfileStore.iconRes(app,5));
        assertEquals(6,PetProfileStore.count(app));
        assertEquals(-1,PetProfileStore.firstEmpty(app));
        for(int opening=0;opening<2;opening++) {
            ActivityController<MainActivity> controller=openMain(5);
            MainActivity main=controller.get();
            assertEquals(PetSpecies.LION,main.petSpecies);
            assertEquals(childSex,main.petSex);
            assertEquals(MainActivity.PetStage.CUB,main.petStage());
            assertEquals(0,main.sp.getInt("parentA",-1));
            assertEquals(1,main.sp.getInt("parentB",-1));
            AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
            TigerIntegrationGraphicsTest.closeMain(controller);
        }
        for(int slot=0;slot<5;slot++) {
            assertEquals(before.get(slot),profileSnapshot(slot));
            assertEquals(species[slot],PetProfileStore.species(app,slot));
            assertEquals(sexes[slot],PetProfileStore.sex(app,slot));
        }
        System.out.println("NATIVE_LION_OFFSPRING_OK childSex="+childSex+" species=lion ownPack=true parents=2 unchangedSaves=5 restart=true");
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

    static long ageAt(MainActivity.PetStage stage) {
        switch(stage) {
            case CUB:return 10_000L;
            case TEEN:return MainActivity.CUB+10_000L;
            case ADULT:return MainActivity.CUB+MainActivity.TEEN+10_000L;
            default:return MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L;
        }
    }

    Map<String,?> profileSnapshot(int slot) {
        return new HashMap<>(app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).getAll());
    }

    List<Map<String,?>> profileSnapshots() {
        List<Map<String,?>> snapshots=new ArrayList<>();
        for(int slot=0;slot<6;slot++)snapshots.add(profileSnapshot(slot));
        return snapshots;
    }

    static void revealCompact(PetChooserActivity activity,View view) {
        view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true);
        TigerIntegrationGraphicsTest.settleCompact(activity);
        Rect visible=new Rect();
        assertTrue("Choice can be reached on 320x480",view.getGlobalVisibleRect(visible));
        assertEquals("Whole choice is visible vertically",view.getHeight(),visible.height());
        assertEquals("Whole choice fits horizontally",view.getWidth(),visible.width());
        assertTrue("Choice lies inside the actual viewport: "+visible,
                visible.left>=0&&visible.top>=0&&visible.right<=320&&visible.bottom<=480);
    }

    static void tapCompact(PetChooserActivity activity,View view) {
        revealCompact(activity,view);
        Rect visible=new Rect();
        assertTrue(view.getGlobalVisibleRect(visible));
        long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,
                visible.exactCenterX(),visible.exactCenterY(),0);
        MotionEvent up=MotionEvent.obtain(now,now+24L,MotionEvent.ACTION_UP,
                visible.exactCenterX(),visible.exactCenterY(),0);
        try {
            assertTrue("Visible target accepts touch down",activity.dispatchTouchEvent(down));
            assertTrue("Visible target accepts touch up",activity.dispatchTouchEvent(up));
        } finally { down.recycle();up.recycle(); }
        TigerIntegrationGraphicsTest.settleCompact(activity);
    }

    static ImageView findPortrait(View view,int resource) {
        Bitmap expected=BitmapFactory.decodeResource(view.getResources(),resource);
        assertNotNull(expected);
        try { return findPortraitBitmap(view,expected); }
        finally { expected.recycle(); }
    }

    static ImageView findPortraitBitmap(View view,Bitmap expected) {
        if(view instanceof ImageView&&((ImageView)view).getDrawable() instanceof BitmapDrawable) {
            Bitmap shown=((BitmapDrawable)((ImageView)view).getDrawable()).getBitmap();
            if(shown.sameAs(expected))return (ImageView)view;
        }
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                ImageView found=findPortraitBitmap(group.getChildAt(i),expected);
                if(found!=null)return found;
            }
        }
        return null;
    }

    static TextView findText(View view,String text) {
        if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return (TextView)view;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                TextView found=findText(group.getChildAt(i),text);
                if(found!=null)return found;
            }
        }
        return null;
    }
}
