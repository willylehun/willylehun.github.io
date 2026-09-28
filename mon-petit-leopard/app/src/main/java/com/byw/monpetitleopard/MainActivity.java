package com.byw.monpetitleopard;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final long H=3600000L, CUB=H, TEEN=5*H, ADULT=5*H, OLD=2*H, LIFE=13*H;
    final Handler handler=new Handler(Looper.getMainLooper());
    final Random rnd=new Random();

    SharedPreferences sp;
    long born,last,nextMischiefAt=0,nextWalkAt=0,manualUntil=0;
    float hunger=85, thirst=85, clean=90, affection=90, happy=90, energy=90;
    float skillClean=5, skillObedience=5, skillCare=5;
    int stars=0,generation=1;
    String pet="Léo",room="salon",incident="";
    boolean endShown=false;

    TextView title,stage,timer,starTxt,moodLabel,eventLabel,skillTxt;
    ProgressBar[] bars=new ProgressBar[6];
    TextView[] vals=new TextView[6];
    ImageView bg,petView;
    FrameLayout scene;
    Button actionBtn,punishBtn;

    Bitmap[] stateFrames=null,walkFrames=null;
    Stage spriteStage=null;
    int walkFrame=0,walkSteps=0,walkDir=1,manualFrame=0;
    int idleTick=0;

    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sp=getSharedPreferences("pet",MODE_PRIVATE);
        load();
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
        public void run(){
            tickNeeds();
            maybeMischief();
            refresh();
            handler.postDelayed(this,1000);
        }
    };

    final Runnable animator=new Runnable(){
        public void run(){
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
        long n=System.currentTimeMillis(),d=Math.max(0,n-last);
        float m=d/60000f;
        if(m>0&&stage()!=Stage.ENDED){
            float cleanFactor=1f-(0.40f*skillClean/100f);
            hunger-=.34f*m;
            thirst-=.42f*m;
            clean-=.12f*m*cleanFactor;
            affection-=.13f*m;
            happy-=.11f*m;
            energy-=.20f*m;

            hunger=clamp(hunger); thirst=clamp(thirst); clean=clamp(clean);
            affection=clamp(affection); energy=clamp(energy);

            int critical=0;
            if(hunger<22)critical++;
            if(thirst<22)critical++;
            if(clean<20)critical++;
            if(affection<20)critical++;
            if(energy<16)critical++;
            if(critical>0)happy-=critical*.08f*m;
            happy=clamp(happy);
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
        root.setPadding(dp(8),dp(6),dp(8),dp(6));
        root.setBackgroundColor(Color.rgb(246,239,221));

        LinearLayout head=new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts=new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        title=txt(21,true);
        stage=txt(12,false);
        texts.addView(title);texts.addView(stage);
        head.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
        timer=pill();starTxt=pill();
        head.addView(timer);
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-2,-2);
        slp.setMargins(dp(5),0,0,0);
        head.addView(starTxt,slp);
        root.addView(head);

        LinearLayout needs=new LinearLayout(this);
        String[] names={"Faim","Eau","Propreté","Câlins","Bonheur","Sommeil"};
        for(int i=0;i<6;i++){
            LinearLayout box=new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            TextView n=txt(10,false);
            n.setText(names[i]);n.setGravity(Gravity.CENTER);
            box.addView(n);
            bars[i]=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
            bars[i].setMax(100);
            box.addView(bars[i],new LinearLayout.LayoutParams(-1,dp(8)));
            vals[i]=txt(9,false);vals[i].setGravity(Gravity.CENTER);
            box.addView(vals[i]);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);
            bp.setMargins(dp(2),0,dp(2),0);
            needs.addView(box,bp);
        }
        root.addView(needs);

        skillTxt=txt(11,false);
        skillTxt.setGravity(Gravity.CENTER);
        skillTxt.setPadding(0,dp(2),0,dp(3));
        root.addView(skillTxt);

        scene=new FrameLayout(this);
        GradientDrawable sg=new GradientDrawable();
        sg.setColor(Color.WHITE);sg.setCornerRadius(dp(16));
        scene.setBackground(sg);scene.setClipToOutline(true);

        bg=new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        scene.addView(bg,new FrameLayout.LayoutParams(-1,-1));

        petView=new ImageView(this);
        petView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams petParams=new FrameLayout.LayoutParams(dp(230),dp(230));
        petParams.gravity=Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;
        petParams.bottomMargin=dp(4);
        scene.addView(petView,petParams);

        moodLabel=overlay();
        FrameLayout.LayoutParams moodParams=new FrameLayout.LayoutParams(-2,-2);
        moodParams.gravity=Gravity.TOP|Gravity.LEFT;
        moodParams.setMargins(dp(8),dp(8),0,0);
        scene.addView(moodLabel,moodParams);

        eventLabel=overlay();
        FrameLayout.LayoutParams eventParams=new FrameLayout.LayoutParams(-2,-2);
        eventParams.gravity=Gravity.TOP|Gravity.RIGHT;
        eventParams.setMargins(0,dp(8),dp(8),0);
        scene.addView(eventLabel,eventParams);

        root.addView(scene,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout actionRow=new LinearLayout(this);
        actionBtn=button("Actions");
        actionBtn.setOnClickListener(v->actions());
        punishBtn=button("⚠ Punir");
        punishBtn.setOnClickListener(v->punish());
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(46),3);
        ap.setMargins(0,dp(3),dp(3),0);
        actionRow.addView(actionBtn,ap);
        LinearLayout.LayoutParams ppb=new LinearLayout.LayoutParams(0,dp(46),1);
        ppb.setMargins(dp(3),dp(3),0,0);
        actionRow.addView(punishBtn,ppb);
        root.addView(actionRow);

        LinearLayout rooms=new LinearLayout(this);
        addRoom(rooms,"Salon","salon");
        addRoom(rooms,"Cuisine","cuisine");
        addRoom(rooms,"Bain","bain");
        addRoom(rooms,"Jardin","jardin");
        root.addView(rooms);

        setContentView(root);
    }

    TextView txt(int size,boolean bold){
        TextView t=new TextView(this);
        t.setTextSize(size);t.setTextColor(Color.rgb(55,47,34));
        if(bold)t.setTypeface(null,1);
        return t;
    }

    TextView pill(){
        TextView t=txt(12,false);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(9),dp(5),dp(9),dp(5));
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(91,103,60));g.setCornerRadius(dp(30));
        t.setBackground(g);
        return t;
    }

    TextView overlay(){
        TextView t=txt(12,true);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(8),dp(5),dp(8),dp(5));
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.argb(190,55,47,34));g.setCornerRadius(dp(12));
        t.setBackground(g);
        return t;
    }

    Button button(String s){
        Button b=new Button(this);
        b.setAllCaps(false);b.setText(s);b.setTextColor(Color.WHITE);
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(99,111,66));g.setCornerRadius(dp(13));
        b.setBackground(g);
        return b;
    }

    void addRoom(LinearLayout l,String label,String id){
        Button b=button(label);b.setTextSize(12);
        b.setOnClickListener(v->{
            room=id;
            walkSteps=0;
            petView.setTranslationX(0);
            refreshRoom();
            save();
        });
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);
        p.setMargins(dp(2),dp(3),dp(2),0);
        l.addView(b,p);
    }

    void refresh(){
        title.setText(pet+" • génération "+generation);
        stage.setText(stageName());
        timer.setText(stage()==Stage.ENDED?"Terminé":format(remain()));
        starTxt.setText("★ "+stars);

        float[] n={hunger,thirst,clean,affection,happy,energy};
        for(int i=0;i<6;i++){
            int v=Math.round(n[i]);
            bars[i].setProgress(v);vals[i].setText(v+"%");
        }

        skillTxt.setText(
            "Compétences — Propreté "+Math.round(skillClean)+
            " • Obéissance "+Math.round(skillObedience)+
            " • Délicatesse "+Math.round(skillCare)
        );

        moodLabel.setText("Humeur : "+moodText());
        eventLabel.setText(incident.isEmpty()?"Aucune bêtise":"⚠ "+incident);
        refreshRoom();
        if(stage()==Stage.ENDED)endLife();
    }

    void refreshRoom(){
        int r=room.equals("cuisine")?R.drawable.room_kitchen:
              room.equals("bain")?R.drawable.room_bathroom:
              room.equals("jardin")?R.drawable.room_garden:
              R.drawable.room_living;
        bg.setImageResource(r);
        actionBtn.setText("Actions • "+(
            room.equals("cuisine")?"Cuisine":
            room.equals("bain")?"Salle de bain":
            room.equals("jardin")?"Jardin":"Salon"
        ));
    }

    String moodText(){
        if(stage()==Stage.ENDED)return "Paisible";
        if(!incident.isEmpty())return "Coupable / inquiet";
        long now=System.currentTimeMillis();
        if(now<manualUntil){
            if(manualFrame==11||manualFrame==12)return "Très affectueux";
            if(manualFrame==10)return "Joueur";
            if(manualFrame==9)return "Endormi";
            if(manualFrame==2)return "Fâché";
            if(manualFrame==3)return "Triste";
        }
        if(energy<18)return "Épuisé";
        if(hunger<18)return "Affamé";
        if(thirst<18)return "Assoiffé";
        if(clean<18)return "Sale";
        if(affection<20)return "En manque de câlins";
        if(happy<25)return "Triste";
        if(happy>82&&affection>72)return "Très heureux";
        if(happy>62)return "Content";
        return "Calme";
    }

    int moodFrame(){
        if(!incident.isEmpty())return 7;
        if(energy<18)return 5;
        if(hunger<18||thirst<18)return 7;
        if(clean<18||affection<20||happy<25)return 3;
        if(happy>82&&affection>72)return 11;
        if(happy>62)return (idleTick%3==0)?1:0;
        return (idleTick%5==0)?6:0;
    }

    void ensureSprites(){
        Stage s=stage();
        if(s==Stage.ENDED)s=Stage.OLD;
        if(spriteStage==s&&stateFrames!=null&&walkFrames!=null)return;

        recycleFrames(stateFrames);recycleFrames(walkFrames);
        stateFrames=null;walkFrames=null;
        spriteStage=s;

        int stateRes=s==Stage.CUB?R.drawable.leopard_cub_state_strip:
                     s==Stage.TEEN?R.drawable.leopard_teen_state_strip:
                     s==Stage.ADULT?R.drawable.leopard_adult_state_strip:
                     R.drawable.leopard_old_state_strip;
        int walkRes=s==Stage.CUB?R.drawable.leopard_cub_walk_strip:
                    s==Stage.TEEN?R.drawable.leopard_teen_walk_strip:
                    s==Stage.ADULT?R.drawable.leopard_adult_walk_strip:
                    R.drawable.leopard_old_walk_strip;

        stateFrames=slice(BitmapFactory.decodeResource(getResources(),stateRes));
        walkFrames=slice(BitmapFactory.decodeResource(getResources(),walkRes));
    }

    Bitmap[] slice(Bitmap strip){
        if(strip==null)return new Bitmap[0];
        int h=strip.getHeight();
        int count=Math.max(1,strip.getWidth()/Math.max(1,h));
        Bitmap[] out=new Bitmap[count];
        int w=strip.getWidth()/count;
        for(int i=0;i<count;i++)out[i]=Bitmap.createBitmap(strip,i*w,0,w,strip.getHeight());
        strip.recycle();
        return out;
    }

    void recycleFrames(Bitmap[] f){
        if(f==null)return;
        for(Bitmap b:f)if(b!=null&&!b.isRecycled())b.recycle();
    }

    void showState(int idx){
        ensureSprites();
        if(stateFrames==null||stateFrames.length==0)return;
        idx=Math.max(0,Math.min(idx,stateFrames.length-1));
        petView.setImageBitmap(stateFrames[idx]);
    }

    void showWalk(){
        ensureSprites();
        if(walkFrames==null||walkFrames.length==0){showState(0);return;}
        petView.setImageBitmap(walkFrames[walkFrame%walkFrames.length]);
        walkFrame++;
    }

    void showAction(int frame,int durationMs){
        manualFrame=frame;
        manualUntil=System.currentTimeMillis()+durationMs;
        walkSteps=0;
        petView.setScaleX(1f);
        showState(frame);
    }

    void animateAuto(){
        if(scene==null||petView==null)return;
        ensureSprites();
        long now=System.currentTimeMillis();
        idleTick++;

        if(now<manualUntil){showState(manualFrame);return;}
        if(stage()==Stage.ENDED){walkSteps=0;showState(9);return;}
        if(energy<12){walkSteps=0;showState(9);return;}
        if(!incident.isEmpty()){walkSteps=0;showState(7);return;}

        if(walkSteps>0){
            showWalk();movePet();walkSteps--;
            if(walkSteps==0)nextWalkAt=now+1800+rnd.nextInt(3500);
            return;
        }

        showState(moodFrame());

        if(nextWalkAt==0)nextWalkAt=now+1500+rnd.nextInt(2500);
        if(now>=nextWalkAt&&energy>24){
            int chance=stage()==Stage.OLD?25:45;
            if(rnd.nextInt(100)<chance){
                walkSteps=7+rnd.nextInt(stage()==Stage.OLD?6:13);
                if(Math.abs(petView.getTranslationX())<dp(20))walkDir=rnd.nextBoolean()?1:-1;
                nextWalkAt=now+2500;
            }else{
                nextWalkAt=now+1800+rnd.nextInt(3200);
            }
        }
    }

    void movePet(){
        if(scene.getWidth()<=0||petView.getWidth()<=0)return;
        float max=Math.max(0,(scene.getWidth()-petView.getWidth())/2f-dp(10));
        float nx=petView.getTranslationX()+walkDir*dp(stage()==Stage.OLD?7:11);
        if(nx>max){nx=max;walkDir=-1;}
        if(nx<-max){nx=-max;walkDir=1;}
        petView.setTranslationX(nx);
        petView.setScaleX(walkDir<0?-1f:1f);
    }

    void maybeMischief(){
        if(stage()==Stage.ENDED)return;
        long now=System.currentTimeMillis();
        if(nextMischiefAt==0){
            nextMischiefAt=now+(25+rnd.nextInt(36))*1000L;
            save();return;
        }
        if(now<nextMischiefAt)return;
        nextMischiefAt=now+(35+rnd.nextInt(56))*1000L;

        if(!incident.isEmpty()){save();return;}

        float base=stage()==Stage.CUB?.42f:
                   stage()==Stage.TEEN?.30f:
                   stage()==Stage.ADULT?.16f:.07f;
        float learned=(skillObedience+skillCare)/200f;
        float chance=base*(1f-.70f*learned);
        if(happy<35)chance+=.08f;
        if(affection<30)chance+=.07f;
        if(hunger<25)chance+=.05f;

        if(rnd.nextFloat()<chance){
            incident=randomIncident();
            clean=clamp(clean-4);
            happy=clamp(happy-2);
            showAction(4,1200);
            Toast.makeText(this,pet+" a fait une bêtise !",Toast.LENGTH_LONG).show();
        }else{
            skillObedience=clamp(skillObedience+.15f);
            skillCare=clamp(skillCare+.12f);
        }
        save();
    }

    String randomIncident(){
        String[] salon={"a griffé le canapé","a renversé les coussins","a fait tomber un objet"};
        String[] cuisine={"a renversé sa gamelle","a fouillé la poubelle","a fait tomber un pot"};
        String[] bain={"a déroulé le papier toilette","a éclaboussé partout","a renversé les serviettes"};
        String[] jardin={"a déterré des fleurs","a renversé l'arrosoir","a cassé une petite branche"};
        String[] a=room.equals("cuisine")?cuisine:room.equals("bain")?bain:room.equals("jardin")?jardin:salon;
        return a[rnd.nextInt(a.length)];
    }

    void punish(){
        if(stage()==Stage.ENDED){endLife();return;}
        if(!incident.isEmpty()){
            String old=incident;
            incident="";
            skillObedience=clamp(skillObedience+4);
            skillCare=clamp(skillCare+2);
            happy=clamp(happy-4);
            affection=clamp(affection-3);
            stars+=1;
            showAction(7,2200);
            save();refresh();
            new AlertDialog.Builder(this)
                .setTitle("Il a compris")
                .setMessage(pet+" "+old+".\n\nLa correction était justifiée : obéissance et délicatesse progressent.")
                .setPositiveButton("OK",null).show();
        }else{
            happy=clamp(happy-18);
            affection=clamp(affection-12);
            skillObedience=clamp(skillObedience-1);
            showAction(3,2600);
            save();refresh();
            new AlertDialog.Builder(this)
                .setTitle("Punition injuste")
                .setMessage(pet+" n'avait rien fait. Son bonheur et son besoin de câlins baissent.")
                .setPositiveButton("OK",null).show();
        }
    }

    void actions(){
        if(stage()==Stage.ENDED){endLife();return;}
        if(room.equals("cuisine"))kitchen();
        else if(room.equals("bain"))bath();
        else if(room.equals("jardin"))garden();
        else living();
    }

    void kitchen(){
        String[] x={"💧 Donner de l'eau","🥛 Lait","🥣 Croquettes junior","🍗 Poulet","🐟 Poisson","🥩 Viande","🍪 Friandise"};
        new AlertDialog.Builder(this).setTitle("Cuisine").setItems(x,(d,w)->{
            Stage s=stage();
            if(w==0)act("Il boit",1,0,40,0,0,2,0,0);
            else if(w==1){
                if(s!=Stage.CUB){no("Le lait est réservé au léopardeau.");return;}
                act("Lait",1,25,12,0,2,5,0,0);
            }else if(w==2){
                if(s==Stage.OLD){no("Trop vieux pour les croquettes junior.");return;}
                act("Croquettes",1,34,3,0,0,3,0,0);
            }else if(w==3||w==4){
                if(s==Stage.CUB){no("Disponible à partir de l'adolescence.");return;}
                act(w==3?"Poulet":"Poisson",1,38,3,0,0,5,0,0);
            }else if(w==5){
                if(s!=Stage.ADULT){no("Repas réservé à l'adulte.");return;}
                act("Viande",1,46,0,0,0,6,0,0);
            }else{
                act("Friandise",11,10,0,0,4,12,0,0);
            }
        }).show();
    }

    void bath(){
        String[] x={"🪮 Brosser","🛁 Donner un bain","🧺 Nettoyer le couchage","🧻 Apprendre la propreté","✨ Soin complet"};
        new AlertDialog.Builder(this).setTitle("Salle de bain").setItems(x,(d,w)->{
            if(w==0){
                act("Pelage brossé",11,0,0,24,6,8,0,0);
                skillClean=clamp(skillClean+1);
            }else if(w==1){
                act("Tout propre",4,0,0,44,1,3,-4,0);
                skillClean=clamp(skillClean+1.5f);
            }else if(w==2){
                act("Couchage propre",0,0,0,20,2,5,0,0);
            }else if(w==3){
                if(stage()==Stage.OLD){no("Il est trop âgé pour un nouvel apprentissage.");return;}
                float gain=stage()==Stage.CUB?4:2.5f;
                skillClean=clamp(skillClean+gain);
                skillObedience=clamp(skillObedience+1);
                act("Apprentissage de la propreté",6,0,0,8,3,6,-3,1);
            }else{
                if(stage()==Stage.CUB||stage()==Stage.OLD){no("Soin complet réservé à l'ado et à l'adulte.");return;}
                skillClean=clamp(skillClean+2);
                act("Soin complet",11,0,0,30,6,12,4,1);
            }
            save();refresh();
        }).show();
    }

    void living(){
        String[] x={"🤍 Caresser","🎵 Musique","😴 Dormir","🧺 Trouve la friandise","⚽ Attrape la balle","🧠 Apprendre à ne rien casser","🏷️ Changer le nom"};
        new AlertDialog.Builder(this).setTitle("Salon").setItems(x,(d,w)->{
            if(w==0)act("Câlin",11,0,0,0,30,16,3,0);
            else if(w==1)act("Moment calme",0,0,0,0,6,10,12,0);
            else if(w==2)act("Bonne sieste",9,-4,-5,0,0,4,55,0);
            else if(w==3)treatGame();
            else if(w==4){
                if(stage()==Stage.OLD){no("Trop fatigant pour un vieux léopard.");return;}
                ballGame();
            }else if(w==5){
                if(stage()==Stage.OLD){no("Il est trop âgé pour cet apprentissage.");return;}
                float gain=stage()==Stage.CUB?2:4;
                skillCare=clamp(skillCare+gain);
                skillObedience=clamp(skillObedience+1.5f);
                act("Il apprend à être délicat",6,0,-3,-2,4,7,-8,1);
            }else rename(false);
            save();refresh();
        }).show();
    }

    void garden(){
        String[] x={"🌿 Promenade","🎾 Rapporter la balle","🎓 Dressage","🦘 Saut","🏆 Concours","🌸 Repos"};
        new AlertDialog.Builder(this).setTitle("Jardin").setItems(x,(d,w)->{
            Stage s=stage();
            if(w==0){
                skillCare=clamp(skillCare+.5f);
                act("Promenade",10,0,-8,-4,3,14,-10,1);
            }else if(w==1){
                if(s==Stage.CUB||s==Stage.OLD){no("Cette activité n'est pas adaptée à cet âge.");return;}
                skillCare=clamp(skillCare+2);
                act("Rapporte la balle",10,0,-8,-4,4,18,-16,2);
            }else if(w==2){
                if(s==Stage.CUB||s==Stage.OLD){no("Dressage réservé à l'ado et à l'adulte.");return;}
                skillObedience=clamp(skillObedience+5);
                skillCare=clamp(skillCare+1);
                act("Bravo !",1,0,-4,-2,3,14,-12,2);
            }else if(w==3){
                if(s==Stage.CUB||s==Stage.OLD){no("Saut indisponible à cet âge.");return;}
                skillCare=clamp(skillCare+3);
                act("Super saut",10,-5,-8,-2,2,18,-18,2);
            }else if(w==4){
                if(s!=Stage.ADULT){no("Les concours sont réservés à l'adulte.");return;}
                competition();
            }else{
                act("Repos au soleil",9,0,-2,0,2,8,28,0);
            }
            save();refresh();
        }).show();
    }

    void act(String msg,int frame,float h,float t,float c,float af,float ha,float e,int st){
        hunger=clamp(hunger+h);thirst=clamp(thirst+t);clean=clamp(clean+c);
        affection=clamp(affection+af);happy=clamp(happy+ha);energy=clamp(energy+e);
        stars+=st;
        showAction(frame,2300);
        save();refresh();
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    void no(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    void treatGame(){
        int win=rnd.nextInt(3);
        String[] x={"Panier 1","Panier 2","Panier 3"};
        new AlertDialog.Builder(this).setTitle("Trouve la friandise")
            .setMessage("Une friandise est cachée.")
            .setItems(x,(d,w)->{
                if(w==win){
                    stars+=2;happy=clamp(happy+18);affection=clamp(affection+4);hunger=clamp(hunger+8);
                    skillObedience=clamp(skillObedience+.5f);
                    showAction(10,2200);
                    Toast.makeText(this,"Trouvé ! +2 ★",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this,"Raté ! C'était le panier "+(win+1),Toast.LENGTH_SHORT).show();
                }
                save();refresh();
            }).show();
    }

    void ballGame(){
        int score=rnd.nextInt(4)+3;
        int reward=score>=5?3:1;
        stars+=reward;
        happy=clamp(happy+12);affection=clamp(affection+5);energy=clamp(energy-10);
        skillCare=clamp(skillCare+1);
        showAction(10,2200);
        save();refresh();
        new AlertDialog.Builder(this).setTitle("Attrape la balle")
            .setMessage("Score : "+score+"/6\nRécompense : +"+reward+" ★")
            .setPositiveButton("OK",null).show();
    }

    void competition(){
        int skillBonus=Math.round((skillObedience+skillCare)/8f);
        int score=Math.max(0,Math.min(100,Math.round((happy+energy+clean)/3)+skillBonus+rnd.nextInt(31)-15));
        int reward=score>=82?10:score>=68?6:score>=55?3:1;
        stars+=reward;
        happy=clamp(happy+12);affection=clamp(affection+4);
        energy=clamp(energy-22);thirst=clamp(thirst-12);
        skillObedience=clamp(skillObedience+1);
        skillCare=clamp(skillCare+1);
        showAction(10,2400);
        save();refresh();
        new AlertDialog.Builder(this).setTitle("Concours")
            .setMessage("Score : "+score+"/100\nRécompense : +"+reward+" ★")
            .setPositiveButton("OK",null).show();
    }

    void rename(boolean first){
        EditText e=new EditText(this);
        e.setSingleLine();e.setText(first?"":pet);
        new AlertDialog.Builder(this)
            .setTitle(first?"Bienvenue !":"Changer le nom")
            .setMessage(first?"Donne un nom à ton léopardeau.":null)
            .setView(e)
            .setPositiveButton("Valider",(d,w)->{
                String n=e.getText().toString().trim();
                pet=n.isEmpty()?"Léo":n;
                sp.edit().putBoolean("named",true).apply();
                save();refresh();
            })
            .setNegativeButton(first?"Léo":"Annuler",(d,w)->{
                if(first){
                    pet="Léo";
                    sp.edit().putBoolean("named",true).apply();
                    save();refresh();
                }
            })
            .setCancelable(!first).show();
    }

    void endLife(){
        if(endShown||isFinishing())return;
        endShown=true;
        actionBtn.setEnabled(false);punishBtn.setEnabled(false);
        AlertDialog a=new AlertDialog.Builder(this)
            .setTitle("Une belle vie")
            .setMessage(pet+" a terminé son cycle de 13 heures réelles.\n\nTu peux maintenant adopter un nouveau léopardeau.")
            .setPositiveButton("Adopter",(d,w)->newGeneration())
            .setCancelable(false).create();
        a.setOnDismissListener(d->endShown=false);
        a.show();
    }

    void newGeneration(){
        long n=System.currentTimeMillis();
        generation++;born=last=n;
        hunger=85;thirst=85;clean=90;affection=90;happy=90;energy=90;
        skillClean=5;skillObedience=5;skillCare=5;
        incident="";nextMischiefAt=0;room="salon";
        actionBtn.setEnabled(true);punishBtn.setEnabled(true);
        spriteStage=null;
        recycleFrames(stateFrames);recycleFrames(walkFrames);
        stateFrames=null;walkFrames=null;
        petView.setTranslationX(0);petView.setScaleX(1f);
        save();refresh();rename(true);
    }

    String format(long ms){
        long s=Math.max(0,ms/1000),h=s/3600,m=(s%3600)/60,sec=s%60;
        return h>0?String.format(Locale.FRANCE,"%dh %02d:%02d",h,m,sec):
                   String.format(Locale.FRANCE,"%02d:%02d",m,sec);
    }

    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
