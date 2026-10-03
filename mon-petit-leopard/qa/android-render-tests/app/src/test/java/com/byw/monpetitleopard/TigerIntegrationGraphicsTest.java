package com.byw.monpetitleopard;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Looper;
import android.view.MotionEvent;
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
        PetProfileStore.createAnimal(app,0,PetSpecies.TIGER,"female","Tigre QA");
        setAge(0,age);
        ActivityController<MainActivity> controller=openMain(0);
        MainActivity main=controller.get();
        try {
            assertEquals(PetSpecies.TIGER,main.petSpecies);
            assertEquals(stage,main.petStage());
            assertTrue("Production validation rejects no tiger asset: "+main.historyLog,main.invalidCharacterAssets.isEmpty());
            CharacterSprites.Pack character=CharacterSprites.forStage(PetSpecies.TIGER,stage);
            GameSprites.Pack game=GameSprites.forStage(PetSpecies.TIGER,stage);
            CareSprites.Pack care=CareSprites.forStage(PetSpecies.TIGER,stage);
            GardenSprites.Pack garden=GardenSprites.forStage(PetSpecies.TIGER,stage);

            // Every direction uses the tiger of this age, including the real run loader.
            for(MainActivity.TravelDirection direction:MainActivity.TravelDirection.values()) {
                assertTrue(main.setPetDrawableSafely(stage,character.idle(direction)));
                renderedFrame(main,character.idle(direction),0);
                main.travelDirection=direction;
                MainActivity.WalkMode mode=direction==MainActivity.TravelDirection.UP?MainActivity.WalkMode.BACK:
                        direction==MainActivity.TravelDirection.DOWN?MainActivity.WalkMode.FRONT:MainActivity.WalkMode.SIDE;
                for(boolean running:new boolean[]{false,true}) {
                    main.games.state=running?LivingRoomGames.RUN_TO_TOY:LivingRoomGames.NONE;
                    int res=running?game.run(direction):character.walk(direction);
                    assertTrue(main.loadWalkFrames(stage,mode,res));
                    assertEquals(6,main.currentWalkFrames.length);
                    for(int frame=0;frame<6;frame++) {
                        main.walkFrameIndex=frame;
                        main.showWalkFrame(mode);
                        renderedFrame(main,res,frame);
                    }
                }
            }
            main.games.cancel();
            main.walking=false;

            for(MainActivity.ActionAnim action:new MainActivity.ActionAnim[]{MainActivity.ActionAnim.EAT,MainActivity.ActionAnim.JUMP,MainActivity.ActionAnim.SLEEP}) {
                main.startActionAnimation(action,3000L);
                assertTrue(main.loadActionFrames(action));
                int count=action==MainActivity.ActionAnim.JUMP?5:3;
                assertEquals(count,main.actionFrames.length);
                for(int frame=0;frame<count;frame++) {
                    main.actionFrameIndex=frame;
                    main.actionFrameAt=0;
                    main.showActionAnimationFrame(System.currentTimeMillis());
                    renderedFrame(main,main.actionResource(action),frame);
                }
            }
            main.actionAnim=MainActivity.ActionAnim.NONE;
            main.releaseActionFrames();
            assertTrue(main.loadFaceMoodFrames());
            assertEquals(12,main.faceMoodFrames.length);
            for(int mood=0;mood<12;mood++) {
                main.activeFaceMood=mood;
                main.faceMoodUntil=System.currentTimeMillis()+4000L;
                main.ensurePetImage();
                renderedFrame(main,character.moods,mood);
            }
            main.activeFaceMood=-1;
            main.faceMoodUntil=0;

            // Use actual catalog entries so the item-to-animation routing is exercised.
            main.room="bain";
            String[] ids={"groom","soap","comb","towel"};
            int[] resources={care.groomFoam,care.soap,care.comb,care.towel};
            for(int i=0;i<ids.length;i++) {
                main.clean=30f;
                main.objects.use(main.objects.findById(ids[i],"bain"));
                assertEquals(resources[i],main.specialPoseRes);
                assertTrue("Care improves cleanliness",main.clean>30f);
                renderedFrame(main,resources[i],0);
            }
            main.clearSpecialPose();
            main.room="cuisine";
            ObjectSystem.Item bottle=main.objects.findById("bottle","cuisine");
            assertEquals(stage==MainActivity.PetStage.CUB,main.objects.allowed(bottle));
            if(stage==MainActivity.PetStage.CUB) {
                main.objects.use(bottle);
                renderedFrame(main,CareSprites.bottle(PetSpecies.TIGER,stage),0);
            } else assertEquals(0,CareSprites.bottle(PetSpecies.TIGER,stage));
            main.hunger=30f;
            main.objects.use(main.objects.findById(stage==MainActivity.PetStage.CUB?"junior":"kibble","cuisine"));
            assertEquals(MainActivity.ActionAnim.EAT,main.actionAnim);
            assertTrue("Meal improves hunger",main.hunger>30f);
            main.showActionAnimationFrame(System.currentTimeMillis());
            renderedFrame(main,character.eat,main.actionFrameIndex);
            main.actionAnim=MainActivity.ActionAnim.NONE;

            // Fetch reaches the same running, carrying, return and reward states in both rooms.
            for(String room:new String[]{"salon","jardin"}) {
                main.room=room;
                for(String toy:new String[]{"tennis","yarn","mouse","plush"}) {
                    main.happy=30f;
                    main.objects.use(main.objects.findById(toy,room));
                    assertEquals(LivingRoomGames.THROW_READY,main.games.state);
                    main.games.launchToy(.5f);
                    if(main.games.toyFlight!=null)main.games.toyFlight.end();
                    assertEquals(LivingRoomGames.RUN_TO_TOY,main.games.state);
                    assertTrue(main.games.onPetArrived(System.currentTimeMillis()));
                    renderedFrame(main,game.fetch(toy),0);
                    main.games.beginReturn();
                    assertEquals(LivingRoomGames.RETURNING,main.games.state);
                    assertTrue(main.games.onPetArrived(System.currentTimeMillis()));
                    assertTrue("Fetch rewards the tiger",main.happy>30f);
                }
                main.objects.use(main.objects.findById("rope",room));
                assertTrue(main.games.onPetArrived(System.currentTimeMillis()));
                MotionEvent hold=MotionEvent.obtain(0L,0L,MotionEvent.ACTION_DOWN,0f,0f,0);
                assertTrue(main.games.handleRopeTouch(hold));
                hold.recycle();
                assertEquals(LivingRoomGames.ROPE_HOLD,main.games.state);
                renderedFrame(main,game.ropePlay,main.games.ropeFrameIndex);
                main.games.cancel();
            }
            assertTrue(main.games.showGamePose(game.fetchBall));
            renderedFrame(main,game.fetchBall,0);
            main.room="jardin";
            main.objects.use(main.objects.findById("scratch","jardin"));
            assertTrue(main.gardenGames.onPetArrived(System.currentTimeMillis()));
            assertEquals(garden.scratcherFrames,main.gardenGames.scratcherFrames.length);
            renderedFrame(main,garden.scratcherPlay,main.gardenGames.frameIndex);
            main.gardenGames.finish(true);
            main.room="salon";
            main.objects.use(main.objects.findById("bed","salon"));
            assertTrue(main.sleeping);
            main.showActionAnimationFrame(System.currentTimeMillis());
            renderedFrame(main,character.sleep,main.actionFrameIndex);
            assertTrue("No loader failed during the complete scenario: "+main.historyLog,main.invalidCharacterAssets.isEmpty());
            System.out.println("NATIVE_TIGER_ACTIONS_OK stage="+stage+" directions=4 moods=12 care=4 fetchRooms=2 rope=true scratcher=true sleep=true");
        } finally { closeMain(controller); }
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

    static void renderedFrame(MainActivity main,int resource,int frame) {
        String name=main.getResources().getResourceEntryName(resource);
        assertTrue("Correct species and age: "+name,name.startsWith("tiger_"+main.petStage().name().toLowerCase(java.util.Locale.ROOT)+"_"));
        assertEquals(main.petStage(),main.displayedPetStage);
        assertEquals(View.VISIBLE,main.petView.getVisibility());
        assertTrue(main.petView.getDrawable() instanceof BitmapDrawable);
        Bitmap shown=((BitmapDrawable)main.petView.getDrawable()).getBitmap();
        Bitmap strip=BitmapFactory.decodeResource(main.getResources(),resource);
        assertNotNull(name,strip);
        Bitmap expected=Bitmap.createBitmap(strip,frame*256,0,256,256);
        assertTrue("Actual renderer displays the intended frame: "+name+"/"+frame,shown.sameAs(expected));
        assertTrue(name+" retains alpha",shown.hasAlpha());
        assertEquals(name+" has transparent margin",0,Color.alpha(shown.getPixel(0,0)));
        Bitmap composite=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(composite);canvas.drawColor(Color.MAGENTA);canvas.drawBitmap(shown,0,0,null);
        assertEquals(Color.MAGENTA,composite.getPixel(0,0));
        int[] pixels=new int[256*256];composite.getPixels(pixels,0,256,0,0,256,256);
        int changed=0;for(int color:pixels)if(color!=Color.MAGENTA)changed++;
        assertTrue(name+" actually paints an animal",changed>100);
        composite.recycle();
        if(expected!=strip)expected.recycle();
        strip.recycle();
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
