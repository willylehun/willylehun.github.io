package com.byw.monpetitleopard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Jeux interactifs du salon :
 * - "Va chercher" avec vrais visuels PNG/WebP et lancer en cloche.
 * - Tir à la corde animé sur 5 frames par âge.
 */
final class LivingRoomGames {
    static final int NONE=0;
    static final int THROW_READY=1;
    static final int TOY_FLYING=2;
    static final int RUN_TO_TOY=3;
    static final int PLAYING=4;
    static final int RETURNING=5;
    static final int ROPE_APPROACH=6;
    static final int ROPE_READY=7;
    static final int ROPE_HOLD=8;

    static final int ROPE_FRAME_COUNT=5;
    static final long ROPE_FRAME_MS=115L;
    static final long ROPE_SOLO_MS=2200L;

    final MainActivity a;
    ImageView toyView;
    ObjectSystem.Item activeItem;
    int state=NONE;
    int landingNode=-1;
    long playUntil=0;
    long ropeHoldStartedAt=0;
    float downRawX,downRawY,startViewX,startViewY;
    float toyNX=.50f,toyNY=.95f;

    ValueAnimator flightAnimator;

    Bitmap ropeStrip;
    Bitmap[] ropeFrames;
    int ropeFrameIndex=0;
    long ropeFrameAt=0;
    MainActivity.PetStage loadedRopeStage=null;

    LivingRoomGames(MainActivity a){this.a=a;}

    void install(){
        toyView=new ImageView(a);
        toyView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        toyView.setAdjustViewBounds(false);
        toyView.setBackground(null);
        toyView.setPadding(0,0,0,0);
        toyView.setElevation(a.dp(10));
        toyView.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(a.dp(64),a.dp(64));
        lp.gravity=Gravity.TOP|Gravity.LEFT;
        a.scene.addView(toyView,lp);
        toyView.setOnTouchListener((v,e)->handleTouch(e));
    }

    int toyDrawable(String id){
        if("tennis".equals(id))return R.drawable.toy_tennis;
        if("yarn".equals(id))return R.drawable.toy_yarn;
        if("mouse".equals(id))return R.drawable.toy_mouse;
        if("plush".equals(id))return R.drawable.toy_plush;
        if("rope".equals(id))return R.drawable.toy_rope;
        return 0;
    }

    void applyToyVisual(ObjectSystem.Item item){
        int res=item==null?0:toyDrawable(item.id);
        toyView.setImageResource(res);
        toyView.setContentDescription(item==null?null:item.name);
        resizeToyForScene();
    }

