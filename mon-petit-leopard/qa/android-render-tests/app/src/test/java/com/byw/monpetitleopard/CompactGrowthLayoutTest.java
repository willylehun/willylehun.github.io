package com.byw.monpetitleopard;

import static org.junit.Assert.*;
import android.content.*;
import android.graphics.Rect;
import android.os.Looper;
import android.view.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class CompactGrowthLayoutTest {
 @Test @Config(sdk=34, qualifiers="w360dp-h640dp-mdpi") public void compact360x640(){check(360,640);}
 @Test @Config(sdk=34, qualifiers="w320dp-h568dp-mdpi") public void compact320x568(){check(320,568);}
 @Test @Config(sdk=34, qualifiers="w320dp-h480dp-mdpi") public void compact320x480(){check(320,480);}
 void check(int width,int height){
  Context app=RuntimeEnvironment.getApplication();
  for(String prefs:new String[]{"pet_profiles_v079","pet","pet_0"})app.getSharedPreferences(prefs,Context.MODE_PRIVATE).edit().clear().commit();
  PetProfileStore.ensureMigrated(app);PetProfileStore.createAnimal(app,0,PetSpecies.WOLF,"female","QA");
  Intent intent=new Intent(app,MainActivity.class).putExtra(PetProfileStore.EXTRA_SLOT,0);
  ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,intent).create().start().resume().visible();
  MainActivity main=controller.get();View decor=main.getWindow().getDecorView();
  for(int i=0;i<3;i++){decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));decor.layout(0,0,width,height);Shadows.shadowOf(Looper.getMainLooper()).idle();}
  GrowthButtonGraphicsTest.capture(main,"compact-"+width+"x"+height);
  if(width==320&&height==480){
   System.out.println("COMPACT_LAYOUT root="+main.root.getHeight()+" scene="+main.scene.getHeight()+" bottom="+main.bottomBar.getMeasuredHeight()+" footer="+main.footerSpace.getMeasuredHeight());
   for(int i=0;i<main.root.getChildCount();i++){View child=main.root.getChildAt(i);System.out.println("COMPACT_CHILD "+i+" "+child.getClass().getSimpleName()+" h="+child.getHeight()+" measured="+child.getMeasuredHeight()+" requested="+child.getLayoutParams().height);}
  }
  for(View button:new View[]{main.devGrowthBtn,main.roomsBtn,main.objectsBtn,main.actionsBtn}){
   Rect visible=new Rect();String label=((android.widget.Button)button).getText().toString();
   assertTrue(label+" remains on screen at "+width+"x"+height,button.getGlobalVisibleRect(visible));
   assertEquals(label+" is fully visible at "+width+"x"+height,button.getHeight(),visible.height());
   assertTrue(label+" is reachable below header",visible.top>=0&&visible.bottom<=height);
  }
  assertTrue(main.scene.getHeight()>100);assertTrue(main.devGrowthBtn.performClick());assertEquals(MainActivity.PetStage.TEEN,main.petStage());
  System.out.println("NATIVE_COMPACT_UI_OK "+width+"x"+height+" scene="+main.scene.getHeight()+" controls=4");
  main.handler.removeCallbacksAndMessages(null);controller.pause().stop().destroy();
 }
}
