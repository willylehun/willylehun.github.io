package com.byw.monpetitleopard;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final long H=3600000L, CUB=H, TEEN=5*H, ADULT=5*H, OLD=2*H, LIFE=13*H;

    final Handler handler=new Handler(Looper.getMainLooper());
    final Random rnd=new Random();

    SharedPreferences sp;
    ObjectSystem objects;

    long born,last,nextMischiefAt=0,nextWalkAt=0,manualUntil=0,sleepEndAt=0,nextAutoSleepAt=0;
    float hunger=85,thirst=85,clean=90,affection=90,happy=90,energy=90;
    float skillClean=5,skillObedience=5,skillCare=5;
    int stars=0,generation=1;
    String pet="Léo",room="salon",incident="";
    String historyLog="",adoptedLog="";
    boolean endShown=false,cleaningMode=false,sleeping=false,walking=false;
    float cleanProgress=0,lastRubX=0,lastRubY=0;
    float petNX=.50f,petNY=.90f,targetNX=.50f,targetNY=.90f;

    TextView title,subTitle,timer,starTxt,moodLabel,skillTxt,cleanHint,incidentView;
    ProgressBar[] bars=new ProgressBar[6];
    TextView[] vals=new TextView[6];
    ImageView bgFill,bg,petView;
    FrameLayout scene;
    LinearLayout root,bottomBar;
    Space flexibleSpace,footerSpace;
    Button roomsBtn,objectsBtn,actionsBtn,menuBtn;

    int walkDir=-1,walkTick=0,idleTick=0,currentPetRes=0,manualFrame=0,walkFrameIndex=0;
    int petNodeIndex=-1,targetNodeIndex=-1;
    WalkMode walkMode=WalkMode.SIDE;
    int currentWalkStripRes=0;
    Bitmap currentWalkStrip=null;
    Bitmap[] currentWalkFrames=null;
    PetStage visualStage=null;
    PetStage loadedWalkStage=null;
    WalkMode loadedWalkMode=null;
    TravelDirection travelDirection=TravelDirection.LEFT;

    Bitmap cubFaceMoodStrip=null;
    Bitmap[] cubFaceMoodFrames=null;
    long faceMoodUntil=0,nextFaceMoodAt=0;
    int activeFaceMood=-1;

    final Set<Integer> invalidCharacterAssets=new HashSet<>();
    boolean assetErrorShown=false;

    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}
    enum PetStage {CUB,TEEN,ADULT,OLD}
    enum PetMood {IDLE,HAPPY,TIRED,SLEEP}
    enum WalkMode {SIDE,FRONT,BACK}
    enum TravelDirection {LEFT,RIGHT,UP,DOWN}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sp=getSharedPreferences("pet",MODE_PRIVATE);
        load();
        if(sp.getBoolean("named",false))ensureCurrentAdoptionRecorded();
        objects=new ObjectSystem(this);
        build();
        validateCharacterAssets();
        tickNeeds();
        refresh();
        if(!sp.getBoolean("named",false))rename(true);
    }

    @Override protected void onResume(){
        super.onResume();
        tickNeeds();
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(animator);
        handler.post(ticker);
        handler.post(animator);
    }

    @Override protected void onPause(){
        super.onPause();
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(animator);
        tickNeeds();
        save();
    }

    final Runnable ticker=new Runnable(){
        @Override public void run(){
            tickNeeds();
            maybeAutoSleep();
            maybeMischief();
            refresh();
            handler.postDelayed(this,1000);
        }
    };

    final Runnable animator=new Runnable(){
        @Override public void run(){
            animateAuto();
            handler.postDelayed(this,220);
        }
    };

    void load(){
        long n=System.currentTimeMillis();
        born=sp.getLong("born",n);
        last=sp.getLong("last",n);
        hunger=sp.getFloat("hunger",85);
        thirst=sp.getFloat("thirst",85);
        clean=sp.getFloat("clean",90);
        affection=sp.getFloat("affection",90);
        happy=sp.getFloat("happy",90);
        energy=sp.getFloat("energy",90);
        skillClean=sp.getFloat("skillClean",5);
        skillObedience=sp.getFloat("skillObedience",5);
        skillCare=sp.getFloat("skillCare",5);
        stars=sp.getInt("stars",0);
        generation=sp.getInt("generation",1);
        pet=sp.getString("name","Léo");
        room=sp.getString("room","salon");
        incident=sp.getString("incident","");
        nextMischiefAt=sp.getLong("nextMischiefAt",0);
        sleeping=sp.getBoolean("sleeping",false);
        sleepEndAt=sp.getLong("sleepEndAt",0);
        nextAutoSleepAt=sp.getLong("nextAutoSleepAt",0);
        historyLog=sp.getString("historyLog","");
        adoptedLog=sp.getString("adoptedLog","");

        // Ne pas annuler ici un sommeil expiré : tickNeeds() calcule d'abord
        // la portion réellement passée à dormir, même si l'app était fermée.
        if(nextAutoSleepAt==0){
            nextAutoSleepAt=n+(4+rnd.nextInt(4))*60000L;
        }
        if(!sp.contains("born"))save();
    }

    void save(){
        sp.edit()
          .putLong("born",born).putLong("last",last)
          .putFloat("hunger",hunger).putFloat("thirst",thirst)
          .putFloat("clean",clean).putFloat("affection",affection)
          .putFloat("happy",happy).putFloat("energy",energy)
          .putFloat("skillClean",skillClean)
          .putFloat("skillObedience",skillObedience)
          .putFloat("skillCare",skillCare)
          .putInt("stars",stars).putInt("generation",generation)
          .putString("name",pet).putString("room",room)
          .putString("incident",incident)
          .putLong("nextMischiefAt",nextMischiefAt)
          .putBoolean("sleeping",sleeping)
          .putLong("sleepEndAt",sleepEndAt)
          .putLong("nextAutoSleepAt",nextAutoSleepAt)
          .putString("historyLog",historyLog)
          .putString("adoptedLog",adoptedLog)
          .apply();
    }

    float clamp(float v){return Math.max(0,Math.min(100,v));}

    void tickNeeds(){
        long n=System.currentTimeMillis();
        long start=last;
        long d=Math.max(0,n-start);
        if(d==0){last=n;return;}

        long sleepMs=0;
        if(sleeping){
            sleepMs=Math.max(0,Math.min(n,sleepEndAt)-start);
        }
        long awakeMs=Math.max(0,d-sleepMs);

        if(stage()!=Stage.ENDED){
            if(sleepMs>0){
                float sm=sleepMs/60000f;
                hunger-=.15f*sm;
                thirst-=.20f*sm;
                clean-=.04f*sm;
                affection-=.02f*sm;
                // 90 secondes de sommeil autonome peuvent recharger complètement la jauge.
                energy+=(100f/1.5f)*sm;
            }

            if(awakeMs>0){
                float m=awakeMs/60000f;
                float learnedClean=1f-(0.38f*skillClean/100f);
                hunger-=.34f*m;
                thirst-=.42f*m;
                clean-=.12f*m*learnedClean;
                affection-=.13f*m;
                happy-=.10f*m;
                energy-=.20f*m;

                if(!incident.isEmpty()){
                    clean-=.09f*m;
                    happy-=.04f*m;
                }

                int critical=0;
                if(hunger<22)critical++;
                if(thirst<22)critical++;
                if(clean<20)critical++;
                if(affection<20)critical++;
                if(energy<16)critical++;
                happy-=critical*.08f*m;
            }
        }

        hunger=clamp(hunger);thirst=clamp(thirst);clean=clamp(clean);
        affection=clamp(affection);happy=clamp(happy);energy=clamp(energy);

        if(sleeping && n>=sleepEndAt){
            sleeping=false;
            sleepEndAt=0;
            currentPetRes=0;
            addHistory(pet+" s'est réveillé naturellement.");
            nextAutoSleepAt=n+(4+rnd.nextInt(4))*60000L;
        }

        last=n;
    }

    Stage stage(){
        long a=System.currentTimeMillis()-born;
        if(a<CUB)return Stage.CUB;
        if(a<CUB+TEEN)return Stage.TEEN;
        if(a<CUB+TEEN+ADULT)return Stage.ADULT;
        if(a<LIFE)return Stage.OLD;
        return Stage.ENDED;
    }

    long remain(){
        long a=System.currentTimeMillis()-born;
        if(a<CUB)return CUB-a;
        if(a<CUB+TEEN)return CUB+TEEN-a;
        if(a<CUB+TEEN+ADULT)return CUB+TEEN+ADULT-a;
        if(a<LIFE)return LIFE-a;
        return 0;
    }

    String stageName(){
        switch(stage()){
            case CUB:return "Léopardeau";
            case TEEN:return "Ado";
            case ADULT:return "Adulte";
            case OLD:return "Vieux";
            default:return "Cycle terminé";
        }
    }

    void build(){
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0,dp(2),0,0);
        root.setBackgroundColor(Color.rgb(242,232,210));

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(6),0,dp(6),0);

        LinearLayout names=new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        title=text(17,true);
        subTitle=text(9,false);
        names.addView(title);
        names.addView(subTitle);
        header.addView(names,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));

        timer=pill();
        starTxt=pill();
        header.addView(timer);

        LinearLayout.LayoutParams starParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        starParams.setMargins(dp(4),0,0,0);
        header.addView(starTxt,starParams);

        menuBtn=button("⋮");
        menuBtn.setTextSize(18);
        menuBtn.setPadding(0,0,0,0);
        LinearLayout.LayoutParams menuParams=new LinearLayout.LayoutParams(dp(36),dp(34));
        menuParams.setMargins(dp(4),0,0,0);
        header.addView(menuBtn,menuParams);
        menuBtn.setOnClickListener(v->showTopMenu());

        root.addView(header);

        LinearLayout needRow1=new LinearLayout(this);
        LinearLayout needRow2=new LinearLayout(this);
        needRow1.setPadding(dp(4),0,dp(4),0);
        needRow2.setPadding(dp(4),0,dp(4),0);
        String[] namesNeeds={"Faim","Eau","Propreté","Câlins","Bonheur","Sommeil"};
        for(int i=0;i<6;i++){
            LinearLayout box=needBox(namesNeeds[i],i);
            (i<3?needRow1:needRow2).addView(box,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        }
        root.addView(needRow1);
        root.addView(needRow2);

        skillTxt=text(8,false);
        skillTxt.setGravity(Gravity.CENTER);
        skillTxt.setPadding(dp(4),0,dp(4),dp(2));
        root.addView(skillTxt);

        scene=new FrameLayout(this);
        GradientDrawable sceneBg=new GradientDrawable();
        sceneBg.setColor(Color.rgb(204,181,145));
        sceneBg.setCornerRadius(dp(10));
        scene.setBackground(sceneBg);
        scene.setClipToOutline(true);

        // Remplissage décoratif derrière l'image complète : évite les bandes claires
        // sans jamais rogner ni déformer l'image principale.
        bgFill=new ImageView(this);
        bgFill.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgFill.setAdjustViewBounds(false);
        bgFill.setAlpha(.34f);
        if(Build.VERSION.SDK_INT>=31){
            bgFill.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                22f,22f,android.graphics.Shader.TileMode.CLAMP));
        }
        scene.addView(bgFill,new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        bg=new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bg.setAdjustViewBounds(false);
        scene.addView(bg,new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        petView=new ImageView(this);
        petView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        petView.setAdjustViewBounds(false);
        petView.setVisibility(View.VISIBLE);
        petView.setAlpha(1f);
        petView.setElevation(dp(4));
        petView.setPivotX(0);
        petView.setPivotY(0);
        FrameLayout.LayoutParams petParams=new FrameLayout.LayoutParams(dp(120),dp(120));
        petParams.gravity=Gravity.TOP|Gravity.LEFT;
        scene.addView(petView,petParams);
        petView.setOnClickListener(v->petLeopard());

        moodLabel=overlay();
        FrameLayout.LayoutParams moodParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        moodParams.gravity=Gravity.TOP|Gravity.LEFT;
        moodParams.setMargins(dp(7),dp(7),0,0);
        scene.addView(moodLabel,moodParams);

        cleanHint=overlay();
        cleanHint.setText("🧽 Frotte sur la bêtise pour la faire disparaître");
        cleanHint.setVisibility(View.GONE);
        FrameLayout.LayoutParams hintParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        hintParams.setMargins(dp(6),dp(44),dp(6),0);
        scene.addView(cleanHint,hintParams);

        incidentView=new TextView(this);
        incidentView.setTextSize(30);
        incidentView.setGravity(Gravity.CENTER);
        incidentView.setVisibility(View.GONE);
        GradientDrawable incidentBg=new GradientDrawable();
        incidentBg.setColor(Color.argb(210,255,249,230));
        incidentBg.setCornerRadius(dp(18));
        incidentBg.setStroke(dp(1),Color.argb(100,90,60,30));
        incidentView.setBackground(incidentBg);
        incidentView.setElevation(dp(8));
        FrameLayout.LayoutParams incidentParams=new FrameLayout.LayoutParams(dp(52),dp(52));
        incidentParams.gravity=Gravity.TOP|Gravity.RIGHT;
        incidentParams.setMargins(0,dp(54),dp(10),0);
        scene.addView(incidentView,incidentParams);
        incidentView.setOnTouchListener((v,e)->handleRub(e));

        root.addView(scene,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(360),0));

        bottomBar=new LinearLayout(this);
        bottomBar.setPadding(dp(4),0,dp(4),0);
        roomsBtn=button("🏠 Pièces");
        objectsBtn=button("🎒 Objets");
        actionsBtn=button("⚙ Actions");

        roomsBtn.setOnClickListener(v->roomsMenu());
        objectsBtn.setOnClickListener(v->objects.openMenu());
        actionsBtn.setOnClickListener(v->actionsMenu());

        bottomBar.addView(roomsBtn,buttonParams());
        bottomBar.addView(objectsBtn,buttonParams());
        bottomBar.addView(actionsBtn,buttonParams());
        root.addView(bottomBar);

        footerSpace=new Space(this);
        root.addView(footerSpace,new LinearLayout.LayoutParams(1,dp(12)));

        setContentView(root);
        root.post(()->{
            fitSceneAndPet();
            resetPetForRoom(false);
        });
    }

    LinearLayout needBox(String label,int index){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2),0,dp(2),0);

        TextView t=text(8,false);
        t.setGravity(Gravity.CENTER);
        t.setText(label);
        box.addView(t);

        bars[index]=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bars[index].setMax(100);
        box.addView(bars[index],new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(5)));

        vals[index]=text(7,false);
        vals[index].setGravity(Gravity.CENTER);
        box.addView(vals[index]);
        return box;
    }

    LinearLayout.LayoutParams buttonParams(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);
        p.setMargins(dp(2),dp(4),dp(2),0);
        return p;
    }

    TextView text(int size,boolean bold){
        TextView t=new TextView(this);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(55,47,34));
        if(bold)t.setTypeface(null,1);
        return t;
    }

    TextView pill(){
        TextView t=text(12,false);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(9),dp(5),dp(9),dp(5));
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(91,103,60));
        g.setCornerRadius(dp(30));
        t.setBackground(g);
        return t;
    }

    TextView overlay(){
        TextView t=text(12,true);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(8),dp(5),dp(8),dp(5));
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.argb(195,55,47,34));
        g.setCornerRadius(dp(12));
        t.setBackground(g);
        return t;
    }

    Button button(String label){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(99,111,66));
        g.setCornerRadius(dp(14));
        b.setBackground(g);
        return b;
    }

    void refresh(){
        syncVisualStage();
        title.setText(pet+" • génération "+generation);
        subTitle.setText(stageName()+" • "+roomName());
        timer.setText(stage()==Stage.ENDED?"Terminé":format(remain()));
        starTxt.setText("★ "+stars);

        float[] v={hunger,thirst,clean,affection,happy,energy};
        for(int i=0;i<6;i++){
            int value=Math.round(v[i]);
            bars[i].setProgress(value);
            vals[i].setText(value+"%");
        }

        skillTxt.setText("Compétences : Propreté "+Math.round(skillClean)+"  •  Obéissance "+Math.round(skillObedience)+"  •  Délicatesse "+Math.round(skillCare));
        moodLabel.setText("Humeur : "+moodText());

        refreshRoom();
        refreshIncident();

        if(stage()==Stage.ENDED)endLife();
    }

    String roomName(){
        if(room.equals("cuisine"))return "Cuisine";
        if(room.equals("bain"))return "Salle de bain";
        if(room.equals("jardin"))return "Jardin";
        return "Salon";
    }

    void refreshRoom(){
        int res=room.equals("cuisine")?R.drawable.room_kitchen_hd:
                room.equals("bain")?R.drawable.room_bathroom_hd:
                room.equals("jardin")?R.drawable.room_garden_hd:R.drawable.room_living_hd;
        bgFill.setImageResource(res);
        bg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bg.setScaleX(1.06f);
        bg.setScaleY(1.06f);
        bg.setImageResource(res);
        ensurePetImage();
        scene.post(this::fitSceneAndPet);
    }

    void ensurePetImage(){
        boolean faceActive=petStage()==PetStage.CUB
                && !sleeping
                && !walking
                && activeFaceMood>=0
                && System.currentTimeMillis()<faceMoodUntil;

        if(faceActive){
            if(loadCubFaceMoodFrames()){
                petView.setImageBitmap(cubFaceMoodFrames[activeFaceMood]);
                currentPetRes=0;
            }else{
                faceActive=false;
                faceMoodUntil=0;
                activeFaceMood=-1;
            }
        }

        int res=0;
        if(!faceActive){
            if(sleeping)res=sleepDrawable();
            else if(!walking)res=emotionDrawable();
        }

        if(res!=0){
            if(invalidCharacterAssets.contains(res)){
                petView.setVisibility(View.INVISIBLE);
                showAssetErrorOnce();
                return;
            }
            if(res!=currentPetRes){
                if(!setPetDrawableSafely(petStage(),res))return;
                currentPetRes=res;
            }
        }

        petView.setAlpha(1f);
        petView.setVisibility(View.VISIBLE);
        petView.bringToFront();
        incidentView.bringToFront();
        moodLabel.bringToFront();
        cleanHint.bringToFront();
        updatePetPosition();
    }

    void syncVisualStage(){
        PetStage now=petStage();
        if(visualStage==now)return;

        PetStage previous=visualStage;
        visualStage=now;

        // Un changement d'âge invalide immédiatement toute image/animation
        // déjà chargée. Ainsi aucun frame de l'âge précédent ne peut rester
        // affiché pendant le nouveau cycle.
        walking=false;
        walkMode=WalkMode.SIDE;
        travelDirection=TravelDirection.LEFT;
        walkFrameIndex=0;
        releaseWalkFrames();
        releaseCubFaceMoodFrames();
        faceMoodUntil=0;
        nextFaceMoodAt=0;
        activeFaceMood=-1;
        currentPetRes=0;

        if(previous!=null){
            addHistory("Verrouillage visuel : "+previous+" → "+now+".");
        }
    }

    PetStage petStage(){
        Stage s=stage();
        if(s==Stage.CUB)return PetStage.CUB;
        if(s==Stage.TEEN)return PetStage.TEEN;
        if(s==Stage.ADULT)return PetStage.ADULT;
        // Fin de cycle : l'animal conserve son apparence OLD. Ce n'est pas un fallback d'asset.
        return PetStage.OLD;
    }

    int getMoodDrawable(PetStage stage,PetMood mood){
        return CharacterSprites.forStage(stage).mood(mood);
    }

    int getWalkStrip(PetStage stage,WalkMode mode){
        return CharacterSprites.forStage(stage).walk(mode);
    }

    int idleDrawable(){return getMoodDrawable(petStage(),PetMood.IDLE);}
    int happyDrawable(){return getMoodDrawable(petStage(),PetMood.HAPPY);}
    int tiredDrawable(){return getMoodDrawable(petStage(),PetMood.TIRED);}
    int sleepDrawable(){return getMoodDrawable(petStage(),PetMood.SLEEP);}

    int emotionDrawable(){
        if(sleeping)return sleepDrawable();
        if(energy<24 || hunger<18 || thirst<18 || clean<18 || happy<28)return tiredDrawable();
        // Le sprite "happy" est volontairement réservé aux réactions ponctuelles.
        return idleDrawable();
    }

    int walkStripDrawable(WalkMode mode){
        return getWalkStrip(petStage(),mode);
    }

    int cubFaceMoodStripRes(){return CharacterSprites.forStage(PetStage.CUB).faceMoods;}

    void releaseCubFaceMoodFrames(){
        if(cubFaceMoodFrames!=null){
            for(Bitmap b:cubFaceMoodFrames){
                if(b!=null && !b.isRecycled())b.recycle();
            }
        }
        if(cubFaceMoodStrip!=null && !cubFaceMoodStrip.isRecycled())cubFaceMoodStrip.recycle();
        cubFaceMoodFrames=null;
        cubFaceMoodStrip=null;
    }

    boolean loadCubFaceMoodFrames(){
        if(petStage()!=PetStage.CUB)return false;
        int res=cubFaceMoodStripRes();
        if(invalidCharacterAssets.contains(res)){
            showAssetErrorOnce();
            return false;
        }
        CharacterSprites.Pack pack=CharacterSprites.forStage(PetStage.CUB);
        if(cubFaceMoodFrames!=null && cubFaceMoodFrames.length==pack.faceFrameCount)return true;

        releaseCubFaceMoodFrames();

        Bitmap strip=null;
        try{strip=BitmapFactory.decodeResource(getResources(),res);}catch(Throwable ignored){}
        if(strip==null){
            markCharacterAssetInvalid(res,"CUB FACE_MOODS","ressource illisible");
            showAssetErrorOnce();
            return false;
        }

        final int frame=pack.faceFrameSize;
        final int count=pack.faceFrameCount; // images 1 à 11 + image 13 de la planche léopardeau
        if(strip.getWidth()!=frame*count || strip.getHeight()!=frame){
            int w=strip.getWidth(),h=strip.getHeight();
            strip.recycle();
            markCharacterAssetInvalid(res,"CUB FACE_MOODS",w+"x"+h+" au lieu de "+(frame*count)+"x"+frame);
            showAssetErrorOnce();
            return false;
        }

        cubFaceMoodStrip=strip;
        cubFaceMoodFrames=new Bitmap[count];
        try{
            for(int i=0;i<count;i++){
                cubFaceMoodFrames[i]=Bitmap.createBitmap(cubFaceMoodStrip,i*frame,0,frame,frame);
            }
        }catch(Throwable err){
            releaseCubFaceMoodFrames();
            markCharacterAssetInvalid(res,"CUB FACE_MOODS","découpage impossible");
            showAssetErrorOnce();
            return false;
        }
        return true;
    }

    int chooseCubFaceMoodIndex(){
        if(!incident.isEmpty())return 1;
        if(energy<22)return 4;
        if(hunger<18 || thirst<18 || clean<18)return 2;
        if(happy<35)return 2;
        if(happy>85 && affection>80)return rnd.nextBoolean()?7:8;
        if(happy>65)return rnd.nextBoolean()?0:5;
        if(affection>82)return 6;
        if(rnd.nextInt(4)==0)return 10;
        return rnd.nextBoolean()?9:11;
    }

    void scheduleNextFaceMood(long now){
        nextFaceMoodAt=now+7000L+rnd.nextInt(7001);
    }

    void maybeShowFaceMood(long now){
        if(petStage()!=PetStage.CUB || sleeping || walking || now<manualUntil)return;
        if(nextFaceMoodAt==0){
            nextFaceMoodAt=now+2500L+rnd.nextInt(2501);
            return;
        }
        if(now<nextFaceMoodAt)return;
        activeFaceMood=chooseCubFaceMoodIndex();
        faceMoodUntil=now+1600L+rnd.nextInt(801);
        scheduleNextFaceMood(now);
    }

    void showCubFaceMoodNow(int index,int duration){
        if(petStage()!=PetStage.CUB)return;
        walking=false;
        walkMode=WalkMode.SIDE;
        travelDirection=TravelDirection.LEFT;
        releaseWalkFrames();
        activeFaceMood=Math.max(0,Math.min(11,index));
        faceMoodUntil=System.currentTimeMillis()+Math.max(250,duration);
        scheduleNextFaceMood(faceMoodUntil);
        currentPetRes=0;
        ensurePetImage();
        updatePetPosition();
    }

    void releaseWalkFrames(){
        if(currentWalkFrames!=null){
            for(Bitmap b:currentWalkFrames){
                if(b!=null && !b.isRecycled())b.recycle();
            }
        }
        if(currentWalkStrip!=null && !currentWalkStrip.isRecycled())currentWalkStrip.recycle();
        currentWalkFrames=null;
        currentWalkStrip=null;
        currentWalkStripRes=0;
        loadedWalkStage=null;
        loadedWalkMode=null;
    }

    boolean loadWalkFrames(PetStage expectedStage,WalkMode expectedMode,int res){
        // Verrou dur : une animation ne peut être chargée que si elle appartient
        // exactement à l'âge et à la direction actuellement demandés.
        if(expectedStage!=petStage() || res!=getWalkStrip(expectedStage,expectedMode)){
            addHistory("ERREUR mélange d'âge bloqué avant chargement de marche.");
            showAssetErrorOnce();
            return false;
        }
        if(invalidCharacterAssets.contains(res)){
            showAssetErrorOnce();
            return false;
        }
        if(currentWalkStripRes==res
                && loadedWalkStage==expectedStage
                && loadedWalkMode==expectedMode
                && currentWalkFrames!=null
                && currentWalkFrames.length>0)return true;

        releaseWalkFrames();

        Bitmap strip=null;
        try{
            strip=BitmapFactory.decodeResource(getResources(),res);
        }catch(Throwable ignored){}

        if(strip==null){
            markCharacterAssetInvalid(res,"strip de marche","ressource illisible");
            showAssetErrorOnce();
            return false;
        }

        int w=strip.getWidth();
        int h=strip.getHeight();
        if(h<=0 || w<h || w%h!=0){
            strip.recycle();
            markCharacterAssetInvalid(res,"strip de marche",w+"x"+h+" : frames non carrées");
            showAssetErrorOnce();
            return false;
        }

        int sourceCount=w/h;
        if(sourceCount<4){
            strip.recycle();
            markCharacterAssetInvalid(res,"strip de marche",w+"x"+h+" : au moins 4 frames requises");
            showAssetErrorOnce();
            return false;
        }
        int count=4;
        currentWalkStrip=strip;
        currentWalkFrames=new Bitmap[count];
        try{
            for(int i=0;i<count;i++){
                currentWalkFrames[i]=Bitmap.createBitmap(currentWalkStrip,i*h,0,h,h);
            }
        }catch(Throwable err){
            releaseWalkFrames();
            markCharacterAssetInvalid(res,"strip de marche","découpage impossible");
            showAssetErrorOnce();
            return false;
        }

        currentWalkStripRes=res;
        loadedWalkStage=expectedStage;
        loadedWalkMode=expectedMode;
        walkFrameIndex=0;
        return true;
    }

    void showWalkFrame(WalkMode mode){
        PetStage expectedStage=petStage();
        int res=getWalkStrip(expectedStage,mode);
        if(!loadWalkFrames(expectedStage,mode,res)){
            walking=false;
            currentPetRes=0;
            ensurePetImage();
            return;
        }

        // Deuxième garde juste avant l'affichage : même si l'âge changeait
        // entre deux ticks, un bitmap de l'ancien pack ne peut pas être rendu.
        if(expectedStage!=petStage()
                || loadedWalkStage!=expectedStage
                || loadedWalkMode!=mode
                || currentWalkStripRes!=res){
            walking=false;
            releaseWalkFrames();
            currentPetRes=0;
            ensurePetImage();
            return;
        }

        petView.setImageBitmap(currentWalkFrames[walkFrameIndex]);
        petView.setVisibility(View.VISIBLE);
        walkFrameIndex=(walkFrameIndex+1)%currentWalkFrames.length;
        currentPetRes=0;
    }

    void fitSceneAndPet(){
        if(root==null||scene==null||root.getWidth()<=0||root.getHeight()<=0)return;

        int fixedHeight=root.getPaddingTop()+root.getPaddingBottom();
        for(int i=0;i<root.getChildCount();i++){
            View child=root.getChildAt(i);
            if(child==scene)continue;
            if(child.getVisibility()==View.GONE)continue;
            ViewGroup.LayoutParams raw=child.getLayoutParams();
            int margins=0;
            if(raw instanceof LinearLayout.LayoutParams){
                LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
                margins=lp.topMargin+lp.bottomMargin;
            }
            fixedHeight+=child.getMeasuredHeight()+margins;
        }

        int available=Math.max(dp(300),root.getHeight()-fixedHeight);
        LinearLayout.LayoutParams sceneLp=(LinearLayout.LayoutParams)scene.getLayoutParams();
        sceneLp.width=ViewGroup.LayoutParams.MATCH_PARENT;
        sceneLp.height=available;
        sceneLp.weight=0;
        scene.setLayoutParams(sceneLp);

        scene.post(()->{
            resizePetForScene();
            updatePetPosition();
        });
    }

    float[] imageRect(){
        float sw=scene.getWidth(), sh=scene.getHeight();
        if(sw<=0||sh<=0)return new float[]{0,0,0,0};
        float scale=Math.min(sw/1536f,sh/1152f);
        float iw=1536f*scale;
        float ih=1152f*scale;
        float left=(sw-iw)/2f;
        float top=(sh-ih)/2f;
        return new float[]{left,top,iw,ih};
    }

    void resizePetForScene(){
        float[] r=imageRect();
        if(r[2]<=0||r[3]<=0)return;

        float ratio=stage()==Stage.CUB?.30f:
                    stage()==Stage.TEEN?.325f:
                    stage()==Stage.ADULT?.35f:.335f;

        int size=Math.round(r[2]*ratio);
        size=Math.max(size,dp(88));

        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)petView.getLayoutParams();
        lp.width=size;
        lp.height=size;
        lp.gravity=Gravity.TOP|Gravity.LEFT;
        petView.setLayoutParams(lp);
        petView.setPivotX(size/2f);
        petView.setPivotY(size);

        FrameLayout.LayoutParams incidentLp=(FrameLayout.LayoutParams)incidentView.getLayoutParams();
        int incidentSize=Math.max(dp(40),Math.round(r[2]*.09f));
        incidentLp.width=incidentSize;
        incidentLp.height=incidentSize;
        incidentLp.gravity=Gravity.TOP|Gravity.RIGHT;
        incidentLp.topMargin=Math.max(dp(44),Math.round(scene.getHeight()*.09f));
        incidentLp.rightMargin=dp(8);
        incidentView.setLayoutParams(incidentLp);
    }

    float clamp01(float v){return Math.max(0f,Math.min(1f,v));}

    /**
     * Points de marche analysés pièce par pièce.
     * Chaque point correspond à une zone de sol réellement dégagée dans l'image.
     * Les connexions ci-dessous empêchent les trajets de traverser canapé,
     * îlot de cuisine, baignoire, toilettes, bancs et massifs.
     */
    float[][] roomNodes(){
        if(room.equals("cuisine")){
            // L'îlot central occupe le milieu : couloirs de marche uniquement à gauche/droite.
            return new float[][]{
                {.09f,.93f},{.10f,.83f},{.12f,.73f},
                {.91f,.93f},{.90f,.83f},{.88f,.73f}
            };
        }
        if(room.equals("bain")){
            return new float[][]{
                {.18f,.93f},{.40f,.95f},{.62f,.95f},{.80f,.92f},
                {.18f,.82f},{.78f,.82f}
            };
        }
        if(room.equals("jardin")){
            return new float[][]{
                {.50f,.90f},{.43f,.79f},{.56f,.70f},{.48f,.61f},
                {.51f,.51f},{.26f,.76f},{.74f,.75f},{.28f,.88f},{.72f,.88f}
            };
        }
        // Salon : grande zone libre devant le canapé.
        return new float[][]{
            {.16f,.91f},{.34f,.93f},{.52f,.94f},{.70f,.93f},{.85f,.91f},
            {.20f,.80f},{.50f,.81f},{.80f,.80f}
        };
    }

    int[][] roomLinks(){
        if(room.equals("cuisine")){
            return new int[][]{
                {1},{0,2},{1},
                {4},{3,5},{4}
            };
        }
        if(room.equals("bain")){
            return new int[][]{
                {1,4},{0,2},{1,3},{2,5},{0},{3}
            };
        }
        if(room.equals("jardin")){
            return new int[][]{
                {1,7,8},{0,2,5},{1,3,6},{2,4},{3},
                {1,7},{2,8},{0,5},{0,6}
            };
        }
        return new int[][]{
            {1,5},{0,2,5,6},{1,3,6},{2,4,6,7},{3,7},
            {0,1,6},{1,2,3,5,7},{3,4,6}
        };
    }

    int defaultRoomNode(){
        if(room.equals("cuisine"))return rnd.nextBoolean()?1:4;
        if(room.equals("bain"))return 1;
        if(room.equals("jardin"))return 0;
        return 2;
    }

    void resetPetForRoom(boolean animate){
        float[][] nodes=roomNodes();
        petNodeIndex=Math.min(defaultRoomNode(),nodes.length-1);
        targetNodeIndex=petNodeIndex;
        petNX=nodes[petNodeIndex][0];
        petNY=nodes[petNodeIndex][1];
        targetNX=petNX;
        targetNY=petNY;
        walking=false;
        walkMode=WalkMode.SIDE;
        travelDirection=TravelDirection.LEFT;
        walkFrameIndex=0;
        faceMoodUntil=0;
        activeFaceMood=-1;
        currentPetRes=0;
        if(!sleeping)ensurePetImage();
        updatePetPosition();
        nextWalkAt=System.currentTimeMillis()+(animate?700:1300)+rnd.nextInt(1800);
    }

    void chooseWalkTarget(){
        float[][] nodes=roomNodes();
        int[][] links=roomLinks();

        if(petNodeIndex<0||petNodeIndex>=nodes.length)petNodeIndex=defaultRoomNode();
        int[] choices=links[Math.min(petNodeIndex,links.length-1)];
        if(choices.length==0)return;

        targetNodeIndex=choices[rnd.nextInt(choices.length)];
        targetNX=nodes[targetNodeIndex][0];
        targetNY=nodes[targetNodeIndex][1];

        float dx=targetNX-petNX;
        float dy=targetNY-petNY;
        // Mode figé pendant tout le segment pour éviter les changements
        // d'animation en plein mouvement.
        if(Math.abs(dy)>Math.abs(dx)*.70f){
            travelDirection=dy>0?TravelDirection.DOWN:TravelDirection.UP;
            walkMode=travelDirection==TravelDirection.DOWN?WalkMode.FRONT:WalkMode.BACK;
        }else{
            travelDirection=dx<0?TravelDirection.LEFT:TravelDirection.RIGHT;
            walkMode=WalkMode.SIDE;
            walkDir=travelDirection==TravelDirection.LEFT?-1:1;
        }

        faceMoodUntil=0;
        activeFaceMood=-1;
        walking=true;
        walkFrameIndex=0;
    }

    float depthScale(){
        float[][] nodes=roomNodes();
        float minY=1f,maxY=0f;
        for(float[] n:nodes){
            minY=Math.min(minY,n[1]);
            maxY=Math.max(maxY,n[1]);
        }
        float t=clamp01((petNY-minY)/Math.max(.01f,maxY-minY));
        // L'animal est naturellement plus petit au fond et plus grand au premier plan.
        return .76f+.28f*t;
    }

    void updatePetPosition(){
        if(petView==null||scene==null||petView.getWidth()<=0)return;
        float[] r=imageRect();
        if(r[2]<=0||r[3]<=0)return;

        petNX=clamp01(petNX);
        petNY=clamp01(petNY);

        float left=r[0]+petNX*r[2]-petView.getWidth()/2f;
        float feet=r[1]+petNY*r[3];
        float top=feet-petView.getHeight();

        petView.setX(left);
        petView.setY(top);

        float s=depthScale();
        // Gauche et droite utilisent le même strip SIDE.
        // Le déplacement vers la droite est uniquement un miroir horizontal.
        float sign=1f;
        if(walking && walkMode==WalkMode.SIDE){
            sign=travelDirection==TravelDirection.RIGHT?-1f:1f;
        }
        petView.setScaleX(sign*s);
        petView.setScaleY(s);
        petView.setAlpha(1f);
        petView.setVisibility(View.VISIBLE);
        petView.bringToFront();
        incidentView.bringToFront();
        moodLabel.bringToFront();
        cleanHint.bringToFront();
    }

    String moodText(){
        if(stage()==Stage.ENDED)return "Paisible";
        if(sleeping)return "Endormi";
        if(!incident.isEmpty())return "Inquiet";
        if(energy<18)return "Épuisé";
        if(hunger<18)return "Affamé";
        if(thirst<18)return "Assoiffé";
        if(clean<18)return "Sale";
        if(affection<20)return "En manque de câlins";
        if(happy<25)return "Triste";
        if(System.currentTimeMillis()<manualUntil && manualFrame==11)return "Très content";
        if(happy>78&&affection>70)return "Très heureux";
        if(happy>58)return "Content";
        return "Calme";
    }

    boolean strongEmotion(){
        return !incident.isEmpty() || hunger<25 || thirst<25 || clean<25 || energy<22 || happy<35;
    }

    void petLeopard(){
        if(stage()==Stage.ENDED)return;

        if(sleeping){
            wakeUp("Réveillé par le joueur",true);
            return;
        }

        affection=clamp(affection+12);
        happy=clamp(happy+6);

        if(!strongEmotion()){
            if(petStage()==PetStage.CUB)showCubFaceMoodNow(rnd.nextBoolean()?7:8,2200);
            else showAction(11,2200);
            toast("❤️ "+pet+" adore la caresse !");
        } else {
            if(petStage()==PetStage.CUB)showCubFaceMoodNow(chooseCubFaceMoodIndex(),1700);
            toast("🤍 "+pet+" apprécie la caresse, mais "+moodText().toLowerCase(Locale.ROOT)+".");
        }
        addHistory("Caresse donnée à "+pet+".");
        save();
        refresh();
    }

    void roomsMenu(){
        String[] rooms={"🛋️ Salon","🍽️ Cuisine","🛁 Salle de bain","🌿 Jardin"};
        new AlertDialog.Builder(this).setTitle("Choisir une pièce").setItems(rooms,(d,w)->{
            wakeForAction();
            room=w==1?"cuisine":w==2?"bain":w==3?"jardin":"salon";
            addHistory("Déplacement vers : "+roomName()+".");
            resetPetForRoom(true);
            save();
            refresh();
        }).show();
    }

    void actionsMenu(){
        String[] actions={"⚠ Punir","🧽 Nettoyer"};
        new AlertDialog.Builder(this).setTitle("Actions").setItems(actions,(d,w)->{
            if(w==0)punish();
            else startCleaning();
        }).show();
    }

    void startCleaning(){
        if(incident.isEmpty()){
            toast("Il n’y a aucune bêtise à nettoyer.");
            return;
        }
        cleaningMode=true;
        cleanProgress=0;
        cleanHint.setVisibility(View.VISIBLE);
        incidentView.setAlpha(1f);
        incidentView.setScaleX(1f);
        incidentView.setScaleY(1f);
        toast("Frotte avec ton doigt directement sur la bêtise.");
    }

    boolean handleRub(MotionEvent e){
        if(!cleaningMode)return true;
        float x=e.getX(),y=e.getY();
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            lastRubX=x;lastRubY=y;
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE){
            float dx=x-lastRubX,dy=y-lastRubY;
            cleanProgress+=(float)Math.sqrt(dx*dx+dy*dy);
            lastRubX=x;lastRubY=y;

            float threshold=dp(360);
            float p=Math.min(1f,cleanProgress/threshold);
            incidentView.setAlpha(1f-.65f*p);
            incidentView.setScaleX(1f-.30f*p);
            incidentView.setScaleY(1f-.30f*p);

            if(cleanProgress>=threshold)finishCleaning();
            return true;
        }
        return true;
    }

    void finishCleaning(){
        cleaningMode=false;
        cleanProgress=0;
        cleanHint.setVisibility(View.GONE);
        String old=incident;
        incident="";
        clean=clamp(clean+20);
        happy=clamp(happy+2);
        skillClean=clamp(skillClean+3);
        stars+=1;
        save();
        refreshIncident();
        refresh();
        toast("✨ Bêtise nettoyée ! +1 ★");
        addHistory("Bêtise nettoyée : "+old+".");
    }

    void refreshIncident(){
        if(incident.isEmpty()){
            incidentView.setVisibility(View.GONE);
            cleanHint.setVisibility(View.GONE);
            cleaningMode=false;
            return;
        }
        incidentView.setText(objects.incidentIcon(incident));
        incidentView.setVisibility(View.VISIBLE);
        if(!cleaningMode){
            incidentView.setAlpha(1f);
            incidentView.setScaleX(1f);
            incidentView.setScaleY(1f);
        }
    }

    void punish(){
        if(stage()==Stage.ENDED)return;
        wakeForAction();
        if(!incident.isEmpty()){
            skillObedience=clamp(skillObedience+4);
            skillCare=clamp(skillCare+1);
            happy=clamp(happy-4);
            affection=clamp(affection-2);
            showAction(3,1800);
            toast("⚠ La punition est justifiée. La bêtise doit encore être nettoyée.");
            addHistory("Punition justifiée après une bêtise.");
        } else {
            happy=clamp(happy-20);
            affection=clamp(affection-14);
            skillObedience=clamp(skillObedience-1);
            showAction(3,2600);
            toast("😢 Punition injuste : son bonheur et ses câlins baissent.");
            addHistory("Punition injuste.");
        }
        save();
        refresh();
    }

    void maybeMischief(){
        if(stage()==Stage.ENDED || !incident.isEmpty() || sleeping)return;

        long now=System.currentTimeMillis();
        if(nextMischiefAt==0){
            nextMischiefAt=now+(40+rnd.nextInt(51))*1000L;
            save();
            return;
        }
        if(now<nextMischiefAt)return;
        nextMischiefAt=now+(55+rnd.nextInt(66))*1000L;

        float base=stage()==Stage.CUB?.42f:stage()==Stage.TEEN?.29f:stage()==Stage.ADULT?.15f:.07f;
        float learning=(skillObedience+skillCare+skillClean)/300f;
        float chance=base*(1f-.70f*learning);
        if(happy<35)chance+=.08f;
        if(affection<30)chance+=.06f;
        if(clean<30)chance+=.05f;

        if(rnd.nextFloat()<chance){
            incident=objects.mischief();
            clean=clamp(clean-4);
            happy=clamp(happy-2);
            showAction(3,1200);
            toast("⚠ "+pet+" "+incident+" !");
            addHistory(pet+" "+incident+".");
            refreshIncident();
        } else {
            skillObedience=clamp(skillObedience+.15f);
            skillCare=clamp(skillCare+.10f);
        }
        save();
    }

    void animateAuto(){
        if(scene==null||petView==null)return;
        syncVisualStage();
        idleTick++;
        long now=System.currentTimeMillis();

        if(sleeping){
            currentPetRes=sleepDrawable();
            if(!setPetDrawableSafely(petStage(),currentPetRes))return;
            walking=false;
            updatePetPosition();
            return;
        }

        if(petStage()==PetStage.CUB && activeFaceMood>=0 && now<faceMoodUntil){
            ensurePetImage();
            updatePetPosition();
            return;
        }

        if(now<manualUntil){
            applyPose(manualFrame);
            return;
        }

        if(stage()==Stage.ENDED){
            applyPose(9);
            return;
        }

        if(energy<13){
            applyPose(9);
            return;
        }

        if(!incident.isEmpty()){
            applyPose(3);
            return;
        }

        if(walking){
            float dx=targetNX-petNX;
            float dy=targetNY-petNY;
            float dist=(float)Math.sqrt(dx*dx+dy*dy);

            if(dist<.012f){
                petNX=targetNX;
                petNY=targetNY;
                if(targetNodeIndex>=0)petNodeIndex=targetNodeIndex;
                walking=false;
                walkMode=WalkMode.SIDE;
                currentPetRes=0;
                ensurePetImage();
                updatePetPosition();
                nextWalkAt=now+1200+rnd.nextInt(3000);
                return;
            }

            float speed=stage()==Stage.OLD?.0048f:stage()==Stage.CUB?.0066f:.0075f;
            petNX+=dx/dist*speed;
            petNY+=dy/dist*speed;

            showWalkFrame(walkMode);
            updatePetPosition();
            return;
        }

        maybeShowFaceMood(now);
        currentPetRes=0;
        ensurePetImage();
        updatePetPosition();

        if(nextWalkAt==0)nextWalkAt=now+1000+rnd.nextInt(1800);
        if(now>=nextWalkAt && energy>22){
            int chance=stage()==Stage.OLD?35:75;
            if(rnd.nextInt(100)<chance){
                chooseWalkTarget();
            }else{
                nextWalkAt=now+1200+rnd.nextInt(2200);
            }
        }
    }

    void applyPose(int frame){
        if(sleeping){
            currentPetRes=sleepDrawable();
            if(!setPetDrawableSafely(petStage(),currentPetRes))return;
            updatePetPosition();
            return;
        }

        int staticRes;
        if(frame==9)staticRes=sleepDrawable();
        else if(frame==11)staticRes=happyDrawable();
        else if(frame==3)staticRes=tiredDrawable();
        else staticRes=emotionDrawable();

        currentPetRes=staticRes;
        if(!setPetDrawableSafely(petStage(),staticRes))return;
        updatePetPosition();

        float base=depthScale();
        float sx=base;
        float sy=base;

        petView.setAlpha(1f);
        petView.setRotation(0f);

        if(frame==3){
            petView.setAlpha(.92f);
            sy*=.97f;
        } else if(frame==9){
            sy*=.96f;
        } else if(frame==10){
            sx*=1.05f; sy*=1.05f;
            petView.setRotation((idleTick%2==0)?-3f:3f);
        } else if(frame==11){
            sx*=1.06f; sy*=1.06f;
        } else if(idleTick%5==0){
            petView.setRotation((idleTick%2==0)?-1.5f:1.5f);
        }

        petView.setScaleX(sx);
        petView.setScaleY(sy);
    }

    void showAction(int frame,int duration){
        manualFrame=frame;
        manualUntil=System.currentTimeMillis()+duration;
        faceMoodUntil=0;
        activeFaceMood=-1;
        walking=false;
        walkMode=WalkMode.SIDE;
        travelDirection=TravelDirection.LEFT;
        currentPetRes=0;
        applyPose(frame);
    }

    void act(String msg,int frame,float h,float w,float c,float af,float joy,float e,int gainStars){
        wakeForAction();
        hunger=clamp(hunger+h);
        thirst=clamp(thirst+w);
        clean=clamp(clean+c);
        affection=clamp(affection+af);
        happy=clamp(happy+joy);
        energy=clamp(energy+e);
        stars+=gainStars;
        showAction(frame,2200);
        save();
        refresh();
        toast(msg);
        addHistory("Action : "+msg+".");
    }

    void competition(){
        wakeForAction();
        if(stage()!=Stage.ADULT){
            toast("Les concours sont réservés à l’adulte.");
            return;
        }
        int skillBonus=Math.round((skillObedience+skillCare)/8f);
        int score=Math.max(0,Math.min(100,Math.round((happy+energy+clean)/3)+skillBonus+rnd.nextInt(31)-15));
        int reward=score>=82?10:score>=68?6:score>=55?3:1;
        stars+=reward;
        happy=clamp(happy+12);
        affection=clamp(affection+4);
        energy=clamp(energy-22);
        thirst=clamp(thirst-12);
        skillObedience=clamp(skillObedience+1);
        skillCare=clamp(skillCare+1);
        showAction(10,2400);
        save();
        refresh();
        addHistory("Concours : "+score+"/100, +"+reward+" ★.");
        new AlertDialog.Builder(this).setTitle("Concours")
            .setMessage("Score : "+score+"/100\nRécompense : +"+reward+" ★")
            .setPositiveButton("OK",null).show();
    }

    void rename(boolean first){
        EditText e=new EditText(this);
        e.setSingleLine();
        e.setText(first?"":pet);
        new AlertDialog.Builder(this)
            .setTitle(first?"Bienvenue !":"Changer le nom")
            .setMessage(first?"Donne un nom à ton léopardeau.":null)
            .setView(e)
            .setPositiveButton("Valider",(d,w)->{
                String n=e.getText().toString().trim();
                pet=n.isEmpty()?"Léo":n;
                sp.edit().putBoolean("named",true).apply();
                if(first)recordAdoption(pet);
                save();
                refresh();
            })
            .setNegativeButton(first?"Léo":"Annuler",(d,w)->{
                if(first){
                    pet="Léo";
                    sp.edit().putBoolean("named",true).apply();
                    recordAdoption(pet);
                    save();
                    refresh();
                }
            })
            .setCancelable(!first).show();
    }

    void endLife(){
        if(endShown||isFinishing())return;
        endShown=true;
        roomsBtn.setEnabled(false);
        objectsBtn.setEnabled(false);
        actionsBtn.setEnabled(false);

        AlertDialog a=new AlertDialog.Builder(this)
            .setTitle("Une belle vie")
            .setMessage(pet+" a terminé son cycle de 13 heures réelles.\n\nTu peux maintenant adopter un nouveau léopardeau.")
            .setPositiveButton("Adopter",(d,w)->newGeneration())
            .setCancelable(false)
            .create();
        a.setOnDismissListener(d->endShown=false);
        a.show();
    }

    void newGeneration(){
        long n=System.currentTimeMillis();
        generation++;
        born=last=n;
        hunger=85;thirst=85;clean=90;affection=90;happy=90;energy=90;
        skillClean=5;skillObedience=5;skillCare=5;
        incident="";
        nextMischiefAt=0;
        room="salon";
        currentPetRes=0;
        releaseWalkFrames();
        sleeping=false;
        sleepEndAt=0;
        walkMode=WalkMode.SIDE;
        travelDirection=TravelDirection.LEFT;
        faceMoodUntil=0;
        nextFaceMoodAt=0;
        activeFaceMood=-1;
        nextAutoSleepAt=n+(4+rnd.nextInt(4))*60000L;
        walking=false;
        roomsBtn.setEnabled(true);
        objectsBtn.setEnabled(true);
        actionsBtn.setEnabled(true);
        resetPetForRoom(false);
        save();
        refresh();
        rename(true);
    }

    void maybeAutoSleep(){
        if(stage()==Stage.ENDED)return;
        long now=System.currentTimeMillis();

        if(sleeping){
            if(now>=sleepEndAt)wakeUp("Réveil naturel",false);
            return;
        }

        if(!incident.isEmpty() || now<manualUntil || walking)return;

        // Fatigue forte : sommeil immédiat.
        if(energy<=35){
            beginAutoSleep();
            return;
        }

        // Sinon le léopard peut choisir de dormir de lui-même.
        if(now>=nextAutoSleepAt){
            if(energy<82){
                beginAutoSleep();
            }else{
                nextAutoSleepAt=now+90000L+rnd.nextInt(90000);
            }
        }
    }

    void beginAutoSleep(){
        if(sleeping||stage()==Stage.ENDED)return;
        sleeping=true;
        walking=false;
        walkMode=WalkMode.SIDE;
        manualUntil=0;
        sleepEndAt=System.currentTimeMillis()+90000L;
        currentPetRes=sleepDrawable();
        if(!setPetDrawableSafely(petStage(),currentPetRes))return;
        addHistory(pet+" s'est endormi pour 1 min 30.");
        save();
        refresh();
    }

    void wakeUp(String reason,boolean notify){
        if(!sleeping)return;
        sleeping=false;
        sleepEndAt=0;
        walkMode=WalkMode.SIDE;
        nextAutoSleepAt=System.currentTimeMillis()+(4+rnd.nextInt(4))*60000L;
        currentPetRes=0;
        addHistory(reason+".");
        ensurePetImage();
        if(notify)toast("😺 "+pet+" se réveille.");
        save();
        refresh();
    }

    void wakeForAction(){
        if(sleeping)wakeUp("Réveillé par une action",false);
    }

    List<String> splitLog(String raw){
        ArrayList<String> out=new ArrayList<>();
        if(raw==null||raw.isEmpty())return out;
        for(String s:raw.split("\u001E")){
            if(s!=null&&!s.trim().isEmpty())out.add(s);
        }
        return out;
    }

    String joinLog(List<String> list){
        StringBuilder b=new StringBuilder();
        for(String s:list){
            if(b.length()>0)b.append('\u001E');
            b.append(s.replace("\u001E"," "));
        }
        return b.toString();
    }

    String historyTime(){
        return new java.text.SimpleDateFormat("HH:mm",Locale.FRANCE).format(new Date());
    }

    void addHistory(String event){
        List<String> list=splitLog(historyLog);
        list.add(historyTime()+" — "+event);
        while(list.size()>60)list.remove(0);
        historyLog=joinLog(list);
    }

    void recordAdoption(String name){
        List<String> list=splitLog(adoptedLog);
        String prefix="G"+generation+" — ";
        boolean updated=false;
        for(int i=0;i<list.size();i++){
            if(list.get(i).startsWith(prefix)){
                list.set(i,prefix+name);
                updated=true;
                break;
            }
        }
        if(!updated){
            list.add(prefix+name);
            addHistory("Adoption de "+name+" (génération "+generation+").");
        }
        while(list.size()>10)list.remove(0);
        adoptedLog=joinLog(list);
    }

    void ensureCurrentAdoptionRecorded(){
        recordAdoption(pet);
        save();
    }

    void showTopMenu(){
        String[] entries={"📜 Historique"};
        new AlertDialog.Builder(this).setTitle("Menu").setItems(entries,(d,w)->showHistory()).show();
    }

    void showHistory(){
        List<String> adopted=splitLog(adoptedLog);
        List<String> events=splitLog(historyLog);

        StringBuilder b=new StringBuilder();
        b.append("Léopards adoptés (").append(adopted.size()).append("/10)\n");
        if(adopted.isEmpty())b.append("Aucun\n");
        else for(int i=adopted.size()-1;i>=0;i--)b.append("• ").append(adopted.get(i)).append("\n");

        b.append("\nHistorique récent\n");
        if(events.isEmpty())b.append("Aucun événement.");
        else{
            int start=Math.max(0,events.size()-30);
            for(int i=events.size()-1;i>=start;i--)b.append("• ").append(events.get(i)).append("\n");
        }

        TextView t=text(14,false);
        t.setText(b.toString());
        t.setPadding(dp(18),dp(12),dp(18),dp(12));

        ScrollView scroll=new ScrollView(this);
        scroll.addView(t);

        new AlertDialog.Builder(this)
            .setTitle("Historique")
            .setView(scroll)
            .setPositiveButton("Fermer",null)
            .show();
    }

    void markCharacterAssetInvalid(int res,String label,String reason){
        invalidCharacterAssets.add(res);
        addHistory("ERREUR asset personnage ["+label+"] : "+reason+".");
    }

    void showAssetErrorOnce(){
        if(assetErrorShown)return;
        assetErrorShown=true;
        toast("⚠ Pack de sprites invalide : animation désactivée, aucun autre âge ne sera utilisé.");
    }

    boolean isMoodResourceForStage(PetStage stage,int res){
        return CharacterSprites.forStage(stage).ownsMood(res);
    }

    boolean setPetDrawableSafely(PetStage expectedStage,int res){
        if(expectedStage!=petStage() || !isMoodResourceForStage(expectedStage,res)){
            addHistory("ERREUR mélange d'âge bloqué sur une pose ("+expectedStage+").");
            petView.setVisibility(View.INVISIBLE);
            showAssetErrorOnce();
            return false;
        }
        if(invalidCharacterAssets.contains(res)){
            petView.setVisibility(View.INVISIBLE);
            showAssetErrorOnce();
            return false;
        }
        try{
            petView.setImageResource(res);
            petView.setVisibility(View.VISIBLE);
            return true;
        }catch(Throwable err){
            markCharacterAssetInvalid(res,"pose","chargement impossible");
            petView.setVisibility(View.INVISIBLE);
            showAssetErrorOnce();
            return false;
        }
    }

    void validateCharacterAssets(){
        invalidCharacterAssets.clear();
        assetErrorShown=false;

        HashMap<Integer,PetStage> owners=new HashMap<>();

        for(PetStage stage:PetStage.values()){
            CharacterSprites.Pack pack=CharacterSprites.forStage(stage);

            // Une ressource graphique ne peut appartenir qu'à un seul âge.
            for(int res:pack.allResources()){
                if(res==0)continue;
                PetStage previous=owners.put(res,stage);
                if(previous!=null && previous!=stage){
                    markCharacterAssetInvalid(res,stage+" PACK",
                        "ressource déjà attribuée à "+previous+" : partage inter-âge interdit");
                }
            }

            int poseW=-1,poseH=-1;
            for(PetMood mood:PetMood.values()){
                int res=pack.mood(mood);
                Bitmap b=null;
                try{b=BitmapFactory.decodeResource(getResources(),res);}catch(Throwable ignored){}
                if(b==null){
                    markCharacterAssetInvalid(res,stage+" "+mood,"ressource manquante ou illisible");
                    continue;
                }

                int w=b.getWidth(),h=b.getHeight();
                if(poseW<0){poseW=w;poseH=h;}
                if(w!=poseW || h!=poseH || w!=640 || h!=640){
                    markCharacterAssetInvalid(res,stage+" "+mood,
                        w+"x"+h+" au lieu du canevas commun 640x640");
                }
                b.recycle();
            }

            for(WalkMode mode:WalkMode.values()){
                int res=pack.walk(mode);
                Bitmap b=null;
                try{b=BitmapFactory.decodeResource(getResources(),res);}catch(Throwable ignored){}
                if(b==null){
                    markCharacterAssetInvalid(res,stage+" "+mode,"strip manquant ou illisible");
                    continue;
                }

                int w=b.getWidth(),h=b.getHeight();
                int expectedFrames=pack.frameCount(mode);
                boolean invalid=h!=640 || w!=640*expectedFrames;
                if(invalid){
                    markCharacterAssetInvalid(res,stage+" "+mode,
                        w+"x"+h+" : attendu "+expectedFrames+" frames de 640x640");
                }
                b.recycle();
            }

            // Les humeurs face-joueur sont propres au pack qui les possède.
            // Aucun âge ne peut emprunter la planche d'un autre.
            if(pack.hasFaceMoods()){
                Bitmap face=null;
                try{face=BitmapFactory.decodeResource(getResources(),pack.faceMoods);}catch(Throwable ignored){}
                if(face==null){
                    markCharacterAssetInvalid(pack.faceMoods,stage+" FACE_MOODS",
                        "ressource manquante ou illisible");
                }else{
                    int fw=face.getWidth(),fh=face.getHeight();
                    int expectedW=pack.faceFrameSize*pack.faceFrameCount;
                    if(fw!=expectedW || fh!=pack.faceFrameSize){
                        markCharacterAssetInvalid(pack.faceMoods,stage+" FACE_MOODS",
                            fw+"x"+fh+" : attendu "+pack.faceFrameCount+
                            " frames de "+pack.faceFrameSize+"x"+pack.faceFrameSize);
                    }
                    face.recycle();
                }
            }
        }

        if(!invalidCharacterAssets.isEmpty())showAssetErrorOnce();
    }

    String format(long ms){
        long s=Math.max(0,ms/1000);
        long h=s/3600,m=(s%3600)/60,sec=s%60;
        return h>0?String.format(Locale.FRANCE,"%dh %02d:%02d",h,m,sec):String.format(Locale.FRANCE,"%02d:%02d",m,sec);
    }

    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
