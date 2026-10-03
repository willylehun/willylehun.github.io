package com.byw.monpetitleopard;

import static org.junit.Assert.*;
import android.app.Activity;
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

 void scenario(String species,MainActivity.PetStage stage,long age){
  Context app=RuntimeEnvironment.getApplication();
  for(String prefs:new String[]{"pet_profiles_v079","pet","pet_0"})app.getSharedPreferences(prefs,Context.MODE_PRIVATE).edit().clear().commit();
  PetProfileStore.ensureMigrated(app);
  PetProfileStore.createAnimal(app,0,species,"female","Alpha QA");
  app.getSharedPreferences("pet_0",Context.MODE_PRIVATE).edit().putLong("born",System.currentTimeMillis()-age).commit();
  Intent intent=new Intent(app,MainActivity.class).putExtra(PetProfileStore.EXTRA_SLOT,0);
  ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();
  MainActivity main=controller.get();
  settle(main);
  assertEquals(stage,main.petStage());
  if(main.petView.getVisibility()!=View.VISIBLE){System.out.println("INITIAL_INVISIBLE species="+species+" stage="+stage+" displayed="+main.displayedPetStage+" invalid="+main.invalidCharacterAssets+" history="+main.historyLog+" idle="+main.getResources().getResourceEntryName(main.idleDownDrawable()));}
  assertEquals(View.VISIBLE,main.petView.getVisibility());
  assertTrue("Real animal must affect the initial native scene",animalPixels(main)>100);
  alphaIsPreserved(main,"initial");

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
  assertEquals("Animal returns after the trip",View.VISIBLE,main.petView.getVisibility());
  assertTrue("Returned animal must affect the native scene",animalPixels(main)>100);
  alphaIsPreserved(main,"returned");
  main.handler.removeCallbacksAndMessages(null);
  controller.pause().stop().destroy();
  assertTrue(species+" "+stage+": "+failures,failures.isEmpty());
  System.out.println("NATIVE_LIFECYCLE_OK species="+species+" stage="+stage+" checks=6 drawable-alpha=true");
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
