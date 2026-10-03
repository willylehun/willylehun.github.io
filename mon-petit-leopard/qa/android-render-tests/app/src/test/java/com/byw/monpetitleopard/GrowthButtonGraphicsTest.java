package com.byw.monpetitleopard;

import static org.junit.Assert.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.*;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class GrowthButtonGraphicsTest {
 @Test public void leopardGrowthButtonAndActions(){growth(PetSpecies.LEOPARD,false);}
 @Test public void wolfGrowthButtonAndActions(){growth(PetSpecies.WOLF,false);}
 @Test public void tigerGrowthButtonAndActions(){growth(PetSpecies.TIGER,false);}
 @Test public void leopardGrowthDuringPromenade(){growth(PetSpecies.LEOPARD,true);}
 @Test public void wolfGrowthDuringPromenade(){growth(PetSpecies.WOLF,true);}
 @Test public void tigerGrowthDuringPromenade(){growth(PetSpecies.TIGER,true);}

 @Test public void lionMaleGrowthButtonAndActions(){growth(PetSpecies.LION,"male",false);}
 @Test public void lionFemaleGrowthButtonAndActions(){growth(PetSpecies.LION,"female",false);}
 @Test public void lionMaleGrowthDuringPromenade(){growth(PetSpecies.LION,"male",true);}
 @Test public void lionFemaleGrowthDuringPromenade(){growth(PetSpecies.LION,"female",true);}

 void growth(String species,boolean trip){growth(species,"female",trip);}

 void growth(String species,String chosenSex,boolean trip){
  Context app=RuntimeEnvironment.getApplication();
  for(String prefs:new String[]{"pet_profiles_v079","pet","pet_0"})app.getSharedPreferences(prefs,Context.MODE_PRIVATE).edit().clear().commit();
  PetProfileStore.ensureMigrated(app);PetProfileStore.createAnimal(app,0,species,chosenSex,"Croissance QA");
  Intent intent=new Intent(app,MainActivity.class).putExtra(PetProfileStore.EXTRA_SLOT,0);
  ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();
  MainActivity main=controller.get();PromenadeLifecycleGraphicsTest.settle(main);
  String capturePrefix=species+(PetSpecies.LION.equals(species)?"-"+chosenSex:"");
  assertEquals(chosenSex,main.petSex);
  assertNotNull(main.devGrowthBtn);assertTrue(main.devGrowthBtn.isShown());assertTrue(main.devGrowthBtn.isEnabled());
  assertEquals(MainActivity.PetStage.CUB,main.petStage());
  if(trip){main.startPromenade(null);assertTrue(main.promenadeActive());assertEquals(View.INVISIBLE,main.petView.getVisibility());}
  else capture(main,capturePrefix+"-cub");
  MainActivity.PetStage[] stages={MainActivity.PetStage.TEEN,MainActivity.PetStage.ADULT,MainActivity.PetStage.OLD};
  for(int i=0;i<stages.length;i++){
   long sleepDeadline=0;
   if(!trip){
    if(i==0){main.startSpecialPose(CareSprites.forStage(species,chosenSex,main.petStage()).soap,6000L);assertNotEquals(0,main.specialPoseRes);}
    if(i==1){main.callLeopard();assertTrue(main.callingToForeground);assertTrue(main.walking);}
    if(i==2){main.beginAutoSleep();assertTrue(main.sleeping);sleepDeadline=main.sleepEndAt;assertEquals(MainActivity.ActionAnim.SLEEP,main.actionAnim);}
   }
   String sex=main.petSex;int generation=main.generation,stars=main.stars;
   float[] needs={main.hunger,main.thirst,main.clean,main.affection,main.happy,main.energy};
   float[] skills={main.skillClean,main.skillObedience,main.skillCare};
   assertTrue(main.devGrowthBtn.isEnabled());assertTrue(main.devGrowthBtn.performClick());
   assertEquals(stages[i],main.petStage());assertEquals(main.born,main.sp.getLong("born",-1L));
   assertEquals(sex,main.petSex);assertEquals(species,main.petSpecies);assertEquals(generation,main.generation);assertEquals(stars,main.stars);
   assertArrayEquals(needs,new float[]{main.hunger,main.thirst,main.clean,main.affection,main.happy,main.energy},0f);
   assertArrayEquals(skills,new float[]{main.skillClean,main.skillObedience,main.skillCare},0f);
   assertFalse(main.walking);assertFalse(main.callingToForeground);assertEquals(0,main.specialPoseRes);
   assertTrue(main.subTitle.getText().toString().contains(PetSpecies.stageLabel(species,chosenSex,stages[i])));
   PromenadeLifecycleGraphicsTest.settle(main);
   if(trip){
    assertTrue(main.promenadeActive());assertEquals(View.INVISIBLE,main.petView.getVisibility());assertEquals(0,PromenadeLifecycleGraphicsTest.animalPixels(main));
   }else{
    if(i==2){assertTrue(main.sleeping);assertEquals(sleepDeadline,main.sleepEndAt);main.animateAuto();assertEquals(MainActivity.ActionAnim.SLEEP,main.actionAnim);assertEquals(sleepDeadline,main.sleepEndAt);}
    assertEquals(stages[i],main.displayedPetStage);assertEquals(View.VISIBLE,main.petView.getVisibility());
    PromenadeLifecycleGraphicsTest.alphaIsPreserved(main,"growth-"+stages[i]);
    assertTrue(PromenadeLifecycleGraphicsTest.animalPixels(main)>100);
    if(PetSpecies.LION.equals(species))AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
    capture(main,capturePrefix+"-"+stages[i].name().toLowerCase());
   }
  }
  assertFalse(main.devGrowthBtn.isEnabled());assertTrue(main.devGrowthBtn.getText().toString().contains("Âge maximum"));
  long oldBorn=main.born;main.devGrowthBtn.performClick();assertEquals(oldBorn,main.born);assertEquals(MainActivity.Stage.OLD,main.stage());
  if(trip){
   capture(main,capturePrefix+"-promenade-home-empty");
   main.sp.edit().putLong("promenadeStart",System.currentTimeMillis()-PromenadeActivity.DURATION_MS-1000L).commit();main.ticker.run();main.animateAuto();PromenadeLifecycleGraphicsTest.settle(main);
   assertEquals(View.VISIBLE,main.petView.getVisibility());assertEquals(MainActivity.PetStage.OLD,main.displayedPetStage);PromenadeLifecycleGraphicsTest.alphaIsPreserved(main,"grown-trip-return");
   if(PetSpecies.LION.equals(species))AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
  }
  main.handler.removeCallbacksAndMessages(null);controller.pause().stop().destroy();
  controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();main=controller.get();PromenadeLifecycleGraphicsTest.settle(main);
  assertEquals(MainActivity.PetStage.OLD,main.petStage());assertFalse(main.devGrowthBtn.isEnabled());assertEquals(oldBorn,main.born);
  assertEquals(species,main.petSpecies);assertEquals(species,PetProfileStore.species(main,0));
  assertEquals(chosenSex,main.petSex);assertEquals(chosenSex,PetProfileStore.sex(main,0));
  main.handler.removeCallbacksAndMessages(null);controller.pause().stop().destroy();
  System.out.println("NATIVE_DEV_GROWTH_OK species="+species+" sex="+chosenSex+" promenade="+trip+" clicks=3 old-disabled=true restart-persisted=true");
 }
 static void capture(MainActivity main,String name){
  try {
   View decor=main.getWindow().getDecorView();Bitmap screen=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);decor.draw(new Canvas(screen));
   File out=new File(System.getProperty("qa.captureDir","build/native-captures"));out.mkdirs();try(FileOutputStream stream=new FileOutputStream(new File(out,name+".png"))){screen.compress(Bitmap.CompressFormat.PNG,100,stream);}screen.recycle();
  }catch(IOException e){throw new AssertionError(e);}
 }
}
