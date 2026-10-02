package com.byw.monpetitleopard;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

final class KitchenWaterSystem {
    final MainActivity a;
    TextView bowlView;

    KitchenWaterSystem(MainActivity a){this.a=a;}

    void install(){
        bowlView=new TextView(a);
        bowlView.setText("🥣");
        bowlView.setTextSize(34);
        bowlView.setGravity(Gravity.CENTER);
        bowlView.setContentDescription("Gamelle d’eau");
        bowlView.setElevation(a.dp(3));
        bowlView.setVisibility(View.GONE);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.argb(90,255,255,255));
        bg.setCornerRadius(a.dp(28));
        bowlView.setBackground(bg);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(a.dp(58),a.dp(58));
        lp.gravity=Gravity.TOP|Gravity.LEFT;
        a.scene.addView(bowlView,lp);
        bowlView.setOnClickListener(v->{
            int amount=Math.round(a.waterBowl);
            a.toast("💧 Gamelle d’eau : "+amount+"%");
        });
        a.scene.post(this::refreshVisibility);
    }

    void fill(){
        a.waterBowl=40f;
        a.save();
        refreshVisibility();
        a.toast("💧 La gamelle d’eau est pleine.");
    }

    void refreshVisibility(){
        if(bowlView==null)return;
        boolean visible="cuisine".equals(a.room)&&a.waterBowl>.05f;
        bowlView.setVisibility(visible?View.VISIBLE:View.GONE);
        if(visible)position();
    }

    void position(){
        if(bowlView==null||a.scene==null)return;
        if(bowlView.getWidth()<=0){bowlView.post(this::position);return;}
        float[] r=a.imageRect();
        if(r[2]<=0||r[3]<=0)return;
        float centerX=r[0]+.82f*r[2];
        float feetY=r[1]+.92f*r[3];
        bowlView.setX(centerX-bowlView.getWidth()/2f);
        bowlView.setY(feetY-bowlView.getHeight());
    }
}
