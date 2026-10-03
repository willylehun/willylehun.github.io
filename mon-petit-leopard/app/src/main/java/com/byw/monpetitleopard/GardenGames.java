package com.byw.monpetitleopard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

final class GardenGames {
    static final int NONE=0,APPROACH=1,PLAYING=2;

    final MainActivity a;
    ImageView scratcherView;
    ObjectSystem.Item activeItem;
    int state=NONE;
    Bitmap scratcherStrip;
    Bitmap[] scratcherFrames;
    int frameIndex=0;
    long frameAt=0,playUntil=0;

    GardenGames(MainActivity a){this.a=a;}

    void install(){
        scratcherView=new ImageView(a);
        scratcherView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        scratcherView.setAdjustViewBounds(false);
        scratcherView.setBackground(null);
        scratcherView.setImageResource(R.drawable.garden_scratcher);
        scratcherView.setElevation(a.dp(2));
        scratcherView.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(a.dp(94),a.dp(94));
        lp.gravity=Gravity.TOP|Gravity.LEFT;
        a.scene.addView(scratcherView,lp);
        scratcherView.setOnClickListener(v->{
            if("jardin".equals(a.room)){
                ObjectSystem.Item item=a.objects.findById("scratch","jardin");
                if(item!=null)startScratcher(item);
            }
        });
        a.scene.post(this::refreshVisibility);
    }

    void refreshVisibility(){
        if(scratcherView==null)return;
        scratcherView.setVisibility("jardin".equals(a.room)?View.VISIBLE:View.GONE);
        if("jardin".equals(a.room))positionScratcher();
    }

    void positionScratcher(){
        if(scratcherView==null||a.scene==null)return;
        if(scratcherView.getWidth()<=0){
            scratcherView.post(this::positionScratcher);
            return;
        }
        float[] r=a.imageRect();
        if(r[2]<=0||r[3]<=0)return;
        float centerX=r[0]+.76f*r[2];
        float feetY=r[1]+.60f*r[3];
        scratcherView.setX(centerX-scratcherView.getWidth()/2f);
        scratcherView.setY(feetY-scratcherView.getHeight());
    }

    void startScratcher(ObjectSystem.Item item){
        if(!"jardin".equals(a.room)){
            a.toast("Le griffoir se trouve dans le jardin.");
            return;
        }
        if(a.games!=null)a.games.cancel();
        cancelAnimationOnly();
        preparePet();
        activeItem=item;
        state=APPROACH;
        scratcherView.setVisibility(View.VISIBLE);
        positionScratcher();

        float[][] nodes=a.roomNodes();
        int best=0;
        float scoreBest=Float.MAX_VALUE;
        for(int i=0;i<nodes.length;i++){
            float dx=nodes[i][0]-.66f,dy=nodes[i][1]-.58f;
            float score=dx*dx+dy*dy;
            if(score<scoreBest){scoreBest=score;best=i;}
        }
        movePetToNode(best);
        a.nextWalkAt=Long.MAX_VALUE;
        a.toast(a.pet+" va vers son griffoir.");
    }

    void preparePet(){
        a.wakeForAction();
        a.activeFaceMood=-1;
        a.pendingFaceMood=-1;
        a.faceMoodUntil=0;
        a.moodApproach=false;
        a.moodExitUp=false;
        a.directionalIdleUntil=0;
        a.manualUntil=0;
        a.walking=false;
        a.clearSpecialPose();
        if(a.actionAnim!=MainActivity.ActionAnim.NONE){
            a.releaseActionFrames();
            a.actionAnim=MainActivity.ActionAnim.NONE;
        }
        a.currentPetRes=0;
    }

    void movePetToNode(int node){
        float[][] nodes=a.roomNodes();
        node=Math.max(0,Math.min(node,nodes.length-1));
        a.targetNodeIndex=node;
        a.targetNX=nodes[node][0];
        a.targetNY=nodes[node][1];
        a.updateTravelDirection(a.targetNX-a.petNX,a.targetNY-a.petNY);
        a.walking=true;
        a.walkStartedAt=System.currentTimeMillis();
        a.walkFrameIndex=0;
        a.currentPetRes=0;
    }

