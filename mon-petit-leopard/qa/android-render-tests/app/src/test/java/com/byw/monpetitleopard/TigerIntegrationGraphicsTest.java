package com.byw.monpetitleopard;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

/** Runs the production selectors and renderers against compiled tiger resources. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class TigerIntegrationGraphicsTest {
    Context app;

    @Before public void clearProfiles() {
        app=RuntimeEnvironment.getApplication();
        app.getSharedPreferences("pet_profiles_v079",Context.MODE_PRIVATE).edit().clear().commit();
        app.getSharedPreferences("pet",Context.MODE_PRIVATE).edit().clear().commit();
        for(int slot=0;slot<PetProfileStore.MAX_PROFILES;slot++)
            app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).edit().clear().commit();
        PetProfileStore.ensureMigrated(app);
    }

    @Test public void cubLoadsAndRendersOwnActions(){actions(MainActivity.PetStage.CUB,10_000L);}
    @Test public void teenLoadsAndRendersOwnActions(){actions(MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
    @Test public void adultLoadsAndRendersOwnActions(){actions(MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
    @Test public void oldLoadsAndRendersOwnActions(){actions(MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}

    void actions(MainActivity.PetStage stage,long age) {
        AnimalActionGraphicsScenario.run(app,PetSpecies.TIGER,"female",stage,age);
    }

    @Test @Config(sdk=34, qualifiers="w320dp-h480dp-mdpi")
    public void adoptsTigerFromInitialChoiceOnCompactScreenAndReopensProfile() {
        ActivityController<PetChooserActivity> chooserController=Robolectric.buildActivity(PetChooserActivity.class).create().start().resume().visible();
        PetChooserActivity chooser=chooserController.get();
        settleCompact(chooser);
        View tiger=findDescription(chooser.root,"Adopter un tigre");
        assertNotNull("Tiger is an initial choice",tiger);
        assertNotNull(findDescription(chooser.root,"Adopter un léopard"));
        assertNotNull(findDescription(chooser.root,"Adopter un loup"));
        ScrollView scroll=findType(chooser.getWindow().getDecorView(),ScrollView.class);
        assertNotNull(scroll);
        Rect target=new Rect(0,0,tiger.getWidth(),tiger.getHeight());
        tiger.requestRectangleOnScreen(target,true);
        settleCompact(chooser);
        Rect visible=new Rect();
        assertTrue(tiger.getGlobalVisibleRect(visible));
        assertEquals("Whole tiger card is reachable at 320x480",tiger.getHeight(),visible.height());
        assertEquals("Tiger card fits horizontally",tiger.getWidth(),visible.width());
        capture(chooser,"tiger-initial-choice-320x480");
        assertTrue(tiger.performClick());
        assertTrue(PromenadeLifecycleGraphicsTest.findButton(chooser.root,"♀  Femelle").performClick());
        EditText name=findType(chooser.root,EditText.class);
        assertNotNull(name);
        name.setText("Tigra QA");
        Button adopt=PromenadeLifecycleGraphicsTest.findButton(chooser.root,"Adopter ce tigre");
        assertNotNull(adopt);assertTrue(adopt.performClick());
        assertEquals(PetSpecies.TIGER,PetProfileStore.species(app,0));
        assertEquals("Tigra QA",PetProfileStore.name(app,0));
        assertEquals("female",PetProfileStore.sex(app,0));
        Intent launch=Shadows.shadowOf(chooser).getNextStartedActivity();
        assertNotNull(launch);
        assertEquals(MainActivity.class.getName(),launch.getComponent().getClassName());
        assertEquals(0,launch.getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
        chooserController.pause().stop().destroy();
        ActivityController<MainActivity> mainController=openMain(0);
        assertEquals(PetSpecies.TIGER,mainController.get().petSpecies);
        assertEquals(MainActivity.PetStage.CUB,mainController.get().petStage());
        closeMain(mainController);
        chooserController=Robolectric.buildActivity(PetChooserActivity.class).create().start().resume().visible();
        chooser=chooserController.get();
        chooser.selectExisting(0);
        assertEquals(0,Shadows.shadowOf(chooser).getNextStartedActivity().getIntExtra(PetProfileStore.EXTRA_SLOT,-1));
        assertEquals(PetSpecies.TIGER,PetProfileStore.species(chooser,0));
        chooserController.pause().stop().destroy();
        System.out.println("NATIVE_TIGER_ADOPTION_OK compact=320x480 sex=female named=true selection-persisted=true");
    }

    @Test public void offspringKeepsTigerSpeciesAndExistingProfiles() {
        PetProfileStore.createAnimal(app,0,PetSpecies.TIGER,"female","Tigresse");
        PetProfileStore.createAnimal(app,1,PetSpecies.TIGER,"male","Tigre");
        PetProfileStore.createAnimal(app,2,PetSpecies.LEOPARD,"male","Léopard existant");
        PetProfileStore.createAnimal(app,3,PetSpecies.WOLF,"male","Loup existant");
        for(int slot=0;slot<4;slot++)setAge(slot,MainActivity.CUB+MainActivity.TEEN+10_000L);
        assertTrue(PetProfileStore.compatibleParents(app,0,1));
        assertFalse(PetProfileStore.compatibleParents(app,0,2));
        assertFalse(PetProfileStore.compatibleParents(app,0,3));
        PetProfileStore.createOffspring(app,4,0,1,"Petit tigre");
        assertEquals(PetSpecies.TIGER,PetProfileStore.species(app,4));
        assertEquals(PetSpecies.LEOPARD,PetProfileStore.species(app,2));
        assertEquals("Léopard existant",PetProfileStore.name(app,2));
        assertEquals(PetSpecies.WOLF,PetProfileStore.species(app,3));
        assertEquals("Loup existant",PetProfileStore.name(app,3));
        for(int opening=0;opening<2;opening++) {
            ActivityController<MainActivity> controller=openMain(4);
            MainActivity main=controller.get();
            assertEquals(PetSpecies.TIGER,main.petSpecies);
            assertEquals(MainActivity.PetStage.CUB,main.petStage());
            assertEquals(0,main.sp.getInt("parentA",-1));
            assertEquals(1,main.sp.getInt("parentB",-1));
            closeMain(controller);
        }
        System.out.println("NATIVE_TIGER_OFFSPRING_OK same-species=true saved-parents=true existing-profiles-preserved=true");
    }

    void setAge(int slot,long age) {
        app.getSharedPreferences(PetProfileStore.petPrefsName(slot),Context.MODE_PRIVATE).edit()
                .putLong("born",System.currentTimeMillis()-age).commit();
    }

    ActivityController<MainActivity> openMain(int slot) {
        Intent intent=new Intent(app,MainActivity.class).putExtra(PetProfileStore.EXTRA_SLOT,slot);
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();
        PromenadeLifecycleGraphicsTest.settle(controller.get());
        return controller;
    }

    static void closeMain(ActivityController<MainActivity> controller) {
        controller.get().handler.removeCallbacksAndMessages(null);
        controller.pause().stop().destroy();
    }


    static void settleCompact(Activity activity) {
        View decor=activity.getWindow().getDecorView();
        for(int i=0;i<3;i++) {
            decor.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));
            decor.layout(0,0,320,480);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
    }

    static View findDescription(View view,String description) {
        if(view.getContentDescription()!=null&&description.contentEquals(view.getContentDescription()))return view;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                View found=findDescription(group.getChildAt(i),description);if(found!=null)return found;
            }
        }
        return null;
    }

    static <T extends View> T findType(View view,Class<T> type) {
        if(type.isInstance(view))return type.cast(view);
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                T found=findType(group.getChildAt(i),type);if(found!=null)return found;
            }
        }
        return null;
    }

    static void capture(Activity activity,String name) {
        View decor=activity.getWindow().getDecorView();
        Bitmap image=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);
        decor.draw(new Canvas(image));
        File directory=new File(System.getProperty("qa.captureDir","build/native-captures"));directory.mkdirs();
        try(FileOutputStream stream=new FileOutputStream(new File(directory,name+".png"))) {
            image.compress(Bitmap.CompressFormat.PNG,100,stream);
        } catch(IOException error) { throw new AssertionError(error); }
        finally { image.recycle(); }
    }
}
