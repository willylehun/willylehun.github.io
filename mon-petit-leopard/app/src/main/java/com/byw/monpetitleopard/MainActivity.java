package com.byw.monpetitleopard;

import android.app.*;
import android.content.*;
import android.graphics.Color;
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

    long born,last,nextMischiefAt=0,nextWalkAt=0,manualUntil=0;
    float hunger=85,thirst=85,clean=90,affection=90,happy=90,energy=90;
    float skillClean=5,skillObedience=5,skillCare=5;
    int stars=0,generation=1;
    String pet="Léo",room="salon",incident="";
    boolean endShown=false,cleaningMode=false;
    float cleanProgress=0,lastRubX=0,lastRubY=0;

    TextView title,subTitle,timer,starTxt,moodLabel,skillTxt,cleanHint,incidentView;
    ProgressBar[] bars=new ProgressBar[6];
    TextView[] vals=new TextView[6];
    ImageView bg,petView;
    FrameLayout scene;
    Button roomsBtn,objectsBtn,actionsBtn;

    int walkDir=1,walkSteps=0,walkTick=0,idleTick=0,currentPetRes=0,manualFrame=0;
    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sp=getSharedPreferences("pet",MODE_PRIVATE);
        load();
        objects=new ObjectSystem(this);
        build();
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
            maybeMischief();
            refresh();
            handler.postDelayed(this,1000);
        }
    };

    final Runnable animator=new Runnable(){
        @Override public void run(){
            animateAuto();
            handler.postDelayed(this,320);
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
          .apply();
    }

    float clamp(float v){return Math.max(0,Math.min(100,v));}

    void tickNeeds(){
        long n=System.currentTimeMillis();
        long d=Math.max(0,n-last);
        float m=d/60000f;
        if(m>0 && stage()!=Stage.ENDED){
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

            hunger=clamp(hunger);thirst=clamp(thirst);clean=clamp(clean);
            affection=clamp(affection);energy=clamp(energy);

            int critical=0;
            if(hunger<22)critical++;
            if(thirst<22)critical++;
            if(clean<20)critical++;
            if(affection<20)critical++;
            if(energy<16)critical++;
            happy=clamp(happy-critical*.08f*m);
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
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10),dp(8),dp(10),dp(8));
        root.setBackgroundColor(Color.rgb(246,239,221));

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout names=new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        title=text(22,true);
        subTitle=text(12,false);
        names.addView(title);
        names.addView(subTitle);
        header.addView(names,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));

        timer=pill();
        starTxt=pill();
        header.addView(timer);
        LinearLayout.LayoutParams starParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        starParams.setMargins(dp(6),0,0,0);
        header.addView(starTxt,starParams);
        root.addView(header);

        LinearLayout needRow1=new LinearLayout(this);
        LinearLayout needRow2=new LinearLayout(this);
        String[] namesNeeds={"Faim","Eau","Propreté","Câlins","Bonheur","Sommeil"};
        for(int i=0;i<6;i++){
            LinearLayout box=needBox(namesNeeds[i],i);
            (i<3?needRow1:needRow2).addView(box,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        }
        root.addView(needRow1);
        root.addView(needRow2);

        skillTxt=text(11,false);
        skillTxt.setGravity(Gravity.CENTER);
        skillTxt.setPadding(0,dp(2),0,dp(5));
        root.addView(skillTxt);

        scene=new FrameLayout(this);
        GradientDrawable sceneBg=new GradientDrawable();
        sceneBg.setColor(Color.WHITE);
        sceneBg.setCornerRadius(dp(18));
        scene.setBackground(sceneBg);
        scene.setClipToOutline(true);

        bg=new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setAdjustViewBounds(false);
        scene.addView(bg,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        moodLabel=overlay();
        FrameLayout.LayoutParams moodParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        moodParams.gravity=Gravity.TOP|Gravity.LEFT;
        moodParams.setMargins(dp(8),dp(8),0,0);
        scene.addView(moodLabel,moodParams);

        cleanHint=overlay();
        cleanHint.setText("🧽 Frotte sur la bêtise pour la faire disparaître");
        cleanHint.setVisibility(View.GONE);
        FrameLayout.LayoutParams hintParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        hintParams.setMargins(dp(8),dp(48),dp(8),0);
        scene.addView(cleanHint,hintParams);

        petView=new ImageView(this);
        petView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        petView.setAdjustViewBounds(true);
        FrameLayout.LayoutParams petParams=new FrameLayout.LayoutParams(dp(230),dp(230));
        petParams.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
        petParams.bottomMargin=dp(8);
        scene.addView(petView,petParams);
        petView.setOnClickListener(v->petLeopard());

        incidentView=new TextView(this);
        incidentView.setTextSize(54);
        incidentView.setGravity(Gravity.CENTER);
        incidentView.setVisibility(View.GONE);
        GradientDrawable incidentBg=new GradientDrawable();
        incidentBg.setColor(Color.argb(210,255,249,230));
        incidentBg.setCornerRadius(dp(20));
        incidentBg.setStroke(dp(2),Color.argb(100,90,60,30));
        incidentView.setBackground(incidentBg);
        incidentView.setElevation(dp(8));
        FrameLayout.LayoutParams incidentParams=new FrameLayout.LayoutParams(dp(86),dp(86));
        incidentParams.gravity=Gravity.BOTTOM|Gravity.RIGHT;
        incidentParams.setMargins(0,0,dp(18),dp(22));
        scene.addView(incidentView,incidentParams);
        incidentView.setOnTouchListener((v,e)->handleRub(e));

        root.addView(scene,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));

        LinearLayout bottom=new LinearLayout(this);
        roomsBtn=button("🏠 Pièces");
        objectsBtn=button("🎒 Objets");
        actionsBtn=button("⚙ Actions");

        roomsBtn.setOnClickListener(v->roomsMenu());
        objectsBtn.setOnClickListener(v->objects.openMenu());
        actionsBtn.setOnClickListener(v->actionsMenu());

        bottom.addView(roomsBtn,buttonParams());
        bottom.addView(objectsBtn,buttonParams());
        bottom.addView(actionsBtn,buttonParams());
        root.addView(bottom);

        setContentView(root);
    }

    LinearLayout needBox(String label,int index){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(3),dp(2),dp(3),dp(2));

        TextView t=text(10,false);
        t.setGravity(Gravity.CENTER);
        t.setText(label);
        box.addView(t);

        bars[index]=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bars[index].setMax(100);
        box.addView(bars[index],new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(8)));

        vals[index]=text(9,false);
        vals[index].setGravity(Gravity.CENTER);
        box.addView(vals[index]);
        return box;
    }

    LinearLayout.LayoutParams buttonParams(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(50),1);
        p.setMargins(dp(2),dp(6),dp(2),0);
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
        int res=room.equals("cuisine")?R.drawable.room_kitchen:
                room.equals("bain")?R.drawable.room_bathroom:
                room.equals("jardin")?R.drawable.room_garden:R.drawable.room_living;
        bg.setImageResource(res);
        ensurePetImage();
    }

    void ensurePetImage(){
        int res=ageDrawable();
        if(res!=currentPetRes){
            currentPetRes=res;
            petView.setImageResource(res);
            petView.setAlpha(1f);
            petView.setScaleX(1f);
            petView.setScaleY(1f);
            petView.setRotation(0f);
        }
    }

    int ageDrawable(){
        Stage s=stage();
        if(s==Stage.ENDED)s=Stage.OLD;
        if(s==Stage.CUB)return R.drawable.leopard_cub;
        if(s==Stage.TEEN)return R.drawable.leopard_teen;
        if(s==Stage.ADULT)return R.drawable.leopard_adult;
        return R.drawable.leopard_old;
    }

    String moodText(){
        if(stage()==Stage.ENDED)return "Paisible";
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
        affection=clamp(affection+12);
        happy=clamp(happy+6);

        if(!strongEmotion()){
            showAction(11,2200);
            toast("❤️ "+pet+" adore la caresse !");
        } else {
            toast("🤍 "+pet+" apprécie la caresse, mais "+moodText().toLowerCase(Locale.ROOT)+".");
        }
        save();
        refresh();
    }

    void roomsMenu(){
        String[] rooms={"🛋️ Salon","🍽️ Cuisine","🛁 Salle de bain","🌿 Jardin"};
        new AlertDialog.Builder(this).setTitle("Choisir une pièce").setItems(rooms,(d,w)->{
            room=w==1?"cuisine":w==2?"bain":w==3?"jardin":"salon";
            petView.setTranslationX(0);
            walkSteps=0;
            nextWalkAt=0;
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
        if(!incident.isEmpty()){
            skillObedience=clamp(skillObedience+4);
            skillCare=clamp(skillCare+1);
            happy=clamp(happy-4);
            affection=clamp(affection-2);
            showAction(3,1800);
            toast("⚠ La punition est justifiée. La bêtise doit encore être nettoyée.");
        } else {
            happy=clamp(happy-20);
            affection=clamp(affection-14);
            skillObedience=clamp(skillObedience-1);
            showAction(3,2600);
            toast("😢 Punition injuste : son bonheur et ses câlins baissent.");
        }
        save();
        refresh();
    }

    void maybeMischief(){
        if(stage()==Stage.ENDED || !incident.isEmpty())return;

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
            refreshIncident();
        } else {
            skillObedience=clamp(skillObedience+.15f);
            skillCare=clamp(skillCare+.10f);
        }
        save();
    }

    void animateAuto(){
        if(scene==null||petView==null)return;
        ensurePetImage();
        idleTick++;

        if(System.currentTimeMillis()<manualUntil){
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

        long now=System.currentTimeMillis();
        if(walkSteps>0){
            walkTick++;
            petView.setRotation((walkTick%2==0)?-2f:2f);
            petView.setScaleY((walkTick%2==0)?1.015f:.985f);
            movePet();
            walkSteps--;
            return;
        }

        applyPose(0);

        if(nextWalkAt==0)nextWalkAt=now+1200+rnd.nextInt(2200);
        if(now>=nextWalkAt && energy>24){
            int chance=stage()==Stage.OLD?30:70;
            if(rnd.nextInt(100)<chance){
                walkSteps=8+rnd.nextInt(stage()==Stage.OLD?6:16);
                if(Math.abs(petView.getTranslationX())<dp(20))walkDir=rnd.nextBoolean()?1:-1;
            }
            nextWalkAt=now+1800+rnd.nextInt(3000);
        }
    }

    void movePet(){
        if(scene.getWidth()<=0||petView.getWidth()<=0)return;
        float max=Math.max(0,(scene.getWidth()-petView.getWidth())/2f-dp(14));
        float step=dp(stage()==Stage.OLD?5:9);
        float nx=petView.getTranslationX()+walkDir*step;
        if(nx>=max){nx=max;walkDir=-1;}
        if(nx<=-max){nx=-max;walkDir=1;}
        petView.setTranslationX(nx);
        petView.setScaleX(walkDir<0?-1f:1f);
    }

    void applyPose(int frame){
        float direction=petView.getScaleX()<0?-1f:1f;
        petView.setAlpha(1f);
        petView.setRotation(0f);
        petView.setScaleY(1f);
        petView.setScaleX(direction);

        if(frame==3){
            petView.setAlpha(.88f);
            petView.setScaleY(.94f);
        } else if(frame==9){
            petView.setRotation(5f);
            petView.setScaleY(.86f);
        } else if(frame==10){
            petView.setScaleX(direction*1.05f);
            petView.setScaleY(1.05f);
            petView.setRotation((idleTick%2==0)?-3f:3f);
        } else if(frame==11){
            petView.setScaleX(direction*1.06f);
            petView.setScaleY(1.06f);
        } else if(idleTick%5==0){
            petView.setRotation((idleTick%2==0)?-1.5f:1.5f);
        }
    }

    void showAction(int frame,int duration){
        manualFrame=frame;
        manualUntil=System.currentTimeMillis()+duration;
        walkSteps=0;
        applyPose(frame);
    }

    void act(String msg,int frame,float h,float w,float c,float af,float joy,float e,int gainStars){
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
    }

    void competition(){
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
                save();
                refresh();
            })
            .setNegativeButton(first?"Léo":"Annuler",(d,w)->{
                if(first){
                    pet="Léo";
                    sp.edit().putBoolean("named",true).apply();
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
        petView.setTranslationX(0);
        petView.setScaleX(1f);
        roomsBtn.setEnabled(true);
        objectsBtn.setEnabled(true);
        actionsBtn.setEnabled(true);
        save();
        refresh();
        rename(true);
    }

    String format(long ms){
        long s=Math.max(0,ms/1000);
        long h=s/3600,m=(s%3600)/60,sec=s%60;
        return h>0?String.format(Locale.FRANCE,"%dh %02d:%02d",h,m,sec):String.format(Locale.FRANCE,"%02d:%02d",m,sec);
    }

    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
