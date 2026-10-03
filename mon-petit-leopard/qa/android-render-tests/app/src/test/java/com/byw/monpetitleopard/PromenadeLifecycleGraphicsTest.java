package com.byw.monpetitleopard;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class PromenadeLifecycleGraphicsTest {
 @Test public void leopardCub(){scenario(PetSpecies.LEOPARD,MainActivity.PetStage.CUB,10_000L);}
 @Test public void leopardTeen(){scenario(PetSpecies.LEOPARD,MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
 @Test public void leopardAdult(){scenario(PetSpecies.LEOPARD,MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
 @Test public void leopardOld(){scenario(PetSpecies.LEOPARD,MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}
 @Test public void wolfCub(){scenario(PetSpecies.WOLF,MainActivity.PetStage.CUB,10_000L);}
 @Test public void wolfTeen(){scenario(PetSpecies.WOLF,MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
 @Test public void wolfAdult(){scenario(PetSpecies.WOLF,MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
 @Test public void wolfOld(){scenario(PetSpecies.WOLF,MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}
 @Test public void tigerCub(){scenario(PetSpecies.TIGER,MainActivity.PetStage.CUB,10_000L);}
 @Test public void tigerTeen(){scenario(PetSpecies.TIGER,MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
 @Test public void tigerAdult(){scenario(PetSpecies.TIGER,MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
 @Test public void tigerOld(){scenario(PetSpecies.TIGER,MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}

 @Test public void lionMaleCub(){scenario(PetSpecies.LION,"male",MainActivity.PetStage.CUB,10_000L);}
 @Test public void lionMaleTeen(){scenario(PetSpecies.LION,"male",MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
 @Test public void lionMaleAdult(){scenario(PetSpecies.LION,"male",MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
 @Test public void lionMaleOld(){scenario(PetSpecies.LION,"male",MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}
 @Test public void lionFemaleCub(){scenario(PetSpecies.LION,"female",MainActivity.PetStage.CUB,10_000L);}
 @Test public void lionFemaleTeen(){scenario(PetSpecies.LION,"female",MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
 @Test public void lionFemaleAdult(){scenario(PetSpecies.LION,"female",MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
 @Test public void lionFemaleOld(){scenario(PetSpecies.LION,"female",MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}
 @Test public void foxCub(){scenario(PetSpecies.FOX,MainActivity.PetStage.CUB,10_000L);}
 @Test public void foxTeen(){scenario(PetSpecies.FOX,MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
 @Test public void foxAdult(){scenario(PetSpecies.FOX,MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
 @Test public void foxOld(){scenario(PetSpecies.FOX,MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}
 @Test public void bearCub(){scenario(PetSpecies.BEAR,MainActivity.PetStage.CUB,10_000L);}
 @Test public void bearTeen(){scenario(PetSpecies.BEAR,MainActivity.PetStage.TEEN,MainActivity.CUB+10_000L);}
 @Test public void bearAdult(){scenario(PetSpecies.BEAR,MainActivity.PetStage.ADULT,MainActivity.CUB+MainActivity.TEEN+10_000L);}
 @Test public void bearOld(){scenario(PetSpecies.BEAR,MainActivity.PetStage.OLD,MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT+10_000L);}

 void scenario(String species,MainActivity.PetStage stage,long age){scenario(species,"female",stage,age);}

 void scenario(String species,String sex,MainActivity.PetStage stage,long age){
  boolean expandedChecks=PetSpecies.LION.equals(species)||PetSpecies.FOX.equals(species)||PetSpecies.BEAR.equals(species);
  String capturePrefix=species+(PetSpecies.LION.equals(species)?"-"+sex:"")+"-"+stage.name().toLowerCase(Locale.ROOT);
  Context app=RuntimeEnvironment.getApplication();
  for(String prefs:new String[]{"pet_profiles_v079","pet","pet_0"})app.getSharedPreferences(prefs,Context.MODE_PRIVATE).edit().clear().commit();
  PetProfileStore.ensureMigrated(app);
  PetProfileStore.createAnimal(app,0,species,sex,"Alpha QA");
  app.getSharedPreferences("pet_0",Context.MODE_PRIVATE).edit().putLong("born",System.currentTimeMillis()-age).commit();
  Intent intent=new Intent(app,MainActivity.class).putExtra(PetProfileStore.EXTRA_SLOT,0);
  ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();
  MainActivity main=controller.get();
  settle(main);
  assertEquals(stage,main.petStage());
  assertEquals(sex,main.petSex);
  if(main.petView.getVisibility()!=View.VISIBLE){System.out.println("INITIAL_INVISIBLE species="+species+" stage="+stage+" displayed="+main.displayedPetStage+" invalid="+main.invalidCharacterAssets+" history="+main.historyLog+" idle="+main.getResources().getResourceEntryName(main.idleDownDrawable()));}
  assertEquals(View.VISIBLE,main.petView.getVisibility());
  assertTrue("Real animal must affect the initial native scene",animalPixels(main)>100);
  alphaIsPreserved(main,"initial");
  if(expandedChecks)AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);

  List<String> failures=new ArrayList<>();
  main.startPromenade(null);
  checkHidden(main,"departure",failures);
  controller.pause().stop();
  Intent promenadeIntent=Shadows.shadowOf(main).getNextStartedActivity();
  assertNotNull(promenadeIntent);
  assertEquals(PromenadeActivity.class.getName(),promenadeIntent.getComponent().getClassName());
  ActivityController<PromenadeActivity> tripController=Robolectric.buildActivity(PromenadeActivity.class,promenadeIntent).create().start().resume().visible();
  PromenadeActivity trip=tripController.get();
  settle(trip);
  assertEquals(PetSpecies.promenadeTokenRes(species,sex,age),trip.mapView.tokenRes);
  assertNotNull("Promenade portrait is decoded",trip.mapView.token);
  assertTrue("Promenade portrait preserves transparency",trip.mapView.token.hasAlpha());
  assertEquals(0,Color.alpha(trip.mapView.token.getPixel(0,0)));
  if(!PetSpecies.LEOPARD.equals(species))assertTrue(trip.getResources().getResourceEntryName(trip.mapView.tokenRes).startsWith(species+(PetSpecies.LION.equals(species)?"_"+sex:"")+"_"+stage.name().toLowerCase(Locale.ROOT)+"_"));
  if(expandedChecks)TigerIntegrationGraphicsTest.capture(trip,capturePrefix+"-promenade");
  assertTrue(main.promenadeActive());
  Button back=findButton(trip.getWindow().getDecorView(),"Retour au jardin");
  assertNotNull(back); assertTrue(back.performClick());
  tripController.pause().stop().destroy();
  controller.restart().start().resume().visible();
  settle(main);
  checkHidden(main,"early-return-resume",failures);
  main.updatePetPosition(); checkHidden(main,"position-update",failures);
  main.animateAuto(); checkHidden(main,"animation-tick",failures);
  main.root.requestLayout(); settle(main); checkHidden(main,"layout-pass",failures);
  if(expandedChecks){
   String[] rooms={"salon","cuisine","bain","jardin"};
   for(int roomIndex=0;roomIndex<rooms.length;roomIndex++){
    assertTrue(main.roomsBtn.performClick());
    AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();
    assertNotNull(menu);
    assertTrue(menu.getListView().performItemClick(null,roomIndex,roomIndex));
    menu.dismiss();
    assertEquals(rooms[roomIndex],main.room);
    settle(main);checkHidden(main,"room-"+rooms[roomIndex],failures);
   }
  }
  if(PetSpecies.FOX.equals(species)||PetSpecies.BEAR.equals(species))
      TigerIntegrationGraphicsTest.capture(main,capturePrefix+"-promenade-home-empty");

  // Activity recreation while the profile still records a running promenade.
  controller.pause().stop().destroy();
  controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();
  main=controller.get(); settle(main);
  checkHidden(main,"recreated-home",failures);

  // The saved start time is elapsed; normal ticker logic performs the return.
  main.sp.edit().putLong("promenadeStart",System.currentTimeMillis()-PromenadeActivity.DURATION_MS-1000L).commit();
  main.ticker.run(); main.animateAuto(); settle(main);
  assertFalse(main.promenadeActive());
  assertEquals(stage,main.petStage());
  assertEquals(sex,main.petSex);
  assertEquals("Animal returns after the trip",View.VISIBLE,main.petView.getVisibility());
  assertTrue("Returned animal must affect the native scene",animalPixels(main)>100);
  alphaIsPreserved(main,"returned");
  if(expandedChecks)AnimalActionGraphicsScenario.assertDisplayedCharacterFromOwnPack(main);
  if(PetSpecies.FOX.equals(species)||PetSpecies.BEAR.equals(species))
      TigerIntegrationGraphicsTest.capture(main,capturePrefix+"-promenade-home-returned");
  main.handler.removeCallbacksAndMessages(null);
  controller.pause().stop().destroy();
  assertTrue(species+" "+stage+": "+failures,failures.isEmpty());
  System.out.println("NATIVE_LIFECYCLE_OK species="+species+" sex="+sex+" stage="+stage+" checks="+(expandedChecks?10:6)+" drawable-alpha=true");
 }

 static void settle(Activity activity){
  View decor=activity.getWindow().getDecorView();
  decor.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(891,View.MeasureSpec.EXACTLY));
  decor.layout(0,0,411,891);
  Shadows.shadowOf(Looper.getMainLooper()).idle();
 }
 static Button findButton(View view,String label){
  if(view instanceof Button&&label.contentEquals(((Button)view).getText()))return (Button)view;
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button result=findButton(group.getChildAt(i),label);if(result!=null)return result;}}
  return null;
 }
 static int animalPixels(MainActivity main){
  int width=main.scene.getWidth(),height=main.scene.getHeight();
  assertTrue("Scene is laid out",width>0&&height>0);
  assertTrue("Animal is laid out",main.petView.getWidth()>0&&main.petView.getHeight()>0);
  Bitmap actual=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888); main.scene.draw(new Canvas(actual));
  int visibility=main.petView.getVisibility(); main.petView.setVisibility(View.INVISIBLE);
  Bitmap empty=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888); main.scene.draw(new Canvas(empty));
  main.petView.setVisibility(visibility);
  int[] a=new int[width*height],b=new int[width*height];actual.getPixels(a,0,width,0,0,width,height);empty.getPixels(b,0,width,0,0,width,height);
  int changed=0;for(int i=0;i<a.length;i++)if(a[i]!=b[i])changed++;
  actual.recycle();empty.recycle();return changed;
 }
 static void checkHidden(MainActivity main,String event,List<String> failures){
  assertTrue("Trip remains active at "+event,main.promenadeActive());
  int affected=animalPixels(main);
  if(main.petView.getVisibility()!=View.INVISIBLE||affected!=0)failures.add(event+" visibility="+main.petView.getVisibility()+" native-animal-pixels="+affected);
 }
 static void alphaIsPreserved(MainActivity main,String event){
  assertTrue(main.petView.getDrawable() instanceof BitmapDrawable);
  Bitmap b=((BitmapDrawable)main.petView.getDrawable()).getBitmap();
  assertTrue("Alpha channel at "+event,b.hasAlpha());
  assertEquals(0,Color.alpha(b.getPixel(0,0)));
  assertEquals(0,Color.alpha(b.getPixel(b.getWidth()-1,0)));
  assertEquals(0,Color.alpha(b.getPixel(0,b.getHeight()-1)));
  assertEquals(0,Color.alpha(b.getPixel(b.getWidth()-1,b.getHeight()-1)));
  // Android's native Canvas composites the sprite onto two real opaque colors.
  for(int background:new int[]{Color.rgb(22,29,42),Color.rgb(11,119,139)}){
   Bitmap output=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(output);canvas.drawColor(background);canvas.drawBitmap(b,0,0,null);
   assertEquals("Transparent corner composites at "+event,background,output.getPixel(0,0));output.recycle();
  }
 }
}