    boolean onPetArrived(long now){
        if(state!=APPROACH)return false;
        a.walking=false;
        state=PLAYING;
        scratcherView.setVisibility(View.VISIBLE);
        positionScratcher();
        if(!loadFrames()){
            finish(false);
            return true;
        }
        playUntil=now+4200L;
        frameAt=0;
        frameIndex=0;
        showFrame(now);
        a.toast("🐾 "+a.pet+" fait ses griffes.");
        return true;
    }

    boolean beforeAnimate(long now){
        if(state!=PLAYING)return false;
        showFrame(now);
        if(now>=playUntil){
            finish(true);
        }
        return true;
    }

    boolean loadFrames(){
        GardenSprites.Pack pack=GardenSprites.forStage(a.petSpecies,a.petStage());
        int res=pack.scratcherPlay;
        int count=pack.scratcherFrames;
        int frame=GardenSprites.FRAME_SIZE;
        if(res==0||a.invalidCharacterAssets.contains(res)){
            a.showAssetErrorOnce();
            return false;
        }
        Bitmap strip=null;
        try{strip=BitmapFactory.decodeResource(a.getResources(),res);}catch(Throwable ignored){}
        if(strip==null||strip.getWidth()!=frame*count||strip.getHeight()!=frame){
            a.markCharacterAssetInvalid(res,"griffoir","strip "+(frame*count)+"x"+frame+" attendu");
            a.showAssetErrorOnce();
            return false;
        }
        scratcherStrip=strip;
        scratcherFrames=new Bitmap[count];
        try{
            for(int i=0;i<count;i++)
                scratcherFrames[i]=Bitmap.createBitmap(strip,i*frame,0,frame,frame);
        }catch(Throwable err){
            releaseFrames();
            a.markCharacterAssetInvalid(res,"griffoir","découpage impossible");
            return false;
        }
        return true;
    }

    void showFrame(long now){
        if(scratcherFrames==null||scratcherFrames.length==0)return;
        int count=scratcherFrames.length;
        if(frameAt==0||now>=frameAt){
            frameIndex=(frameIndex+1)%count;
            frameAt=now+260L;
        }
        a.petView.setImageBitmap(scratcherFrames[frameIndex]);
        a.displayedPetStage=a.petStage();
        a.petView.setVisibility(View.VISIBLE);
        a.currentPetRes=0;
        a.updatePetPosition();
        a.petView.setRotation(count==1?1.6f*(float)Math.sin(now/140.0):0f);
    }

    void finish(boolean reward){
        a.walking=false;
        a.petView.setRotation(0f);
        state=NONE;
        if(reward && activeItem!=null){
            a.applyItemEffects(activeItem,1f);
            a.skillObedience=a.clamp(a.skillObedience+2.5f);
            a.skillCare=a.clamp(a.skillCare+1f);
            a.addHistory("Griffoir utilisé dans le jardin.");
        }
        activeItem=null;
        releaseFrames();
        a.idleDirection=MainActivity.TravelDirection.DOWN;
        a.travelDirection=MainActivity.TravelDirection.DOWN;
        a.walkMode=MainActivity.WalkMode.FRONT;
        a.currentPetRes=0;
        a.nextWalkAt=System.currentTimeMillis()+3500L;
        refreshVisibility();
        a.ensurePetImage();
        a.updatePetPosition();
        a.save();
        a.refresh();
    }

    void cancelAnimationOnly(){
        if(a.petView!=null)a.petView.setRotation(0f);
        state=NONE;
        activeItem=null;
        releaseFrames();
    }

    void cancel(){
        cancelAnimationOnly();
        refreshVisibility();
    }

    void releaseFrames(){
        scratcherStrip=null;
        scratcherFrames=null;
        frameIndex=0;
        frameAt=0;
        playUntil=0;
    }
}
