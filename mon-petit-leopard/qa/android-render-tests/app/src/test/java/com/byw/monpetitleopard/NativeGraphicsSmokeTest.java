package com.byw.monpetitleopard;
import static org.junit.Assert.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeGraphicsSmokeTest {
 @Test public void decodesAndCompositesRealSprite() {
  Bitmap image=BitmapFactory.decodeResource(RuntimeEnvironment.getApplication().getResources(),R.drawable.wolf_cub_idle_down);
  assertNotNull(image); assertTrue(image.hasAlpha()); assertEquals(0,Color.alpha(image.getPixel(0,0)));
  Bitmap screen=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
  Canvas canvas=new Canvas(screen); canvas.drawColor(Color.MAGENTA);
  ImageView view=new ImageView(RuntimeEnvironment.getApplication()); view.setImageBitmap(image); view.layout(0,0,256,256); view.draw(canvas);
  assertEquals(Color.MAGENTA,screen.getPixel(0,0));
  int visible=0; for(int y=0;y<256;y++)for(int x=0;x<256;x++)if(screen.getPixel(x,y)!=Color.MAGENTA)visible++;
  assertTrue(visible>1000); System.out.println("NATIVE_GRAPHICS_OK visible="+visible+" config="+image.getConfig()+" alpha="+image.hasAlpha());
 }
}
