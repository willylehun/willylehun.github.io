package com.byw.monpetitleopard;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

/** Shared real-object and native-render scenario for tiger, sexed lion, fox and bear packs. */
final class AnimalActionGraphicsScenario {
    static void run(Context app,String species,String sex,MainActivity.PetStage stage,long age) {
        PetProfileStore.createAnimal(app,0,species,sex,"Actions QA");
        app.getSharedPreferences(PetProfileStore.petPrefsName(0),Context.MODE_PRIVATE).edit()
                .putLong("born",System.currentTimeMillis()-age).commit();
        Intent intent=new Intent(app,MainActivity.class).putExtra(PetProfileStore.EXTRA_SLOT,0);
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();
        PromenadeLifecycleGraphicsTest.settle(controller.get());
        MainActivity main=controller.get();
        try {
            assertEquals(species,main.petSpecies);
            assertEquals(sex,main.petSex);
            assertEquals(stage,main.petStage());
            assertTrue("Production validation rejects no animal asset: "+main.historyLog,main.invalidCharacterAssets.isEmpty());
            CharacterSprites.Pack character=CharacterSprites.forStage(species,sex,stage);
            GameSprites.Pack game=GameSprites.forStage(species,sex,stage);
            CareSprites.Pack care=CareSprites.forStage(species,sex,stage);
            GardenSprites.Pack garden=GardenSprites.forStage(species,sex,stage);

            assertEquals(PetSpecies.LION.equals(species)?sex:"",character.sex);
            assertEquals(character.sex,game.sex);
            assertEquals(character.sex,care.sex);
            assertEquals(character.sex,garden.sex);
            TigerIntegrationGraphicsTest.capture(main,resourcePrefix(main).replace('_','-')+"actions");

            // Every direction uses this species, sex and age, including the real run loader.
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
                renderedFrame(main,CareSprites.bottle(species,sex,stage),0);
            } else assertEquals(0,CareSprites.bottle(species,sex,stage));
            main.hunger=30f;
            main.objects.use(main.objects.findById(stage==MainActivity.PetStage.CUB?"junior":"kibble","cuisine"));
            assertEquals(MainActivity.ActionAnim.EAT,main.actionAnim);
            assertTrue("Meal improves hunger",main.hunger>30f);
            main.showActionAnimationFrame(System.currentTimeMillis());
            renderedFrame(main,character.eat,main.actionFrameIndex);
            main.actionAnim=MainActivity.ActionAnim.NONE;

            // Fetch reaches the same running, carrying, return and reward states in both rooms.
            // The fixed name "Actions QA" likes tennis and dislikes plush.
            // A successful return must preserve those tastes, including negative joy.
            String[] fetchToys={"tennis","yarn","mouse","plush"};
            int[] preferences={PetPreferences.LIKE,PetPreferences.NEUTRAL,
                    PetPreferences.NEUTRAL,PetPreferences.DISLIKE};
            float[] joyDeltas={28.8f,12f,14f,-7.5f};
            for(String room:new String[]{"salon","jardin"}) {
                main.room=room;
                for(int toyIndex=0;toyIndex<fetchToys.length;toyIndex++) {
                    String toy=fetchToys[toyIndex];
                    main.happy=30f;
                    main.skillCare=30f;
                    assertEquals("Known fixture taste for "+toy,preferences[toyIndex],PetPreferences.toy(main,toy));
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
                    assertEquals("Fetch completes its return",LivingRoomGames.NONE,main.games.state);
                    assertNull("Returned object is released",main.games.activeItem);
                    assertEquals("Fetch respects taste for "+toy,30f+joyDeltas[toyIndex],main.happy,.0001f);
                    assertEquals("Care progresses even for a disliked toy",30.6f,main.skillCare,.0001f);
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
            System.out.println("NATIVE_ANIMAL_ACTIONS_OK species="+species+" sex="+sex+" stage="+stage+" directions=4 moods=12 care=4 fetchRooms=2 rope=true scratcher=true sleep=true");
        } finally { TigerIntegrationGraphicsTest.closeMain(controller); }
    }

    static void renderedFrame(MainActivity main,int resource,int frame) {
        String name=main.getResources().getResourceEntryName(resource);
        assertTrue("Correct species, sex and age: "+name,name.startsWith(resourcePrefix(main)));
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


    static String resourcePrefix(MainActivity main) {
        return main.petSpecies+(PetSpecies.LION.equals(main.petSpecies)?"_"+main.petSex:"")+"_"
                +main.petStage().name().toLowerCase(Locale.ROOT)+"_";
    }

    /** Checks the current visual without changing the production renderer's state. */
    static void assertDisplayedCharacterFromOwnPack(MainActivity main) {
        assertEquals(main.petStage(),main.displayedPetStage);
        assertTrue(main.petView.getDrawable() instanceof BitmapDrawable);
        Bitmap shown=((BitmapDrawable)main.petView.getDrawable()).getBitmap();
        CharacterSprites.Pack pack=CharacterSprites.forStage(main.petSpecies,main.petSex,main.petStage());
        boolean found=false;
        for(int resource:pack.allResources()) {
            Bitmap strip=BitmapFactory.decodeResource(main.getResources(),resource);
            assertNotNull(strip);
            for(int frame=0;frame<strip.getWidth()/256;frame++) {
                Bitmap expected=Bitmap.createBitmap(strip,frame*256,0,256,256);
                found|=shown.sameAs(expected);
                if(expected!=strip)expected.recycle();
                if(found)break;
            }
            strip.recycle();
            if(found)break;
        }
        assertTrue("Displayed character belongs to "+resourcePrefix(main),found);
    }

    private AnimalActionGraphicsScenario(){}
}
