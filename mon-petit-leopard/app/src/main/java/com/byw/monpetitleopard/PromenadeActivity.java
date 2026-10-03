package com.byw.monpetitleopard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class PromenadeActivity extends Activity {
    static final long DURATION_MS=3L*60L*1000L;
    static final int MAP_W=1448, MAP_H=1086;

    android.content.SharedPreferences sp;
    int profileSlot=-1;
    PromenadeView mapView;
    TextView status;
    boolean internalReturn=false,resumeNeedsChooser=false;
    final Handler handler=new Handler(Looper.getMainLooper());

    final Runnable ticker=new Runnable(){
        @Override public void run(){
            updateProgress();
            handler.postDelayed(this,1000L);
        }
    };

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        profileSlot=getIntent().getIntExtra(PetProfileStore.EXTRA_SLOT,-1);
        if(!PetProfileStore.validSlot(profileSlot)||!PetProfileStore.exists(this,profileSlot)){
            finish();
            return;
        }
        sp=getSharedPreferences(PetProfileStore.petPrefsName(profileSlot),MODE_PRIVATE);
        build();
        updateProgress();
    }

    @Override protected void onResume(){
        super.onResume();
        if(resumeNeedsChooser){
            resumeNeedsChooser=false;
            Intent chooser=new Intent(this,PetChooserActivity.class);
            startActivity(chooser);
            finish();
            return;
        }
        internalReturn=false;
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override protected void onPause(){
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override protected void onStop(){
        super.onStop();
        if(!internalReturn&&!isFinishing())resumeNeedsChooser=true;
    }

    @Override public void onBackPressed(){
        internalReturn=true;
        super.onBackPressed();
    }

    void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(242,232,210));

        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10),dp(8),dp(8),dp(8));

        LinearLayout titles=new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title=new TextView(this);
        title.setText("🌿 Promenade");
        title.setTextSize(19);
        title.setTextColor(Color.rgb(55,47,34));
        title.setTypeface(null,1);
        titles.addView(title);

        status=new TextView(this);
        status.setTextSize(12);
        status.setTextColor(Color.rgb(85,74,55));
        titles.addView(status);
        bar.addView(titles,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        Button close=new Button(this);
        close.setAllCaps(false);
        close.setText("Retour au jardin");
        close.setTextSize(11);
        close.setOnClickListener(v->{internalReturn=true;finish();});
        bar.addView(close,new LinearLayout.LayoutParams(dp(128),dp(44)));
        root.addView(bar);

        TextView note=new TextView(this);
        note.setText("3 minutes réelles • la promenade continue même si tu quittes cet écran");
        note.setTextSize(10);
        note.setTextColor(Color.rgb(95,84,63));
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8),0,dp(8),dp(6));
        root.addView(note);

        mapView=new PromenadeView();
        root.addView(mapView,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        setContentView(root);
    }

    void updateProgress(){
        if(sp==null||mapView==null)return;
        long now=System.currentTimeMillis();
        long start=sp.getLong("promenadeStart",now);
        boolean active=sp.getBoolean("promenadeActive",false);
        if(start<=0){
            start=now;
            sp.edit().putLong("promenadeStart",start).putBoolean("promenadeActive",true).apply();
            active=true;
        }

        long elapsed=Math.max(0L,now-start);
        float progress=Math.max(0f,Math.min(1f,elapsed/(float)DURATION_MS));
        mapView.setProgress(progress);

        long remain=Math.max(0L,DURATION_MS-elapsed);
        long minutes=remain/60000L;
        long seconds=(remain/1000L)%60L;
        if(progress>=1f){
            status.setText("Retour à la maison • promenade terminée");
            if(active)sp.edit().putBoolean("promenadeActive",false).apply();
        }else{
            status.setText(String.format(Locale.FRANCE,"%02d:%02d restantes",minutes,seconds));
        }
    }

    int dp(int value){
        return Math.round(value*getResources().getDisplayMetrics().density);
    }

    final class PromenadeView extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final Bitmap map=BitmapFactory.decodeResource(getResources(),R.drawable.promenade_map);
        Bitmap token;
        int tokenRes=0;
        float progress=0f;

        final float[][] route={
            {160,905},{200,905},{250,860},{330,815},{385,775},
            {405,835},{455,900},{590,940},{735,900},{935,805},
            {1040,770},{1100,720},{1108,620},{1120,520},{1150,460},
            {1230,455},{1280,420},{1310,360},{1230,320},
            {1305,270},{1360,210},{1390,145},{1360,210},{1305,270},{1230,320},
            {1170,350},{1120,405},{1040,430},{950,420},{890,375},
            {898,310},{850,260},{780,250},{710,305},{620,330},
            {535,330},{490,270},{405,220},{310,270},{260,330},
            {220,370},{250,405},{330,430},{395,500},{430,575},
            {400,625},{315,650},{245,715},{260,760},{330,775},
            {385,775},{320,815},{245,860},{200,905},{160,905}
        };
        final float[] cumulative=new float[route.length];
        final float totalLength;

        PromenadeView(){
            super(PromenadeActivity.this);
            float total=0f;
            cumulative[0]=0f;
            for(int i=1;i<route.length;i++){
                float dx=route[i][0]-route[i-1][0];
                float dy=route[i][1]-route[i-1][1];
                total+=(float)Math.sqrt(dx*dx+dy*dy);
                cumulative[i]=total;
            }
            totalLength=total;
            setBackgroundColor(Color.rgb(37,49,30));
        }

        void setProgress(float value){
            progress=Math.max(0f,Math.min(1f,value));
            String species=PetProfileStore.species(PromenadeActivity.this,profileSlot);
            long age=Math.max(0L,System.currentTimeMillis()-sp.getLong("born",System.currentTimeMillis()));
            String sex=PetProfileStore.sex(PromenadeActivity.this,profileSlot);
            int nextToken=PetSpecies.promenadeTokenRes(species,sex,age);
            if(tokenRes!=nextToken){
                token=BitmapFactory.decodeResource(getResources(),nextToken);
                tokenRes=nextToken;
            }
            invalidate();
        }

        float[] routePosition(float p){
            if(p<=0f)return new float[]{route[0][0],route[0][1]};
            if(p>=1f)return new float[]{route[route.length-1][0],route[route.length-1][1]};
            float target=p*totalLength;
            int index=1;
            while(index<cumulative.length && cumulative[index]<target)index++;
            index=Math.min(index,route.length-1);
            float before=cumulative[index-1];
            float seg=Math.max(.001f,cumulative[index]-before);
            float t=(target-before)/seg;
            return new float[]{
                route[index-1][0]+(route[index][0]-route[index-1][0])*t,
                route[index-1][1]+(route[index][1]-route[index-1][1])*t
            };
        }

        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            if(map==null||token==null)return;
            float scale=Math.min(getWidth()/(float)MAP_W,getHeight()/(float)MAP_H);
            float drawW=MAP_W*scale, drawH=MAP_H*scale;
            float left=(getWidth()-drawW)/2f;
            float top=(getHeight()-drawH)/2f;
            RectF dst=new RectF(left,top,left+drawW,top+drawH);
            canvas.drawBitmap(map,null,dst,paint);

            float[] pos=routePosition(progress);
            float cx=left+(pos[0]/MAP_W)*drawW;
            float cy=top+(pos[1]/MAP_H)*drawH;
            float size=Math.max(24f,drawW*(58f/MAP_W));
            RectF tokenDst=new RectF(cx-size/2f,cy-size/2f,cx+size/2f,cy+size/2f);
            canvas.drawBitmap(token,null,tokenDst,paint);
        }
    }
}
