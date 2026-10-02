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
    static final long MIN_SLEEP_MS=90000L;
    static final long MOOD_DURATION_MS=4000L;
    static final long DIRECTIONAL_IDLE_MS=700L;
    // Minimum share of AWAKE visible time reserved for static IDLE DOWN (30%).
    // Face moods count because they are always rendered on the full-body idle-down pose.
    static final float MIN_IDLE_DOWN_SHARE=.30f;

    final Handler handler=new Handler(Looper.getMainLooper());
    final Random rnd=new Random();

    SharedPreferences sp;
    ObjectSystem objects;
    LivingRoomGames games;
    GardenGames gardenGames;
    KitchenWaterSystem kitchenWater;
    int profileSlot=-1;
    String petSex="";
    boolean internalTransition=false,resumeNeedsChooser=false;

    long born,last,nextMischiefAt=0,nextWalkAt=0,manualUntil=0,sleepEndAt=0,nextAutoSleepAt=0,walkStartedAt=0;
    long directionalIdleUntil=0,faceRecoveryUntil=0,actionUntil=0,actionFrameAt=0,actionStartedAt=0;
    float hunger=85,thirst=85,clean=90,affection=90,happy=90,energy=90;
    float waterBowl=0f;
    float skillClean=5,skillObedience=5,skillCare=5;
    int stars=0,generation=1;
    String pet="Léo",room="salon",incident="",incidentRoom="";
    String historyLog="",adoptedLog="";
    boolean endShown=false,cleaningMode=false,sleeping=false,walking=false,callingToForeground=false;
    float cleanProgress=0,lastRubX=0,lastRubY=0;
    float incidentNX=.50f,incidentNY=.85f;
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
    PetStage displayedPetStage=null;
    PetStage loadedWalkStage=null;
    WalkMode loadedWalkMode=null;
    TravelDirection travelDirection=TravelDirection.LEFT;

    Bitmap faceMoodStrip=null;
    Bitmap[] faceMoodFrames=null;
    long faceMoodUntil=0,nextFaceMoodAt=0;
    boolean moodApproach=false,moodExitUp=false;
    int pendingFaceMood=-1;
    int activeFaceMood=-1;
    int queuedFaceMood=-1;
    float callingEffectFactor=1f;
    TravelDirection idleDirection=TravelDirection.DOWN;
    ActionAnim actionAnim=ActionAnim.NONE;
    Bitmap actionStrip=null;
    Bitmap[] actionFrames=null;
    int actionStripRes=0,actionFrameIndex=0;
    int specialPoseRes=0;
    PetStage specialPoseStage=null;

    final Set<Integer> invalidCharacterAssets=new HashSet<>();
    boolean assetErrorShown=false;

    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}
    enum PetStage {CUB,TEEN,ADULT,OLD}
    enum PetMood {IDLE,HAPPY,TIRED,SLEEP}
    enum WalkMode {SIDE,FRONT,BACK}
    enum TravelDirection {LEFT,RIGHT,UP,DOWN}
    enum ActionAnim {NONE,EAT,JUMP,SLEEP}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        PetProfileStore.ensureMigrated(this);
        profileSlot=getIntent().getIntExtra(PetProfileStore.EXTRA_SLOT,-1);
        if(!PetProfileStore.validSlot(profileSlot)||!PetProfileStore.exists(this,profileSlot)){
            Intent chooser=new Intent(this,PetChooserActivity.class);
            startActivity(chooser);
            finish();
            return;
        }
        sp=getSharedPreferences(PetProfileStore.petPrefsName(profileSlot),MODE_PRIVATE);
        PetBehavior.ensurePersonality(sp,profileSlot);
        load();
        if(sp.getBoolean("named",false))ensureCurrentAdoptionRecorded();
        objects=new ObjectSystem(this);
        build();
        validateCharacterAssets();
        tickNeeds();
        refresh();
    }

    @Override protected void onResume(){
        super.onResume();
        if(sp==null)return;
        if(resumeNeedsChooser){
            resumeNeedsChooser=false;
            openPetChooser();
            return;
        }
        internalTransition=false;
        tickNeeds();
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(animator);
        handler.post(ticker);
        handler.post(animator);
    }

    @Override protected void onPause(){
        super.onPause();
        if(sp==null)return;
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(animator);
        tickNeeds();
        save();
    }

    @Override protected void onStop(){
        super.onStop();
        if(sp==null||isFinishing())return;
        if(!internalTransition)resumeNeedsChooser=true;
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
        pet=sp.getString("name",PetProfileStore.name(this,profileSlot));
        petSex=PetProfileStore.sex(this,profileSlot);
        room=sp.getString("room","salon");
        incident=sp.getString("incident","");
        incidentRoom=sp.getString("incidentRoom",incident.isEmpty()?"":room);
        incidentNX=sp.getFloat("incidentNX",.50f);
        incidentNY=sp.getFloat("incidentNY",.85f);
        nextMischiefAt=sp.getLong("nextMischiefAt",0);
        sleeping=sp.getBoolean("sleeping",false);
        sleepEndAt=sp.getLong("sleepEndAt",0);
        nextAutoSleepAt=sp.getLong("nextAutoSleepAt",0);
        historyLog=sp.getString("historyLog","");
        adoptedLog=sp.getString("adoptedLog","");
        waterBowl=sp.getFloat("waterBowl",0f);

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
          .putString("incidentRoom",incidentRoom)
          .putFloat("incidentNX",incidentNX).putFloat("incidentNY",incidentNY)
          .putLong("nextMischiefAt",nextMischiefAt)
          .putBoolean("sleeping",sleeping)
          .putLong("sleepEndAt",sleepEndAt)
          .putLong("nextAutoSleepAt",nextAutoSleepAt)
          .putString("historyLog",historyLog)
          .putString("adoptedLog",adoptedLog)
          .putFloat("waterBowl",waterBowl)
          .apply();
    }

    float clamp(float v){return Math.max(0,Math.min(100,v));}

    boolean promenadeAway(){
        return promenadeAwayAt(System.currentTimeMillis());
    }

    boolean promenadeAwayAt(long now){
        if(sp==null)return false;
        boolean active=sp.getBoolean("promenadeActive",false);
        long start=sp.getLong("promenadeStart",0L);
        if(!active||start<=0L)return false;
        if(now-start>=PromenadeActivity.DURATION_MS){
            sp.edit().putBoolean("promenadeActive",false).apply();
            currentPetRes=0;
            return false;
        }
        return true;
    }

    long promenadeAwayOverlap(long from,long to){
        if(sp==null||to<=from)return 0L;
        long start=sp.getLong("promenadeStart",0L);
        if(start<=0L)return 0L;
        long end=start+PromenadeActivity.DURATION_MS;
        long a=Math.max(from,start);
        long b=Math.min(to,end);
        return Math.max(0L,b-a);
    }

    float beginRepeatedAction(String family){
        float factor=PetBehavior.registerRepeat(sp,family);
        String note=PetBehavior.boredomText(pet,factor);
        if(!note.isEmpty())toast(note);
        return factor;
    }

    void queueFaceMood(int mood){
        queuedFaceMood=Math.max(0,Math.min(11,mood));
    }

    void applyNeedDelta(float h,float w,float c,float af,float joy,float e,int gainStars,float factor){
        hunger=clamp(hunger+PetBehavior.positive(h,factor));
        thirst=clamp(thirst+PetBehavior.positive(w,factor));
        clean=clamp(clean+PetBehavior.positive(c,factor));
        affection=clamp(affection+PetBehavior.positive(af,factor));
        happy=clamp(happy+PetBehavior.positive(joy,factor));
        energy=clamp(energy+PetBehavior.positive(e,factor));
        stars+=PetBehavior.rewardStars(gainStars,factor);
    }

    void queuePreferenceReaction(PetBehavior.Preference preference){
        if(preference==PetBehavior.Preference.DISLIKE)queueFaceMood(1);
        else if(preference==PetBehavior.Preference.LOVE)queueFaceMood(rnd.nextBoolean()?0:8);
    }

    void performItemAction(ObjectSystem.Item item,String family,String animation){
        if(item==null)return;
        float factor=beginRepeatedAction(family);
        float h=item.hunger,w=item.water,c=item.clean,af=item.affection,joy=item.happy,e=item.energy;
        PetBehavior.Preference preference=PetBehavior.Preference.NEUTRAL;

        boolean food="food".equals(item.kind)||"snack".equals(item.kind)||"treat".equals(item.kind);
        if(food){
            preference=PetBehavior.foodPreference(sp,item.id);
            // Manger salit davantage l'animal, même si l'aliment est neutre.
            c-=("snack".equals(item.kind)||"treat".equals(item.kind))?1.5f:3f;
            if(preference==PetBehavior.Preference.LOVE){
                h*=1.35f;
                joy+=6f;
            }else if(preference==PetBehavior.Preference.DISLIKE){
                h*=.85f;
                joy=-7f;
            }
        }

        wakeForAction();
        applyNeedDelta(h,w,c,af,joy,e,item.stars,factor);
        if("groom".equals(item.id)||"comb".equals(item.id))
            skillClean=clamp(skillClean+1.2f*factor);
        if("care".equals(family))skillCare=clamp(skillCare+.35f*factor);

        int special=specialPoseResource(animation);
        if(special!=0)startSpecialPose(special,"bottle".equals(animation)?3000L:2600L);
        else if("eat".equals(animation))startActionAnimation(ActionAnim.EAT,3000L);
        else if("jump".equals(animation))startActionAnimation(ActionAnim.JUMP,2200L);
        else showAction(item.frame,2200);

        queuePreferenceReaction(preference);
        save();
        refresh();

        String boredom=PetBehavior.boredomText(pet,factor);
        if(!boredom.isEmpty())toast(boredom);
        else if(preference==PetBehavior.Preference.LOVE)toast("😍 "+pet+" adore "+item.name.toLowerCase(Locale.ROOT)+" !");
        else if(preference==PetBehavior.Preference.DISLIKE)toast("😠 "+pet+" n’aime pas "+item.name.toLowerCase(Locale.ROOT)+".");
        else toast(item.name);
        addHistory("Action : "+item.name+".");
    }

    void applyToyRewards(ObjectSystem.Item item,float repetitionFactor,float playFactor){
        if(item==null)return;
        float duration=Math.max(0f,Math.min(1f,playFactor));
        float h=item.hunger*duration;
        float w=item.water*duration;
        float c=item.clean*duration;
        float af=item.affection*duration;
        float joy=item.happy*duration;
        float e=item.energy*duration;

        PetBehavior.Preference preference=PetBehavior.toyPreference(sp,item.id);
        if(preference==PetBehavior.Preference.LOVE){
            joy=joy*1.35f+3f*duration;
            if("plush".equals(item.id))af*=1.25f;
        }else if(preference==PetBehavior.Preference.DISLIKE){
            joy=-6f*duration;
            if("plush".equals(item.id))af=0f;
        }

        applyNeedDelta(h,w,c,af,joy,e,item.stars,repetitionFactor);
        skillCare=clamp(skillCare+.6f*repetitionFactor*duration);
        queuePreferenceReaction(preference);
    }

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
                // 90 secondes complètes garantissent une barre Sommeil pleine.
                energy+=100f*(sleepMs/(float)MIN_SLEEP_MS);
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
                    clean-=.16f*m;
                    happy-=.04f*m;
                }

                long awayMs=promenadeAwayOverlap(start,n);
                long homeAwakeMs=Math.max(0L,awakeMs-awayMs);
                if(homeAwakeMs>0L && waterBowl>.05f && thirst<100f){
                    float drinkCapacity=(homeAwakeMs/60000f)*6f;
                    float drink=Math.min(waterBowl,Math.min(100f-thirst,drinkCapacity));
                    if(drink>0f){
                        thirst+=drink;
                        waterBowl=Math.max(0f,waterBowl-drink);
                    }
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

        promenadeAwayAt(n);
        hunger=clamp(hunger);thirst=clamp(thirst);clean=clamp(clean);
        affection=clamp(affection);happy=clamp(happy);energy=clamp(energy);
        if(waterBowl<.05f)waterBowl=0f;
        if(kitchenWater!=null)kitchenWater.refreshVisibility();

        if(sleeping && n>=sleepEndAt){
            sleeping=false;
            sleepEndAt=0;
            energy=100f;
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
        incidentView.setTextSize(32);
        incidentView.setGravity(Gravity.CENTER);
        incidentView.setVisibility(View.GONE);
        incidentView.setBackground(null);
        incidentView.setPadding(0,0,0,0);
        incidentView.setElevation(dp(8));
        FrameLayout.LayoutParams incidentParams=new FrameLayout.LayoutParams(dp(60),dp(60));
        incidentParams.gravity=Gravity.TOP|Gravity.LEFT;
        scene.addView(incidentView,incidentParams);
        incidentView.setOnTouchListener((v,e)->handleRub(e));

        games=new LivingRoomGames(this);
        games.install();
        gardenGames=new GardenGames(this);
        gardenGames.install();
        kitchenWater=new KitchenWaterSystem(this);
        kitchenWater.install();

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
        if(gardenGames!=null)gardenGames.refreshVisibility();
        if(kitchenWater!=null)kitchenWater.refreshVisibility();
        scene.post(()->{
            fitSceneAndPet();
            if(gardenGames!=null)gardenGames.refreshVisibility();
            if(kitchenWater!=null)kitchenWater.refreshVisibility();
        });
    }

    void ensurePetImage(){
        syncVisualStage();
        long now=System.currentTimeMillis();
        if(promenadeAwayAt(now)){
            petView.setVisibility(View.INVISIBLE);
            return;
        }

        if(specialPoseRes!=0){
            if(now<manualUntil && specialPoseStage==petStage() && renderSpecialPose()){
                petView.setAlpha(1f);
                petView.setVisibility(View.VISIBLE);
                updatePetPosition();
                return;
            }
            clearSpecialPose();
        }

        if(actionAnim!=ActionAnim.NONE){
            showActionAnimationFrame(now);
            petView.setAlpha(1f);
            petView.setVisibility(displayedPetStage==petStage()
                    && petView.getDrawable()!=null?View.VISIBLE:View.INVISIBLE);
            updatePetPosition();
            return;
        }

        boolean faceActive=CharacterSprites.forStage(petStage()).hasFaceMoods()
                && !sleeping && !walking && activeFaceMood>=0 && now<faceMoodUntil;

        if(faceActive){
            if(loadFaceMoodFrames()){
                petView.setImageBitmap(faceMoodFrames[activeFaceMood]);
                displayedPetStage=petStage();
                currentPetRes=0;
            }else{
                faceActive=false;
                faceMoodUntil=0;
                activeFaceMood=-1;
            }
        }

        int res=0;
        if(!faceActive && !walking){
            if(now<directionalIdleUntil)res=idleDrawableForDirection(idleDirection);
            else res=idleDownDrawable();
        }

        if(res!=0 && res!=currentPetRes){
            if(!setPetDrawableSafely(petStage(),res))return;
            currentPetRes=res;
        }

        petView.setAlpha(1f);
        petView.setVisibility(displayedPetStage==petStage()
                && petView.getDrawable()!=null?View.VISIBLE:View.INVISIBLE);
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
        manualUntil=0;
        manualFrame=0;
        walking=false;
        walkMode=WalkMode.SIDE;
        travelDirection=TravelDirection.DOWN;
        idleDirection=TravelDirection.DOWN;
        directionalIdleUntil=0;
        faceRecoveryUntil=0;
        walkFrameIndex=0;
        releaseWalkFrames();
        releaseFaceMoodFrames();
        releaseActionFrames();
        clearSpecialPose();
        actionAnim=ActionAnim.NONE;
        faceMoodUntil=0;
        nextFaceMoodAt=0;
        activeFaceMood=-1;
        pendingFaceMood=-1;
        moodApproach=false;
        moodExitUp=false;
        currentPetRes=0;
        displayedPetStage=null;
        if(petView!=null){
            petView.setImageDrawable(null);
            petView.setRotation(0f);
            petView.setScaleX(1f);
            petView.setScaleY(1f);
        }
        if(previous!=null)addHistory("Verrouillage visuel : "+previous+" → "+now+".");
    }

    PetStage petStage(){
        Stage s=stage();
        if(s==Stage.CUB)return PetStage.CUB;
        if(s==Stage.TEEN)return PetStage.TEEN;
        if(s==Stage.ADULT)return PetStage.ADULT;
        // Fin de cycle : l'animal conserve son apparence OLD. Ce n'est pas un fallback d'asset.
        return PetStage.OLD;
    }

    int getWalkStrip(PetStage stage,WalkMode mode){
        if(games!=null && games.fastRun()){
            GameSprites.Pack game=GameSprites.forStage(stage);
            if(mode==WalkMode.FRONT)return game.runDown;
            if(mode==WalkMode.BACK)return game.runUp;
            return travelDirection==TravelDirection.RIGHT?game.runRight:game.runLeft;
        }

        CharacterSprites.Pack pack=CharacterSprites.forStage(stage);
        if(mode==WalkMode.FRONT)return pack.walkDown;
        if(mode==WalkMode.BACK)return pack.walkUp;
        return travelDirection==TravelDirection.RIGHT?pack.walkRight:pack.walkLeft;
    }

    int idleDownDrawable(){return CharacterSprites.forStage(petStage()).idleDown;}

    int idleDrawableForDirection(TravelDirection direction){
        return CharacterSprites.forStage(petStage()).idle(direction);
    }

    int idleDrawable(){return idleDownDrawable();}
    int happyDrawable(){return idleDownDrawable();}
    int tiredDrawable(){return idleDownDrawable();}
    int sleepDrawable(){return idleDownDrawable();}
    int emotionDrawable(){return idleDownDrawable();}

    int walkStripDrawable(WalkMode mode){return getWalkStrip(petStage(),mode);}
    int faceMoodStripRes(){return CharacterSprites.forStage(petStage()).moods;}

    void releaseFaceMoodFrames(){
        faceMoodFrames=null;
        faceMoodStrip=null;
    }

    boolean loadFaceMoodFrames(){
        CharacterSprites.Pack pack=CharacterSprites.forStage(petStage());
        if(!pack.hasFaceMoods())return false;
        int res=pack.moods;
        if(invalidCharacterAssets.contains(res)){
            showAssetErrorOnce();
            return false;
        }
        if(faceMoodFrames!=null && faceMoodFrames.length==12)return true;

        releaseFaceMoodFrames();
        Bitmap strip=null;
        try{strip=BitmapFactory.decodeResource(getResources(),res);}catch(Throwable ignored){}
        if(strip==null){
            markCharacterAssetInvalid(res,petStage()+" MOODS","ressource illisible");
            showAssetErrorOnce();
            return false;
        }

        final int frame=CharacterSprites.FRAME_SIZE;
        final int count=12;
        if(strip.getWidth()!=frame*count || strip.getHeight()!=frame){
            markCharacterAssetInvalid(res,petStage()+" MOODS",
                strip.getWidth()+"x"+strip.getHeight()+" au lieu de "+(frame*count)+"x"+frame);
            showAssetErrorOnce();
            return false;
        }

        faceMoodStrip=strip;
        faceMoodFrames=new Bitmap[count];
        try{
            for(int i=0;i<count;i++)
                faceMoodFrames[i]=Bitmap.createBitmap(faceMoodStrip,i*frame,0,frame,frame);
        }catch(Throwable err){
            releaseFaceMoodFrames();
            markCharacterAssetInvalid(res,petStage()+" MOODS","découpage impossible");
            showAssetErrorOnce();
            return false;
        }
        return true;
    }

    int chooseFaceMoodIndex(){
        // 0 sourire, 1 énervé, 2 triste, 3 surpris, 4 fatigué, 5 faim,
        // 6 amitié, 7 amoureux, 8 rire, 9 calme, 10 curieux, 11 anxieux.
        if(!incident.isEmpty())return rnd.nextBoolean()?1:11;
        if(energy<22)return 4;
        if(hunger<22 || thirst<18)return 5;
        if(happy<35)return 2;
        if(happy>85 && affection>80)return 7;
        if(happy>72)return rnd.nextBoolean()?0:8;
        if(affection>82)return 6;
        if(rnd.nextInt(4)==0)return 10;
        return 9;
    }

    void scheduleNextFaceMood(long now){
        nextFaceMoodAt=now+60000L+rnd.nextInt(60001);
    }

    void maybeShowFaceMood(long now){
        if(!CharacterSprites.FACE_ATLAS_REVIEWED)return;
        CharacterSprites.Pack pack=CharacterSprites.forStage(petStage());
        if(!pack.hasFaceMoods() || sleeping || walking || actionAnim!=ActionAnim.NONE
                || moodApproach || moodExitUp || now<manualUntil)return;
        if(nextFaceMoodAt==0){
            scheduleNextFaceMood(now);
            return;
        }
        if(now<nextFaceMoodAt)return;
        beginMoodApproach(chooseFaceMoodIndex());
    }

    int foregroundNode(){
        float[][] nodes=roomNodes();
        int best=0;
        float bestY=nodes[0][1];
        float bestCenter=Math.abs(nodes[0][0]-.50f);
        for(int i=1;i<nodes.length;i++){
            float y=nodes[i][1];
            float center=Math.abs(nodes[i][0]-.50f);
            if(y>bestY+.001f || (Math.abs(y-bestY)<=.001f && center<bestCenter)){
                best=i;bestY=y;bestCenter=center;
            }
        }
        return best;
    }

    int upwardNode(){
        float[][] nodes=roomNodes();
        int best=-1;
        float bestScore=Float.MAX_VALUE;
        for(int i=0;i<nodes.length;i++){
            float dy=petNY-nodes[i][1];
            if(dy<=.06f)continue;
            float score=Math.abs(nodes[i][0]-petNX)-dy*.35f;
            if(score<bestScore){bestScore=score;best=i;}
        }
        if(best>=0)return best;
        for(int i=0;i<nodes.length;i++)if(best<0||nodes[i][1]<nodes[best][1])best=i;
        return Math.max(0,best);
    }

    void beginMoodApproach(int index){
        if(sleeping||stage()==Stage.ENDED||actionAnim!=ActionAnim.NONE)return;
        pendingFaceMood=Math.max(0,Math.min(11,index));
        activeFaceMood=-1;
        faceMoodUntil=0;
        moodExitUp=false;
        moodApproach=true;
        directionalIdleUntil=0;

        float[][] nodes=roomNodes();
        int target=foregroundNode();
        targetNodeIndex=target;
        targetNX=nodes[target][0];
        targetNY=nodes[target][1];

        travelDirection=TravelDirection.DOWN;
        walkMode=WalkMode.FRONT;
        walkDir=0;
        walking=true;
        walkStartedAt=System.currentTimeMillis();
        currentPetRes=0;
    }

    void startFacePresentation(){
        moodApproach=false;
        walking=false;
        walkMode=WalkMode.FRONT;
        travelDirection=TravelDirection.DOWN;
        idleDirection=TravelDirection.DOWN;
        directionalIdleUntil=0;
        releaseWalkFrames();
        activeFaceMood=pendingFaceMood>=0?pendingFaceMood:chooseFaceMoodIndex();
        pendingFaceMood=-1;
        faceMoodUntil=System.currentTimeMillis()+MOOD_DURATION_MS;
        faceRecoveryUntil=Math.max(faceRecoveryUntil,faceMoodUntil);
        currentPetRes=0;
        ensurePetImage();
        updatePetPosition();
    }

    void startMoodExitUp(){
        activeFaceMood=-1;
        faceMoodUntil=0;
        moodExitUp=true;
        scheduleNextFaceMood(System.currentTimeMillis());
        int target=upwardNode();
        float[][] nodes=roomNodes();
        targetNodeIndex=target;
        targetNX=nodes[target][0];
        targetNY=nodes[target][1];
        travelDirection=TravelDirection.UP;
        walkMode=WalkMode.BACK;
        walkDir=0;
        walking=true;
        walkStartedAt=System.currentTimeMillis();
        currentPetRes=0;
    }

    void showFaceMoodNow(int index,long duration){
        // Une humeur est toujours un idle-down complet, affiché 4 secondes.
        beginMoodApproach(index);
    }

    void releaseWalkFrames(){
        // ImageView / RenderThread may still reference these bitmaps: let GC reclaim them.
        currentWalkFrames=null;
        currentWalkStrip=null;
        currentWalkStripRes=0;
        loadedWalkStage=null;
        loadedWalkMode=null;
    }

    boolean loadWalkFrames(PetStage expectedStage,WalkMode expectedMode,int res){
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
                && currentWalkFrames.length==6)return true;

        releaseWalkFrames();

        Bitmap strip=null;
        try{strip=BitmapFactory.decodeResource(getResources(),res);}catch(Throwable ignored){}
        if(strip==null){
            markCharacterAssetInvalid(res,"walk","ressource illisible");
            showAssetErrorOnce();
            return false;
        }

        final int frame=CharacterSprites.FRAME_SIZE;
        final int count=6;
        if(strip.getWidth()!=frame*count || strip.getHeight()!=frame){
            markCharacterAssetInvalid(res,"walk",
                strip.getWidth()+"x"+strip.getHeight()+" : attendu "+(frame*count)+"x"+frame);
            showAssetErrorOnce();
            return false;
        }

        currentWalkStrip=strip;
        currentWalkFrames=new Bitmap[count];
        try{
            for(int i=0;i<count;i++)
                currentWalkFrames[i]=Bitmap.createBitmap(currentWalkStrip,i*frame,0,frame,frame);
        }catch(Throwable err){
            releaseWalkFrames();
            markCharacterAssetInvalid(res,"walk","découpage impossible");
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
        displayedPetStage=expectedStage;
        petView.setVisibility(View.VISIBLE);
        int advance=(games!=null?games.walkFrameAdvance():1);
        walkFrameIndex=(walkFrameIndex+advance)%currentWalkFrames.length;
        currentPetRes=0;
    }

    void releaseActionFrames(){
        actionStrip=null;
        actionFrames=null;
        actionStripRes=0;
        actionFrameIndex=0;
        actionFrameAt=0;
    }

    void clearSpecialPose(){
        specialPoseRes=0;
        specialPoseStage=null;
    }

    int specialPoseResource(String animation){
        if("bottle".equals(animation))return CareSprites.bottle(petStage());
        if("groom_foam".equals(animation)||"soap".equals(animation)
                ||"comb".equals(animation)||"towel".equals(animation))
            return CareSprites.forStage(petStage()).action(animation);
        return 0;
    }

    boolean renderSpecialPose(){
        if(specialPoseRes==0||specialPoseStage==null||specialPoseStage!=petStage())return false;
        if(invalidCharacterAssets.contains(specialPoseRes)){
            showAssetErrorOnce();
            return false;
        }
        try{
            petView.setImageResource(specialPoseRes);
            displayedPetStage=specialPoseStage;
            petView.setVisibility(View.VISIBLE);
            currentPetRes=0;
            return true;
        }catch(Throwable err){
            markCharacterAssetInvalid(specialPoseRes,"action spéciale","chargement impossible");
            showAssetErrorOnce();
            return false;
        }
    }

    void startSpecialPose(int res,long duration){
        if(res==0)return;
        walking=false;
        moodApproach=false;
        moodExitUp=false;
        activeFaceMood=-1;
        pendingFaceMood=-1;
        faceMoodUntil=0;
        directionalIdleUntil=0;
        releaseWalkFrames();
        releaseActionFrames();
        actionAnim=ActionAnim.NONE;
        specialPoseRes=res;
        specialPoseStage=petStage();
        manualUntil=System.currentTimeMillis()+Math.max(500L,duration);
        manualFrame=0;
        currentPetRes=0;
        renderSpecialPose();
        updatePetPosition();
    }

    int actionResource(ActionAnim type){
        CharacterSprites.Pack pack=CharacterSprites.forStage(petStage());
        if(type==ActionAnim.EAT)return pack.eat;
        if(type==ActionAnim.JUMP)return pack.jump;
        if(type==ActionAnim.SLEEP)return pack.sleep;
        return 0;
    }

    int actionFrameCount(ActionAnim type){
        if(type==ActionAnim.JUMP)return 5;
        if(type==ActionAnim.EAT||type==ActionAnim.SLEEP)return 3;
        return 0;
    }

    boolean loadActionFrames(ActionAnim type){
        int res=actionResource(type);
        int count=actionFrameCount(type);
        if(res==0||count==0)return false;
        if(invalidCharacterAssets.contains(res)){showAssetErrorOnce();return false;}
        if(actionStripRes==res && actionFrames!=null && actionFrames.length==count)return true;

        releaseActionFrames();
        Bitmap strip=null;
        try{strip=BitmapFactory.decodeResource(getResources(),res);}catch(Throwable ignored){}
        if(strip==null){
            markCharacterAssetInvalid(res,type.name(),"ressource illisible");
            showAssetErrorOnce();
            return false;
        }

        int frame=CharacterSprites.FRAME_SIZE;
        if(strip.getWidth()!=frame*count || strip.getHeight()!=frame){
            markCharacterAssetInvalid(res,type.name(),
                strip.getWidth()+"x"+strip.getHeight()+" au lieu de "+(frame*count)+"x"+frame);
            showAssetErrorOnce();
            return false;
        }

        actionStrip=strip;
        actionFrames=new Bitmap[count];
        try{
            for(int i=0;i<count;i++)
                actionFrames[i]=Bitmap.createBitmap(actionStrip,i*frame,0,frame,frame);
        }catch(Throwable err){
            releaseActionFrames();
            markCharacterAssetInvalid(res,type.name(),"découpage impossible");
            showAssetErrorOnce();
            return false;
        }
        actionStripRes=res;
        actionFrameIndex=0;
        return true;
    }

    void startActionAnimation(ActionAnim type,long duration){
        if(type==ActionAnim.NONE)return;
        walking=false;
        moodApproach=false;
        moodExitUp=false;
        activeFaceMood=-1;
        pendingFaceMood=-1;
        faceMoodUntil=0;
        directionalIdleUntil=0;
        releaseWalkFrames();
        releaseActionFrames();
        clearSpecialPose();
        actionAnim=type;
        actionStartedAt=System.currentTimeMillis();
        actionUntil=type==ActionAnim.SLEEP?Long.MAX_VALUE:actionStartedAt+Math.max(500L,duration);
        actionFrameAt=0;
        currentPetRes=0;

        if(type!=ActionAnim.SLEEP){
            long nonFace=Math.max(500L,duration);
            long recovery=(long)Math.ceil(nonFace*(MIN_IDLE_DOWN_SHARE/(1f-MIN_IDLE_DOWN_SHARE)));
            faceRecoveryUntil=Math.max(faceRecoveryUntil,actionStartedAt+duration+recovery);
            nextWalkAt=Math.max(nextWalkAt,faceRecoveryUntil);
        }
    }

    void showActionAnimationFrame(long now){
        if(actionAnim==ActionAnim.NONE)return;
        if(!loadActionFrames(actionAnim))return;
        long delay=actionAnim==ActionAnim.SLEEP?900L:220L;
        if(actionFrameAt==0 || now-actionFrameAt>=delay){
            if(actionFrameAt!=0)actionFrameIndex=(actionFrameIndex+1)%actionFrames.length;
            actionFrameAt=now;
        }
        petView.setImageBitmap(actionFrames[actionFrameIndex]);
        displayedPetStage=petStage();
        petView.setVisibility(View.VISIBLE);
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
                    stage()==Stage.TEEN?.34f:
                    stage()==Stage.ADULT?.39f:.38f;

        int size=Math.max(dp(88),Math.round(r[2]*ratio));
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)petView.getLayoutParams();
        lp.width=size;
        lp.height=size;
        lp.gravity=Gravity.TOP|Gravity.LEFT;
        petView.setLayoutParams(lp);
        petView.setPadding(0,0,0,0);
        petView.setPivotX(size/2f);
        petView.setPivotY(size*(240f/256f));

        FrameLayout.LayoutParams incidentLp=(FrameLayout.LayoutParams)incidentView.getLayoutParams();
        int incidentSize=Math.max(dp(48),Math.round(r[2]*.10f));
        incidentLp.width=incidentSize;
        incidentLp.height=incidentSize;
        incidentLp.gravity=Gravity.TOP|Gravity.LEFT;
        incidentLp.setMargins(0,0,0,0);
        incidentView.setLayoutParams(incidentLp);
        incidentView.post(this::positionIncident);
        if(games!=null)games.positionToy();
    }

    float clamp01(float v){return Math.max(0f,Math.min(1f,v));}

    void chooseIncidentPosition(){
        float[][] nodes=roomNodes();
        if(nodes.length==0){
            incidentNX=.50f;incidentNY=.85f;incidentRoom=room;return;
        }

        int index=rnd.nextInt(nodes.length);
        if(nodes.length>1 && index==petNodeIndex)
            index=(index+1+rnd.nextInt(nodes.length-1))%nodes.length;

        float minX=1f,maxX=0f,minY=1f,maxY=0f;
        for(float[] n:nodes){
            minX=Math.min(minX,n[0]);maxX=Math.max(maxX,n[0]);
            minY=Math.min(minY,n[1]);maxY=Math.max(maxY,n[1]);
        }

        float jitterX=(rnd.nextFloat()-.5f)*.10f;
        float jitterY=(rnd.nextFloat()-.5f)*.05f;
        incidentNX=Math.max(minX,Math.min(maxX,nodes[index][0]+jitterX));
        incidentNY=Math.max(minY,Math.min(maxY,nodes[index][1]+jitterY));
        incidentRoom=room;
    }

    void positionIncident(){
        if(incidentView==null||scene==null||incident.isEmpty())return;
        if(!incidentRoom.isEmpty()&&!room.equals(incidentRoom))return;

        float[] r=imageRect();
        if(r[2]<=0||r[3]<=0)return;
        int w=incidentView.getWidth()>0?incidentView.getWidth():incidentView.getLayoutParams().width;
        int h=incidentView.getHeight()>0?incidentView.getHeight():incidentView.getLayoutParams().height;
        if(w<=0||h<=0)return;

        float centerX=r[0]+clamp01(incidentNX)*r[2];
        float groundY=r[1]+clamp01(incidentNY)*r[3];
        float x=Math.max(r[0],Math.min(r[0]+r[2]-w,centerX-w/2f));
        float y=Math.max(r[1],Math.min(r[1]+r[3]-h,groundY-h));
        incidentView.setX(x);
        incidentView.setY(y);
    }


    /**
     * Points de marche analysés pièce par pièce.
     * Chaque point correspond à une zone de sol réellement dégagée dans l'image.
     * Les connexions ci-dessous empêchent les trajets de traverser canapé,
     * mobilier fixe, baignoire, toilettes et massifs.
     */
    float[][] roomNodes(){
        if(room.equals("cuisine")){
            // v0.6.3 : nouveau décor sans table, grande zone de sol libre.
            return new float[][]{
                {.16f,.93f},{.34f,.94f},{.52f,.94f},{.70f,.94f},{.86f,.93f},
                {.20f,.82f},{.40f,.82f},{.60f,.82f},{.80f,.82f}
            };
        }
        if(room.equals("bain")){
            return new float[][]{
                {.18f,.93f},{.40f,.95f},{.62f,.95f},{.80f,.92f},
                {.18f,.82f},{.78f,.82f}
            };
        }
        if(room.equals("jardin")){
            // v0.6.3 : nouveau jardin sans barrières, grille centrale ouverte.
            // Toutes les liaisons sont horizontales ou verticales.
            return new float[][]{
                {.34f,.90f},{.50f,.90f},{.66f,.90f},
                {.34f,.74f},{.50f,.74f},{.66f,.74f},
                {.34f,.58f},{.50f,.58f},{.66f,.58f}
            };
        }
        // Salon v0.7.0 : uniquement le tapis, avec marge devant le canapé.
        // La rangée .74 est supprimée pour que le corps ne chevauche jamais le canapé.
        return new float[][]{
            {.30f,.95f},{.50f,.95f},{.70f,.95f},
            {.30f,.86f},{.50f,.86f},{.70f,.86f}
        };
    }

    int[][] roomLinks(){
        if(room.equals("cuisine")){
            return new int[][]{
                {1,5},{0,2,5,6},{1,3,6},{2,4,6,7},{3,7},
                {0,1,6},{1,2,3,5,7,8},{3,4,6,8},{6,7}
            };
        }
        if(room.equals("bain")){
            return new int[][]{
                {1,4},{0,2},{1,3},{2,5},{0},{3}
            };
        }
        if(room.equals("jardin")){
            return new int[][]{
                {1,3},{0,2,4},{1,5},
                {0,4,6},{1,3,5,7},{2,4,8},
                {3,7},{4,6,8},{5,7}
            };
        }
        return new int[][]{
            {1,3},{0,2,4},{1,5},
            {0,4},{1,3,5},{2,4}
        };
    }

    int defaultRoomNode(){
        if(room.equals("cuisine"))return 2;
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
        moodApproach=false;
        moodExitUp=false;
        pendingFaceMood=-1;
        currentPetRes=0;
        if(!sleeping)ensurePetImage();
        updatePetPosition();
        nextWalkAt=System.currentTimeMillis()+(animate?3500:4500)+rnd.nextInt(2501);
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
        updateTravelDirection(dx,dy);

        faceMoodUntil=0;
        activeFaceMood=-1;
        walking=true;
        walkStartedAt=System.currentTimeMillis();
        walkFrameIndex=0;
    }

    void updateTravelDirection(float dx,float dy){
        int direction=SpriteMotion.direction(dx,dy,1f,1f);
        travelDirection=TravelDirection.values()[direction];
        // Règle stricte : vers le haut = dos, vers le bas = face.
        walkMode=direction==SpriteMotion.UP?WalkMode.BACK:
                direction==SpriteMotion.DOWN?WalkMode.FRONT:WalkMode.SIDE;
        walkDir=direction==SpriteMotion.LEFT?-1:direction==SpriteMotion.RIGHT?1:0;
    }

    float depthScale(){
        float[][] nodes=roomNodes();
        float minY=1f,maxY=0f;
        for(float[] n:nodes){
            minY=Math.min(minY,n[1]);
            maxY=Math.max(maxY,n[1]);
        }
        float t=clamp01((petNY-minY)/Math.max(.01f,maxY-minY));
        // Perspective plus douce : profondeur visible sans saut de taille excessif.
        return .90f+.14f*t;
    }

    void updatePetPosition(){
        if(petView==null||scene==null||petView.getWidth()<=0)return;
        if(promenadeAway()){
            petView.setVisibility(View.INVISIBLE);
            return;
        }
        float[] r=imageRect();
        if(r[2]<=0||r[3]<=0)return;

        petNX=clamp01(petNX);
        petNY=clamp01(petNY);
        float left=r[0]+petNX*r[2]-petView.getWidth()/2f;
        float feet=r[1]+petNY*r[3];
        float top=feet-petView.getHeight()*(240f/256f);
        petView.setX(left);
        petView.setY(top);

        float scale=depthScale();
        // v0.6.0 : LEFT et RIGHT ont chacun leurs 6 vraies frames. Aucun miroir.
        petView.setScaleX(scale);
        petView.setScaleY(scale);
        petView.setAlpha(1f);
        petView.setVisibility(displayedPetStage==petStage()
                && petView.getDrawable()!=null?View.VISIBLE:View.INVISIBLE);
        petView.bringToFront();
        incidentView.bringToFront();
        moodLabel.bringToFront();
        cleanHint.bringToFront();
    }

    String moodText(){
        if(promenadeAway())return "En promenade";
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
        if(promenadeAway()){
            toast("🌿 "+pet+" est en promenade.");
            return;
        }

        if(sleeping){
            wakeUp("Réveillé par le joueur",true);
            return;
        }

        float factor=beginRepeatedAction("affection");
        applyNeedDelta(0,0,0,12,4,0,0,factor);

        String boredom=PetBehavior.boredomText(pet,factor);
        if(!boredom.isEmpty()){
            showFaceMoodNow(10,4000);
            toast(boredom);
        }else if(!strongEmotion()){
            showFaceMoodNow(rnd.nextBoolean()?6:7,6000);
            toast("❤️ "+pet+" adore la caresse !");
        } else {
            showFaceMoodNow(chooseFaceMoodIndex(),6000);
            toast("🤍 "+pet+" apprécie la caresse, mais "+moodText().toLowerCase(Locale.ROOT)+".");
        }
        addHistory("Caresse donnée à "+pet+".");
        save();
        refresh();
    }

    void roomsMenu(){
        String[] rooms={"🛋️ Salon","🍽️ Cuisine","🛁 Salle de bain","🌿 Jardin"};
        new AlertDialog.Builder(this).setTitle("Choisir une pièce").setItems(rooms,(d,w)->{
            if(!promenadeAway())wakeForAction();
            if(games!=null)games.cancel();
            if(gardenGames!=null)gardenGames.cancel();
            callingToForeground=false;
            room=w==1?"cuisine":w==2?"bain":w==3?"jardin":"salon";
            addHistory("Déplacement vers : "+roomName()+".");
            resetPetForRoom(true);
            save();
            refresh();
        }).show();
    }

    void startPromenade(ObjectSystem.Item item){
        if(stage()==Stage.ENDED)return;
        long now=System.currentTimeMillis();
        long start=sp.getLong("promenadeStart",0L);
        boolean active=sp.getBoolean("promenadeActive",false);
        if(active && (start<=0L || now-start>=PromenadeActivity.DURATION_MS)){
            active=false;
            sp.edit().putBoolean("promenadeActive",false).apply();
        }

        if(!active){
            wakeForAction();
            if(games!=null)games.cancel();
            if(gardenGames!=null)gardenGames.cancel();
            releaseActionFrames();
            actionAnim=ActionAnim.NONE;
            clearSpecialPose();
            walking=false;

            float factor=beginRepeatedAction("walk");
            applyNeedDelta(item.hunger,item.water,item.clean,item.affection,item.happy,item.energy,item.stars,factor);
            skillCare=clamp(skillCare+.5f*factor);
            start=now;
            sp.edit()
              .putBoolean("promenadeActive",true)
              .putLong("promenadeStart",start)
              .apply();
            addHistory("Promenade démarrée pour 3 minutes.");
            petView.setVisibility(View.INVISIBLE);
            save();
            refresh();
        }

        Intent intent=new Intent(this,PromenadeActivity.class);
        intent.putExtra(PetProfileStore.EXTRA_SLOT,profileSlot);
        internalTransition=true;
        startActivity(intent);
    }

    void actionsMenu(){
        String[] actions={"⚠ Punir","🧽 Nettoyer","📣 Appeler"};
        new AlertDialog.Builder(this).setTitle("Actions").setItems(actions,(d,w)->{
            if(w==0)punish();
            else if(w==1)startCleaning();
            else callLeopard();
        }).show();
    }

    void callLeopard(){
        if(stage()==Stage.ENDED)return;
        if(promenadeAway()){
            toast("🌿 "+pet+" est en promenade.");
            return;
        }
        wakeForAction();
        callingEffectFactor=beginRepeatedAction("affection");
        if(games!=null)games.cancel();
        activeFaceMood=-1;
        pendingFaceMood=-1;
        faceMoodUntil=0;
        moodApproach=false;
        moodExitUp=false;
        manualUntil=0;
        releaseActionFrames();
        actionAnim=ActionAnim.NONE;

        int target=foregroundNode();
        float[][] nodes=roomNodes();
        targetNodeIndex=target;
        targetNX=nodes[target][0];
        targetNY=nodes[target][1];
        updateTravelDirection(targetNX-petNX,targetNY-petNY);
        callingToForeground=true;
        walking=true;
        walkStartedAt=System.currentTimeMillis();
        walkFrameIndex=0;
        currentPetRes=0;
        toast("📣 "+pet+" arrive !");
    }

    void startCleaning(){
        if(incident.isEmpty()){
            toast("Il n’y a aucune bêtise à nettoyer.");
            return;
        }
        if(!incidentRoom.isEmpty()&&!room.equals(incidentRoom)){
            toast("Retourne dans la pièce où la bêtise a été faite.");
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
        if(incident.isEmpty())return true;
        if(!incidentRoom.isEmpty()&&!room.equals(incidentRoom))return true;

        float x=e.getX(),y=e.getY();
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            if(!cleaningMode){
                cleaningMode=true;
                cleanProgress=0;
                cleanHint.setVisibility(View.VISIBLE);
                incidentView.setAlpha(1f);
                incidentView.setScaleX(1f);
                incidentView.setScaleY(1f);
            }
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
        if(games!=null)games.cancel();
        callingToForeground=false;
        incident="";
        incidentRoom="";
        incidentNX=.50f;incidentNY=.85f;
        clean=clamp(clean+20);
        happy=clamp(happy+2);
        skillClean=clamp(skillClean+3);
        stars+=1;
        addHistory("Bêtise nettoyée : "+old+".");
        save();
        refreshIncident();
        refresh();
        toast("✨ Bêtise nettoyée ! +1 ★");
    }

    void refreshIncident(){
        if(incident.isEmpty()){
            incidentView.setVisibility(View.GONE);
            cleanHint.setVisibility(View.GONE);
            cleaningMode=false;
            return;
        }
        if(incidentRoom.isEmpty())incidentRoom=room;
        if(!room.equals(incidentRoom)){
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
        incidentView.post(this::positionIncident);
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
        if(stage()==Stage.ENDED || !incident.isEmpty() || sleeping
                || activeFaceMood>=0 || moodApproach || moodExitUp)return;

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
            chooseIncidentPosition();
            clean=clamp(clean-8);
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
        long now=System.currentTimeMillis();

        if(promenadeAwayAt(now)){
            walking=false;
            petView.setVisibility(View.INVISIBLE);
            return;
        }

        if(games!=null && games.beforeAnimate(now))return;
        if(gardenGames!=null && gardenGames.beforeAnimate(now))return;

        if(sleeping){
            if(actionAnim!=ActionAnim.SLEEP)startActionAnimation(ActionAnim.SLEEP,MIN_SLEEP_MS);
            showActionAnimationFrame(now);
            walking=false;
            updatePetPosition();
            return;
        }else if(actionAnim==ActionAnim.SLEEP){
            releaseActionFrames();
            actionAnim=ActionAnim.NONE;
        }

        if(actionAnim!=ActionAnim.NONE){
            if(now<actionUntil){
                showActionAnimationFrame(now);
                updatePetPosition();
                return;
            }
            releaseActionFrames();
            actionAnim=ActionAnim.NONE;
            currentPetRes=0;
        }

        if(activeFaceMood>=0){
            if(now<faceMoodUntil){
                ensurePetImage();
                return;
            }
            startMoodExitUp();
        }

        if(now<manualUntil){
            if(specialPoseRes!=0){
                if(renderSpecialPose()){
                    updatePetPosition();
                    return;
                }
                clearSpecialPose();
            }
            applyPose(manualFrame);
            return;
        }else if(specialPoseRes!=0){
            clearSpecialPose();
        }

        if(queuedFaceMood>=0 && actionAnim==ActionAnim.NONE && now>=manualUntil
                && !walking && activeFaceMood<0 && !moodApproach && !sleeping){
            int mood=queuedFaceMood;
            queuedFaceMood=-1;
            beginMoodApproach(mood);
            return;
        }

        if(stage()==Stage.ENDED){
            ensurePetImage();
            return;
        }

        if(walking){
            float dx=targetNX-petNX;
            float dy=targetNY-petNY;
            float dist=(float)Math.sqrt(dx*dx+dy*dy);

            if(dist<.012f){
                petNX=targetNX;petNY=targetNY;
                if(targetNodeIndex>=0)petNodeIndex=targetNodeIndex;

                if(games!=null && games.onPetArrived(now))return;
                if(gardenGames!=null && gardenGames.onPetArrived(now))return;

                if(callingToForeground){
                    callingToForeground=false;
                    walking=false;
                    travelDirection=TravelDirection.DOWN;
                    idleDirection=TravelDirection.DOWN;
                    walkMode=WalkMode.FRONT;
                    directionalIdleUntil=0;
                    currentPetRes=0;
                    walkStartedAt=0;
                    applyNeedDelta(0,0,0,8,2,0,0,callingEffectFactor);
                    ensurePetImage();
                    updatePetPosition();
                    save();
                    refresh();
                    String boredom=PetBehavior.boredomText(pet,callingEffectFactor);
                    toast(boredom.isEmpty()?"🐆 "+pet+" est là ! ❤️":boredom);
                    callingEffectFactor=1f;
                    return;
                }

                if(moodApproach){
                    startFacePresentation();
                    return;
                }

                boolean finishedMoodExit=moodExitUp;
                moodExitUp=false;
                walking=false;
                idleDirection=travelDirection;
                long walkDuration=Math.max(500L,now-walkStartedAt);
                long directional=(travelDirection==TravelDirection.DOWN)?0L:DIRECTIONAL_IDLE_MS;
                directionalIdleUntil=now+directional;

                // Seul IDLE DOWN + les humeurs comptent dans la cible de 30 %.
                long nonFace=walkDuration+directional;
                long requiredFace=(long)Math.ceil(nonFace*
                    (MIN_IDLE_DOWN_SHARE/(1f-MIN_IDLE_DOWN_SHARE)));
                faceRecoveryUntil=directionalIdleUntil+requiredFace;
                nextWalkAt=Math.max(faceRecoveryUntil,
                    finishedMoodExit?now+3500L:faceRecoveryUntil);

                currentPetRes=0;
                walkStartedAt=0;
                ensurePetImage();
                return;
            }

            float speed=stage()==Stage.OLD?.0048f:stage()==Stage.CUB?.0066f:.0075f;
            if(games!=null && games.fastRun())speed*=1.85f;
            float step=Math.min(speed,dist);
            if(!moodApproach && !moodExitUp)updateTravelDirection(dx,dy);
            petNX+=dx/dist*step;
            petNY+=dy/dist*step;
            showWalkFrame(walkMode);
            updatePetPosition();
            return;
        }

        maybeShowFaceMood(now);
        currentPetRes=0;
        ensurePetImage();

        if(nextWalkAt==0)nextWalkAt=now+5000L;
        if(now>=nextWalkAt && now>=faceRecoveryUntil && energy>22
                && actionAnim==ActionAnim.NONE && activeFaceMood<0 && !moodApproach){
            int chance=stage()==Stage.OLD?35:75;
            if(rnd.nextInt(100)<chance)chooseWalkTarget();
            else nextWalkAt=now+3500L+rnd.nextInt(2501);
        }
    }

    int poseDrawableForFrame(int frame){
        return idleDownDrawable();
    }

    void applyPose(int frame){
        syncVisualStage();
        walking=false;
        idleDirection=TravelDirection.DOWN;
        directionalIdleUntil=0;
        int res=idleDownDrawable();
        if(!setPetDrawableSafely(petStage(),res))return;
        currentPetRes=res;
        petView.setRotation(0f);
        updatePetPosition();
    }

    void showAction(int frame,int duration){
        if(frame==11 && !sleeping){
            showFaceMoodNow(rnd.nextBoolean()?0:8,MOOD_DURATION_MS);
            return;
        }
        clearSpecialPose();
        manualFrame=frame;
        manualUntil=System.currentTimeMillis()+duration;
        activeFaceMood=-1;
        faceMoodUntil=0;
        walking=false;
        idleDirection=TravelDirection.DOWN;
        directionalIdleUntil=0;
        currentPetRes=0;
        applyPose(frame);
    }

    void act(String msg,int frame,float h,float w,float c,float af,float joy,float e,int gainStars){
        act(msg,frame,h,w,c,af,joy,e,gainStars,null);
    }

    void act(String msg,int frame,float h,float w,float c,float af,float joy,float e,int gainStars,String animation){
        wakeForAction();
        hunger=clamp(hunger+h);
        thirst=clamp(thirst+w);
        clean=clamp(clean+c);
        affection=clamp(affection+af);
        happy=clamp(happy+joy);
        energy=clamp(energy+e);
        stars+=gainStars;

        int special=specialPoseResource(animation);
        if(special!=0)startSpecialPose(special,"bottle".equals(animation)?3000L:2600L);
        else if("eat".equals(animation))startActionAnimation(ActionAnim.EAT,3000L);
        else if("jump".equals(animation))startActionAnimation(ActionAnim.JUMP,2200L);
        else showAction(frame,2200);

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
        stars+=reward;happy=clamp(happy+12);affection=clamp(affection+4);
        energy=clamp(energy-22);thirst=clamp(thirst-12);
        skillObedience=clamp(skillObedience+1);skillCare=clamp(skillCare+1);
        startActionAnimation(ActionAnim.JUMP,2400L);
        save();refresh();
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
                PetProfileStore.updateName(this,profileSlot,pet);
                if(first)recordAdoption(pet);
                save();
                refresh();
            })
            .setNegativeButton(first?"Léo":"Annuler",(d,w)->{
                if(first){
                    pet="Léo";
                    sp.edit().putBoolean("named",true).apply();
                    PetProfileStore.updateName(this,profileSlot,pet);
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
        incidentRoom="";
        incidentNX=.50f;incidentNY=.85f;
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

        if(!incident.isEmpty() || now<manualUntil || walking || actionAnim!=ActionAnim.NONE
                || activeFaceMood>=0 || moodApproach || moodExitUp)return;

        if(energy<=35){beginAutoSleep();return;}

        if(now>=nextAutoSleepAt){
            if(energy<82)beginAutoSleep();
            else nextAutoSleepAt=now+90000L+rnd.nextInt(90000);
        }
    }

    void beginAutoSleep(){
        if(sleeping||stage()==Stage.ENDED)return;
        sleeping=true;
        PetBehavior.resetRepetition(sp);
        walking=false;
        moodApproach=false;
        moodExitUp=false;
        activeFaceMood=-1;
        faceMoodUntil=0;
        manualUntil=0;
        sleepEndAt=System.currentTimeMillis()+MIN_SLEEP_MS;
        startActionAnimation(ActionAnim.SLEEP,MIN_SLEEP_MS);
        addHistory(pet+" s'est endormi pour 1 min 30.");
        save();
        refresh();
    }

    void wakeUp(String reason,boolean notify){
        if(!sleeping)return;
        boolean natural=System.currentTimeMillis()>=sleepEndAt;
        sleeping=false;
        sleepEndAt=0;
        releaseActionFrames();
        actionAnim=ActionAnim.NONE;
        idleDirection=TravelDirection.DOWN;
        directionalIdleUntil=0;
        nextAutoSleepAt=System.currentTimeMillis()+(4+rnd.nextInt(4))*60000L;
        if(natural)energy=100f;
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
        String[] entries={"🐾 Changer d’animal","📜 Historique","🔎 Vérifier les quatre packs"};
        new AlertDialog.Builder(this).setTitle("Menu").setItems(entries,(d,w)->{
            if(w==0)openPetChooser();
            else if(w==1)showHistory();
            else showSpritePackPicker();
        }).show();
    }

    void openPetChooser(){
        save();
        if(games!=null)games.cancel();
        if(gardenGames!=null)gardenGames.cancel();
        internalTransition=true;
        Intent chooser=new Intent(this,PetChooserActivity.class);
        startActivity(chooser);
        finish();
    }

    void showSpritePackPicker(){
        String[] names={"Léopardeau / CUB","Ado / TEEN","Adulte / ADULT","Vieux / OLD"};
        new AlertDialog.Builder(this).setTitle("Packs séparés — diagnostic")
            .setItems(names,(d,w)->showSpritePack(PetStage.values()[w],0,0)).show();
    }

    void showSpritePack(PetStage age,int item,int frame){
        CharacterSprites.Pack p=CharacterSprites.forStage(age);
        int[] ids={p.idleDown,p.idleLeft,p.idleRight,p.idleUp,
            p.walkDown,p.walkLeft,p.walkRight,p.walkUp,p.jump,p.eat,p.sleep,p.moods};
        int[] counts={1,1,1,1,6,6,6,6,5,3,3,12};
        String[] names={"idle_down","idle_left","idle_right","idle_up",
            "walk_down","walk_left","walk_right","walk_up","jump","eat","sleep","moods"};
        int k=Math.floorMod(item,ids.length);
        int f=Math.floorMod(frame,counts[k]);

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView note=text(12,false);
        note.setText("Version 0.7.5 • "+p.zone+"\n"+names[k]+
            (counts[k]>1?" • frame "+(f+1)+"/"+counts[k]:""));
        note.setPadding(dp(14),dp(8),dp(14),dp(8));
        box.addView(note);

        ImageView image=new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap b=BitmapFactory.decodeResource(getResources(),ids[k]);
        int fs=CharacterSprites.FRAME_SIZE;
        if(b!=null && counts[k]>1 && b.getWidth()==fs*counts[k] && b.getHeight()==fs)
            b=Bitmap.createBitmap(b,f*fs,0,fs,fs);
        if(b!=null)image.setImageBitmap(b);
        box.addView(image,new LinearLayout.LayoutParams(-1,dp(260)));

        if(counts[k]>1){
            Button next=button("Frame suivante");
            box.addView(next);
            final AlertDialog[] dialog=new AlertDialog[1];
            next.setOnClickListener(v->{dialog[0].dismiss();showSpritePack(age,k,f+1);});
            dialog[0]=new AlertDialog.Builder(this).setTitle("Pack "+age)
                .setView(box).setNegativeButton("Précédent",(d,w)->showSpritePack(age,k-1,0))
                .setPositiveButton("Suivant",(d,w)->showSpritePack(age,k+1,0))
                .setNeutralButton("Fermer",null).create();
            dialog[0].show();
        }else{
            new AlertDialog.Builder(this).setTitle("Pack "+age).setView(box)
                .setNegativeButton("Précédent",(d,w)->showSpritePack(age,k-1,0))
                .setPositiveButton("Suivant",(d,w)->showSpritePack(age,k+1,0))
                .setNeutralButton("Fermer",null).show();
        }
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
        return CharacterSprites.forStage(stage).ownsIdle(res);
    }

    boolean keepCurrentImageIfSameStage(PetStage expectedStage){
        boolean safe=displayedPetStage==expectedStage && petView.getDrawable()!=null;
        petView.setVisibility(safe?View.VISIBLE:View.INVISIBLE);
        return safe;
    }

    boolean setPetDrawableSafely(PetStage expectedStage,int res){
        if(expectedStage!=petStage() || !isMoodResourceForStage(expectedStage,res)){
            addHistory("ERREUR mélange d'âge bloqué sur une pose ("+expectedStage+").");
            keepCurrentImageIfSameStage(expectedStage);
            showAssetErrorOnce();
            return false;
        }
        if(invalidCharacterAssets.contains(res)){
            keepCurrentImageIfSameStage(expectedStage);
            showAssetErrorOnce();
            return false;
        }
        try{
            petView.setImageResource(res);
            displayedPetStage=expectedStage;
            petView.setVisibility(View.VISIBLE);
            return true;
        }catch(Throwable err){
            markCharacterAssetInvalid(res,"pose","chargement impossible");
            keepCurrentImageIfSameStage(expectedStage);
            showAssetErrorOnce();
            return false;
        }
    }

    void validateCharacterAssets(){
        invalidCharacterAssets.clear();
        assetErrorShown=false;
        HashMap<Integer,PetStage> owners=new HashMap<>();
        for(PetStage age:PetStage.values()){
            CharacterSprites.Pack pack=CharacterSprites.forStage(age);
            for(int res:pack.allResources()){
                PetStage other=owners.put(res,age);
                if(other!=null && other!=age){
                    markCharacterAssetInvalid(res,age+" PACK","partage inter-âge interdit");
                    continue;
                }
                try{
                    String name=getResources().getResourceEntryName(res);
                    if(!name.startsWith("leopard_"+age.name().toLowerCase(Locale.ROOT)+"_"))
                        throw new IllegalArgumentException("mauvais préfixe : "+name);
                    BitmapFactory.Options opts=new BitmapFactory.Options();
                    opts.inJustDecodeBounds=true;opts.inScaled=false;
                    BitmapFactory.decodeResource(getResources(),res,opts);
                    int width=pack.expectedWidth(res);
                    int height=CharacterSprites.FRAME_SIZE;
                    if(width<0 || opts.outWidth!=width || opts.outHeight!=height)
                        throw new IllegalArgumentException(opts.outWidth+"x"+opts.outHeight+
                            " au lieu de "+width+"x"+height);
                }catch(RuntimeException error){
                    markCharacterAssetInvalid(res,age+" PACK",error.getMessage());
                }
            }

            GameSprites.Pack game=GameSprites.forStage(age);
            for(int res:game.allResources()){
                PetStage other=owners.put(res,age);
                if(other!=null && other!=age){
                    markCharacterAssetInvalid(res,age+" GAME","partage inter-âge interdit");
                    continue;
                }
                try{
                    String name=getResources().getResourceEntryName(res);
                    if(!name.startsWith("leopard_"+age.name().toLowerCase(Locale.ROOT)+"_"))
                        throw new IllegalArgumentException("mauvais préfixe jeu : "+name);
                    BitmapFactory.Options opts=new BitmapFactory.Options();
                    opts.inJustDecodeBounds=true;opts.inScaled=false;
                    BitmapFactory.decodeResource(getResources(),res,opts);
                    int width=game.expectedWidth(res);
                    int height=GameSprites.FRAME_SIZE;
                    if(width<0 || opts.outWidth!=width || opts.outHeight!=height)
                        throw new IllegalArgumentException(opts.outWidth+"x"+opts.outHeight+
                            " au lieu de "+width+"x"+height);
                }catch(RuntimeException error){
                    markCharacterAssetInvalid(res,age+" GAME",error.getMessage());
                }
            }

            CareSprites.Pack care=CareSprites.forStage(age);
            for(int res:care.allResources()){
                PetStage other=owners.put(res,age);
                if(other!=null && other!=age){
                    markCharacterAssetInvalid(res,age+" CARE","partage inter-âge interdit");
                    continue;
                }
                try{
                    String name=getResources().getResourceEntryName(res);
                    if(!name.startsWith("leopard_"+age.name().toLowerCase(Locale.ROOT)+"_"))
                        throw new IllegalArgumentException("mauvais préfixe soin : "+name);
                    BitmapFactory.Options opts=new BitmapFactory.Options();
                    opts.inJustDecodeBounds=true;opts.inScaled=false;
                    BitmapFactory.decodeResource(getResources(),res,opts);
                    int width=care.expectedWidth(res);
                    int height=CareSprites.FRAME_SIZE;
                    if(width<0 || opts.outWidth!=width || opts.outHeight!=height)
                        throw new IllegalArgumentException(opts.outWidth+"x"+opts.outHeight+
                            " au lieu de "+width+"x"+height);
                }catch(RuntimeException error){
                    markCharacterAssetInvalid(res,age+" CARE",error.getMessage());
                }
            }

            GardenSprites.Pack garden=GardenSprites.forStage(age);
            for(int res:garden.allResources()){
                PetStage other=owners.put(res,age);
                if(other!=null && other!=age){
                    markCharacterAssetInvalid(res,age+" GARDEN","partage inter-âge interdit");
                    continue;
                }
                try{
                    String name=getResources().getResourceEntryName(res);
                    if(!name.startsWith("leopard_"+age.name().toLowerCase(Locale.ROOT)+"_"))
                        throw new IllegalArgumentException("mauvais préfixe jardin : "+name);
                    BitmapFactory.Options opts=new BitmapFactory.Options();
                    opts.inJustDecodeBounds=true;opts.inScaled=false;
                    BitmapFactory.decodeResource(getResources(),res,opts);
                    int width=garden.expectedWidth(res);
                    int height=GardenSprites.FRAME_SIZE;
                    if(width<0 || opts.outWidth!=width || opts.outHeight!=height)
                        throw new IllegalArgumentException(opts.outWidth+"x"+opts.outHeight+
                            " au lieu de "+width+"x"+height);
                }catch(RuntimeException error){
                    markCharacterAssetInvalid(res,age+" GARDEN",error.getMessage());
                }
            }

            if(age==PetStage.CUB){
                int bottle=CareSprites.bottle(age);
                PetStage other=owners.put(bottle,age);
                if(other!=null && other!=age){
                    markCharacterAssetInvalid(bottle,age+" BOTTLE","partage inter-âge interdit");
                }else{
                    try{
                        String name=getResources().getResourceEntryName(bottle);
                        if(!name.startsWith("leopard_cub_bottle"))
                            throw new IllegalArgumentException("mauvais préfixe biberon : "+name);
                        BitmapFactory.Options opts=new BitmapFactory.Options();
                        opts.inJustDecodeBounds=true;opts.inScaled=false;
                        BitmapFactory.decodeResource(getResources(),bottle,opts);
                        if(opts.outWidth!=CareSprites.FRAME_SIZE || opts.outHeight!=CareSprites.FRAME_SIZE)
                            throw new IllegalArgumentException(opts.outWidth+"x"+opts.outHeight+
                                " au lieu de "+CareSprites.FRAME_SIZE+"x"+CareSprites.FRAME_SIZE);
                    }catch(RuntimeException error){
                        markCharacterAssetInvalid(bottle,age+" BOTTLE",error.getMessage());
                    }
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