    void resizeToyForScene(){
        if(toyView==null)return;
        int petSize=a.petView!=null?a.petView.getWidth():0;
        if(petSize<=0)petSize=a.dp(110);
        float ratio=.34f;
        if(activeItem!=null){
            if("tennis".equals(activeItem.id))ratio=.30f;
            else if("yarn".equals(activeItem.id))ratio=.34f;
            else if("mouse".equals(activeItem.id))ratio=.36f;
            else if("plush".equals(activeItem.id))ratio=.40f;
            else if("rope".equals(activeItem.id))ratio=.58f;
        }
        int size=Math.round(petSize*ratio);
        size=Math.max(a.dp(42),Math.min(a.dp(88),size));
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)toyView.getLayoutParams();
        if(lp.width!=size||lp.height!=size){
            lp.width=size;lp.height=size;
            lp.gravity=Gravity.TOP|Gravity.LEFT;
            toyView.setLayoutParams(lp);
        }
    }

    boolean active(){return state!=NONE;}
    boolean fastRun(){return state==RUN_TO_TOY||state==RETURNING;}
    int walkFrameAdvance(){return fastRun()?2:1;}

    void releaseRopeFrames(){
        ropeFrames=null;
        ropeStrip=null;
        loadedRopeStage=null;
        ropeFrameIndex=0;
        ropeFrameAt=0;
    }

    void cancel(){
        if(flightAnimator!=null){
            flightAnimator.cancel();
            flightAnimator=null;
        }
        if(toyView!=null){
            toyView.animate().cancel();
            toyView.setVisibility(View.GONE);
            toyView.setRotation(0f);
            toyView.setImageDrawable(null);
        }
        releaseRopeFrames();
        activeItem=null;
        state=NONE;
        landingNode=-1;
        playUntil=0;
        ropeHoldStartedAt=0;
        if(a.actionAnim==MainActivity.ActionAnim.JUMP){
            a.releaseActionFrames();
            a.actionAnim=MainActivity.ActionAnim.NONE;
        }
        a.currentPetRes=0;
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
        if(a.actionAnim!=MainActivity.ActionAnim.NONE){
            a.releaseActionFrames();
            a.actionAnim=MainActivity.ActionAnim.NONE;
        }
        a.currentPetRes=0;
    }

    void startFetch(ObjectSystem.Item item){
        if(!"salon".equals(a.room)){
            a.toast("Le jeu « Va chercher » se joue dans le salon.");
            return;
        }
        cancel();
        preparePet();
        activeItem=item;
        state=THROW_READY;
        applyToyVisual(item);
        toyView.setVisibility(View.VISIBLE);
        int front=foregroundCenterNode();
        float[][] nodes=a.roomNodes();
        toyNX=nodes[front][0];
        toyNY=nodes[front][1];
        positionToy();
        a.nextWalkAt=Long.MAX_VALUE;
        a.toast("Saisis l’objet et lance-le vers le haut.");
    }

    void startRope(ObjectSystem.Item item){
        if(!"salon".equals(a.room)){
            a.toast("La corde se joue dans le salon.");
            return;
        }
        cancel();
        preparePet();
        activeItem=item;
        state=ROPE_APPROACH;
        applyToyVisual(item);
        toyView.setVisibility(View.VISIBLE);
        int front=foregroundCenterNode();
        float[][] nodes=a.roomNodes();
        toyNX=Math.min(.82f,nodes[front][0]+.17f);
        toyNY=nodes[front][1];
        positionToy();
        movePetToNode(front,false);
        a.toast("Le léopard arrive. Maintiens ensuite ton doigt sur la corde.");
    }

    int foregroundCenterNode(){
        float[][] nodes=a.roomNodes();
        float maxY=-1f;
        int best=0;
        float bestCenter=Float.MAX_VALUE;
        for(int i=0;i<nodes.length;i++){
            float y=nodes[i][1];
            float center=Math.abs(nodes[i][0]-.50f);
            if(y>maxY+.001f || (Math.abs(y-maxY)<=.001f && center<bestCenter)){
                maxY=y;bestCenter=center;best=i;
            }
        }
        return best;
    }

    int chooseLandingNode(float desiredX){
        float[][] nodes=a.roomNodes();
        float minY=1f;
        for(float[] n:nodes)minY=Math.min(minY,n[1]);
        int best=0;
        float scoreBest=Float.MAX_VALUE;
        for(int i=0;i<nodes.length;i++){
            float score=Math.abs(nodes[i][0]-desiredX)*1.5f + Math.abs(nodes[i][1]-minY)*5f;
            if(score<scoreBest){scoreBest=score;best=i;}
        }
        return best;
    }

    void movePetToNode(int node,boolean running){
        float[][] nodes=a.roomNodes();
        node=Math.max(0,Math.min(node,nodes.length-1));
        a.targetNodeIndex=node;
        a.targetNX=nodes[node][0];
        a.targetNY=nodes[node][1];
        float dx=a.targetNX-a.petNX;
        float dy=a.targetNY-a.petNY;
        a.updateTravelDirection(dx,dy);
        a.walking=true;
        a.walkStartedAt=System.currentTimeMillis();
        a.walkFrameIndex=0;
        a.currentPetRes=0;
    }

    boolean handleTouch(MotionEvent e){
        if(state==THROW_READY||state==TOY_FLYING)return handleThrowTouch(e);
        if(state==ROPE_READY||state==ROPE_HOLD)return handleRopeTouch(e);
        return true;
    }

    boolean handleThrowTouch(MotionEvent e){
        if(state==TOY_FLYING)return true;
        float rx=e.getRawX(),ry=e.getRawY();
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            downRawX=rx;downRawY=ry;
            startViewX=toyView.getX();startViewY=toyView.getY();
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE){
            float nx=startViewX+(rx-downRawX);
            float ny=startViewY+(ry-downRawY);
            float[] r=a.imageRect();
            float maxX=r[0]+r[2]-toyView.getWidth();
            float maxY=r[1]+r[3]-toyView.getHeight();
            toyView.setX(Math.max(r[0],Math.min(maxX,nx)));
            toyView.setY(Math.max(r[1],Math.min(maxY,ny)));
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){
            float dy=ry-downRawY;
            if(e.getAction()==MotionEvent.ACTION_UP && dy<-a.dp(48)){
                float[] r=a.imageRect();
                float cx=toyView.getX()+toyView.getWidth()/2f;
                float desiredX=a.clamp01((cx-r[0])/Math.max(1f,r[2]));
                launchToy(desiredX);
            }else{
                positionToy();
                a.toast("Fais un geste vers le haut pour lancer.");
            }
            return true;
        }
        return true;
    }

    void launchToy(float desiredX){
        state=TOY_FLYING;
        landingNode=chooseLandingNode(desiredX);
        float[][] nodes=a.roomNodes();
        toyNX=nodes[landingNode][0];
        toyNY=nodes[landingNode][1];

        final float sx=toyView.getX(),sy=toyView.getY();
        final float[] target=toyPixelPosition(toyNX,toyNY);
        final float distance=Math.abs(target[1]-sy);
        final float arc=Math.max(a.dp(92),distance*.55f+a.dp(36));

        if(flightAnimator!=null)flightAnimator.cancel();
        flightAnimator=ValueAnimator.ofFloat(0f,1f);
        flightAnimator.setDuration(720L);
        flightAnimator.setInterpolator(new LinearInterpolator());
        flightAnimator.addUpdateListener(anim->{
            float t=(float)anim.getAnimatedValue();
            float x=sx+(target[0]-sx)*t;
            float y=sy+(target[1]-sy)*t-4f*arc*t*(1f-t);
            toyView.setX(x);
            toyView.setY(y);
            toyView.setRotation(420f*t);
        });
        flightAnimator.addListener(new AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(Animator animation){
                if(state!=TOY_FLYING)return;
                toyView.setRotation(0f);
                toyView.setX(target[0]);
                toyView.setY(target[1]);
                state=RUN_TO_TOY;
                movePetToNode(landingNode,true);
                flightAnimator=null;
            }
        });
        flightAnimator.start();
    }

    boolean handleRopeTouch(MotionEvent e){
        if(state==ROPE_READY && e.getAction()==MotionEvent.ACTION_DOWN){
            state=ROPE_HOLD;
            ropeHoldStartedAt=System.currentTimeMillis();
            ropeFrameAt=0;
            ropeFrameIndex=0;
            a.releaseActionFrames();
            a.actionAnim=MainActivity.ActionAnim.NONE;
            showRopePose();
            positionToyNearPet();
            return true;
        }
        if(state==ROPE_HOLD && (e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)){
            long held=Math.max(0,System.currentTimeMillis()-ropeHoldStartedAt);
            float factor=Math.min(1f,held/1800f);
            applyRewards(factor);
            state=PLAYING;
            playUntil=System.currentTimeMillis()+ROPE_SOLO_MS;
            toyView.setVisibility(View.GONE);
            ropeFrameAt=0;
            ropeFrameIndex=0;
            showRopePose();
            return true;
        }
        return true;
    }

    /**
     * Appelé avant la logique automatique normale.
     * Retourne true quand le jeu doit geler les déplacements automatiques.
     */
    boolean beforeAnimate(long now){
        if(state==NONE||state==RUN_TO_TOY||state==RETURNING||state==ROPE_APPROACH)return false;

        if(state==PLAYING){
            if(activeItem!=null && "rope".equals(activeItem.kind)){
                showRopePose();
                if(now>=playUntil)finishRopePlay();
            }else{
                showFetchPose();
                if(now>=playUntil){
                    beginReturn();
                    return false;
                }
            }
            return true;
        }

        if(state==ROPE_HOLD){
            showRopePose();
            positionToyNearPet();
            return true;
        }

        if(state==THROW_READY||state==TOY_FLYING||state==ROPE_READY){
            a.walking=false;
            a.currentPetRes=0;
            a.ensurePetImage();
            a.updatePetPosition();
            return true;
        }
        return false;
    }

    boolean onPetArrived(long now){
        if(state==RUN_TO_TOY){
            a.walking=false;
            state=PLAYING;
            playUntil=now+1350L;
            toyView.setVisibility(View.GONE);
            showFetchPose();
            return true;
        }
        if(state==RETURNING){
            finishFetch();
            return true;
        }
        if(state==ROPE_APPROACH){
            a.walking=false;
            a.idleDirection=MainActivity.TravelDirection.DOWN;
            a.travelDirection=MainActivity.TravelDirection.DOWN;
            a.walkMode=MainActivity.WalkMode.FRONT;
            a.currentPetRes=0;
            state=ROPE_READY;
            a.ensurePetImage();
            positionToy();
            a.toast("Maintiens ton doigt sur la corde pour jouer.");
            return true;
        }
        return false;
    }

    boolean showGamePose(int res){
        if(res==0||a.invalidCharacterAssets.contains(res)){
            a.showAssetErrorOnce();
            return false;
        }
        try{
            a.petView.setImageResource(res);
            a.displayedPetStage=a.petStage();
            a.petView.setVisibility(View.VISIBLE);
            a.currentPetRes=0;
            a.updatePetPosition();
            return true;
        }catch(Throwable err){
            a.markCharacterAssetInvalid(res,"jeu","chargement impossible");
            a.showAssetErrorOnce();
            return false;
        }
    }

    void showFetchPose(){
        if(activeItem==null)return;
        int res=GameSprites.forStage(a.petStage()).fetch(activeItem.id);
        showGamePose(res);
    }

    boolean loadRopeFrames(){
        MainActivity.PetStage stage=a.petStage();
        if(ropeFrames!=null&&loadedRopeStage==stage&&ropeFrames.length==ROPE_FRAME_COUNT)return true;

        releaseRopeFrames();
        int res=GameSprites.forStage(stage).ropePlay;
        if(res==0||a.invalidCharacterAssets.contains(res)){
            a.showAssetErrorOnce();
            return false;
        }

        Bitmap strip=null;
        try{strip=BitmapFactory.decodeResource(a.getResources(),res);}catch(Throwable ignored){}
        if(strip==null){
            a.markCharacterAssetInvalid(res,"corde","ressource illisible");
            a.showAssetErrorOnce();
            return false;
        }
        int frame=GameSprites.FRAME_SIZE;
        if(strip.getWidth()!=frame*ROPE_FRAME_COUNT||strip.getHeight()!=frame){
            a.markCharacterAssetInvalid(res,"corde",
                strip.getWidth()+"x"+strip.getHeight()+" au lieu de "+(frame*ROPE_FRAME_COUNT)+"x"+frame);
            a.showAssetErrorOnce();
            return false;
        }

        ropeStrip=strip;
        ropeFrames=new Bitmap[ROPE_FRAME_COUNT];
        try{
            for(int i=0;i<ROPE_FRAME_COUNT;i++)
                ropeFrames[i]=Bitmap.createBitmap(strip,i*frame,0,frame,frame);
        }catch(Throwable err){
            releaseRopeFrames();
            a.markCharacterAssetInvalid(res,"corde","découpage impossible");
            a.showAssetErrorOnce();
            return false;
        }
        loadedRopeStage=stage;
        ropeFrameIndex=0;
        ropeFrameAt=0;
        return true;
    }

    void showRopePose(){
        if(!loadRopeFrames())return;
        long now=System.currentTimeMillis();
        if(ropeFrameAt==0||now>=ropeFrameAt){
            ropeFrameIndex=(ropeFrameIndex+1)%ropeFrames.length;
            ropeFrameAt=now+ROPE_FRAME_MS;
        }
        a.petView.setImageBitmap(ropeFrames[ropeFrameIndex]);
        a.displayedPetStage=a.petStage();
        a.petView.setVisibility(View.VISIBLE);
        a.currentPetRes=0;
        a.updatePetPosition();
    }

    void finishRopePlay(){
        a.walking=false;
        state=ROPE_READY;
        releaseRopeFrames();
        a.currentPetRes=0;
        toyView.setVisibility(View.VISIBLE);
        applyToyVisual(activeItem);
        positionToy();
        a.ensurePetImage();
        a.updatePetPosition();
        a.save();
        a.refresh();
        a.toast("🪢 Le léopard continue de jouer puis te rend la corde.");
    }

    void beginReturn(){
        if(activeItem==null){cancel();return;}
        a.releaseActionFrames();
        a.actionAnim=MainActivity.ActionAnim.NONE;
        toyView.setVisibility(View.GONE);
        state=RETURNING;
        movePetToNode(foregroundCenterNode(),true);
    }

    void finishFetch(){
        a.walking=false;
        state=NONE;
        toyView.setVisibility(View.GONE);
        applyRewards(1f);
        String name=activeItem!=null?activeItem.name:"Objet";
        activeItem=null;
        a.idleDirection=MainActivity.TravelDirection.DOWN;
        a.travelDirection=MainActivity.TravelDirection.DOWN;
        a.walkMode=MainActivity.WalkMode.FRONT;
        a.currentPetRes=0;
        a.ensurePetImage();
        a.updatePetPosition();
        a.nextWalkAt=System.currentTimeMillis()+3500L;
        a.save();
        a.refresh();
        a.toast("🐆 "+name+" rapporté !");
    }

    void applyRewards(float factor){
        if(activeItem==null||factor<=0)return;
        a.hunger=a.clamp(a.hunger+activeItem.hunger*factor);
        a.thirst=a.clamp(a.thirst+activeItem.water*factor);
        a.clean=a.clamp(a.clean+activeItem.clean*factor);
        a.affection=a.clamp(a.affection+activeItem.affection*factor);
        a.happy=a.clamp(a.happy+activeItem.happy*factor);
        a.energy=a.clamp(a.energy+activeItem.energy*factor);
        a.skillCare=a.clamp(a.skillCare+.6f*factor);
        if(factor>=.8f)a.stars+=activeItem.stars;
    }

    float[] toyPixelPosition(float nx,float ny){
        resizeToyForScene();
        float[] r=a.imageRect();
        int w=toyView.getWidth()>0?toyView.getWidth():toyView.getLayoutParams().width;
        int h=toyView.getHeight()>0?toyView.getHeight():toyView.getLayoutParams().height;
        float x=r[0]+a.clamp01(nx)*r[2]-w/2f;
        float y=r[1]+a.clamp01(ny)*r[3]-h;
        x=Math.max(r[0],Math.min(r[0]+r[2]-w,x));
        y=Math.max(r[1],Math.min(r[1]+r[3]-h,y));
        return new float[]{x,y};
    }

    void positionToy(){
        if(toyView==null||toyView.getVisibility()!=View.VISIBLE)return;
        toyView.post(()->{
            resizeToyForScene();
            float[] xy=toyPixelPosition(toyNX,toyNY);
            toyView.setX(xy[0]);
            toyView.setY(xy[1]);
            toyView.bringToFront();
        });
    }

    void positionToyNearPet(){
        if(toyView==null)return;
        toyView.setVisibility(View.VISIBLE);
        toyView.post(()->{
            resizeToyForScene();
            int w=toyView.getWidth()>0?toyView.getWidth():toyView.getLayoutParams().width;
            int h=toyView.getHeight()>0?toyView.getHeight():toyView.getLayoutParams().height;
            float[] r=a.imageRect();
            float x=a.petView.getX()+a.petView.getWidth()*.72f;
            float y=a.petView.getY()+a.petView.getHeight()*.38f-h*.42f;
            x=Math.max(r[0],Math.min(r[0]+r[2]-w,x));
            y=Math.max(r[1],Math.min(r[1]+r[3]-h,y));
            toyView.setX(x);
            toyView.setY(y);
            toyView.bringToFront();
        });
    }
}
