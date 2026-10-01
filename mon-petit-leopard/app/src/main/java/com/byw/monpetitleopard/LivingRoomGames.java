package com.byw.monpetitleopard;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Jeux interactifs du salon :
 * - "Va chercher" avec geste de lancer vers le haut.
 * - Corde maintenue au doigt.
 *
 * Les déplacements utilisent volontairement les packs de marche déjà normalisés
 * de chaque âge afin de conserver exactement la même échelle que le reste du jeu.
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

    final MainActivity a;
    TextView toyView;
    ObjectSystem.Item activeItem;
    int state=NONE;
    int landingNode=-1;
    long playUntil=0;
    long ropeHoldStartedAt=0;
    float downRawX,downRawY,startViewX,startViewY;
    float toyNX=.50f,toyNY=.95f;

    LivingRoomGames(MainActivity a){this.a=a;}

    void install(){
        toyView=new TextView(a);
        toyView.setTextSize(34);
        toyView.setGravity(Gravity.CENTER);
        toyView.setBackground(null);
        toyView.setPadding(0,0,0,0);
        toyView.setElevation(a.dp(10));
        toyView.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(a.dp(68),a.dp(68));
        lp.gravity=Gravity.TOP|Gravity.LEFT;
        a.scene.addView(toyView,lp);
        toyView.setOnTouchListener((v,e)->handleTouch(e));
    }

    boolean active(){return state!=NONE;}
    boolean fastRun(){return state==RUN_TO_TOY||state==RETURNING;}
    int walkFrameAdvance(){return fastRun()?2:1;}

    void cancel(){
        if(toyView!=null){
            toyView.animate().cancel();
            toyView.setVisibility(View.GONE);
            toyView.setRotation(0f);
        }
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
        toyView.setText(item.icon);
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
        toyView.setText("🪢");
        toyView.setVisibility(View.VISIBLE);
        int front=foregroundCenterNode();
        float[][] nodes=a.roomNodes();
        toyNX=Math.min(.82f,nodes[front][0]+.16f);
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
            // Un lancer vers le haut doit atterrir dans la rangée la plus profonde du tapis.
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
            toyView.animate().cancel();
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
        float[] xy=toyPixelPosition(toyNX,toyNY);
        toyView.animate().cancel();
        toyView.animate()
            .x(xy[0]).y(xy[1])
            .rotationBy(360f)
            .setDuration(520)
            .withEndAction(()->{
                toyView.setRotation(0f);
                state=RUN_TO_TOY;
                movePetToNode(landingNode,true);
            }).start();
    }

    boolean handleRopeTouch(MotionEvent e){
        if(state==ROPE_READY && e.getAction()==MotionEvent.ACTION_DOWN){
            state=ROPE_HOLD;
            ropeHoldStartedAt=System.currentTimeMillis();
            // Boucle l'animation de jeu existante : même taille et même pack d'âge.
            a.startActionAnimation(MainActivity.ActionAnim.JUMP,Long.MAX_VALUE/4);
            positionToyNearPet();
            return true;
        }
        if(state==ROPE_HOLD && (e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)){
            long held=Math.max(0,System.currentTimeMillis()-ropeHoldStartedAt);
            float factor=Math.min(1f,held/1800f);
            applyRewards(factor);
            a.releaseActionFrames();
            a.actionAnim=MainActivity.ActionAnim.NONE;
            a.currentPetRes=0;
            state=ROPE_READY;
            positionToy();
            a.ensurePetImage();
            a.updatePetPosition();
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
            positionToyNearPet();
            if(now>=playUntil){
                beginReturn();
                return false;
            }
            return false; // laisse ActionAnim.JUMP afficher le jeu.
        }

        if(state==ROPE_HOLD){
            positionToyNearPet();
            return false; // laisse l'animation JUMP boucler tant que le doigt reste appuyé.
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
            positionToyNearPet();
            a.startActionAnimation(MainActivity.ActionAnim.JUMP,1350L);
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

    void beginReturn(){
        if(activeItem==null){cancel();return;}
        a.releaseActionFrames();
        a.actionAnim=MainActivity.ActionAnim.NONE;
        toyView.setVisibility(View.GONE); // objet considéré comme attrapé.
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
            float[] xy=toyPixelPosition(toyNX,toyNY);
            toyView.setX(xy[0]);toyView.setY(xy[1]);
            toyView.bringToFront();
        });
    }

    void positionToyNearPet(){
        if(toyView==null)return;
        toyView.setVisibility(View.VISIBLE);
        toyNX=a.clamp01(a.petNX+.09f);
        toyNY=a.clamp01(a.petNY+.01f);
        positionToy();
    }
}
