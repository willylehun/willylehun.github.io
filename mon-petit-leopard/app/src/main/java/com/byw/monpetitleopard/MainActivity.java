package com.byw.monpetitleopard;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final long H=3600000L, CUB=H, TEEN=5*H, ADULT=5*H, OLD=2*H, LIFE=13*H;

    final Handler handler=new Handler(Looper.getMainLooper());
    final Random rnd=new Random();
    SharedPreferences sp;

    long born,last,lastGoodTick;
    float hunger=85, thirst=85, clean=90, affection=90, happy=90, sleep=90;
    float skillClean=5, skillObedience=5, skillAgility=5;
    int stars=0,generation=1;
    String pet="Léo",room="salon";
    boolean endShown=false, pendingMischief=false;
    String mischief="";
    String moodOverride="";
    long moodUntil=0;

    TextView title,stage,timer,starTxt,moodTxt,eventTxt,skillsTxt;
    ProgressBar[] bars=new ProgressBar[6];
    TextView[] vals=new TextView[6];
    ImageView bg,petView;
    FrameLayout scene;
    Button actionBtn,punishBtn;

    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sp=getSharedPreferences("pet",MODE_PRIVATE);
        load();
        build();
        tickNeeds();
        refresh();
        if(!sp.getBoolean("named",false)) rename(true);
    }

    @Override protected void onResume(){
        super.onResume();
        tickNeeds();
        handler.post(ticker);
        handler.post(autoPet);
        handler.postDelayed(randomEvents,30000);
    }

    @Override protected void onPause(){
        super.onPause();
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(autoPet);
        handler.removeCallbacks(randomEvents);
        tickNeeds();
        save();
    }

    final Runnable ticker=new Runnable(){
        public void run(){
            tickNeeds();
            refresh();
            handler.postDelayed(this,1000);
        }
    };

    final Runnable autoPet=new Runnable(){
        public void run(){
            if(stage()!=Stage.ENDED && scene!=null && petView!=null){
                autonomousMove();
            }
            handler.postDelayed(this,2200+rnd.nextInt(2200));
        }
    };

    final Runnable randomEvents=new Runnable(){
        public void run(){
            if(stage()!=Stage.ENDED && !pendingMischief){
                maybeMischief();
            }
            handler.postDelayed(this,45000);
        }
    };

    void load(){
        long n=System.currentTimeMillis();
        born=sp.getLong("born",n);
        last=sp.getLong("last",n);
        lastGoodTick=sp.getLong("lastGoodTick",n);
        hunger=sp.getFloat("hunger",85);
        thirst=sp.getFloat("thirst",85);
        clean=sp.getFloat("clean",90);
        affection=sp.getFloat("affection",90);
        happy=sp.getFloat("happy",90);
        sleep=sp.getFloat("sleep",90);
        skillClean=sp.getFloat("skillClean",5);
        skillObedience=sp.getFloat("skillObedience",5);
        skillAgility=sp.getFloat("skillAgility",5);
        stars=sp.getInt("stars",0);
        generation=sp.getInt("generation",1);
        pet=sp.getString("name","Léo");
        room=sp.getString("room","salon");
        pendingMischief=sp.getBoolean("pendingMischief",false);
        mischief=sp.getString("mischief","");
        if(!sp.contains("born")) save();
    }

    void save(){
        sp.edit()
          .putLong("born",born).putLong("last",last).putLong("lastGoodTick",lastGoodTick)
          .putFloat("hunger",hunger).putFloat("thirst",thirst).putFloat("clean",clean)
          .putFloat("affection",affection).putFloat("happy",happy).putFloat("sleep",sleep)
          .putFloat("skillClean",skillClean).putFloat("skillObedience",skillObedience).putFloat("skillAgility",skillAgility)
          .putInt("stars",stars).putInt("generation",generation)
          .putString("name",pet).putString("room",room)
          .putBoolean("pendingMischief",pendingMischief).putString("mischief",mischief)
          .apply();
    }

    float clamp(float v){ return Math.max(0,Math.min(100,v)); }

    void tickNeeds(){
        long n=System.currentTimeMillis();
        long d=Math.max(0,n-last);
        float m=d/60000f;
        if(m>0 && stage()!=Stage.ENDED){
            hunger=clamp(hunger-.44f*m);
            thirst=clamp(thirst-.53f*m);
            clean=clamp(clean-.12f*m);
            affection=clamp(affection-.11f*m);
            happy=clamp(happy-.10f*m);
            sleep=clamp(sleep-.20f*m);

            if(hunger<20 || thirst<20 || clean<20 || affection<20 || sleep<20)
                happy=clamp(happy-.18f*m);

            if(n-lastGoodTick>300000L && !pendingMischief){
                skillObedience=clamp(skillObedience+.25f);
                skillClean=clamp(skillClean+.15f);
                lastGoodTick=n;
            }
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
        root.setPadding(dp(10),dp(7),dp(10),dp(7));
        root.setBackgroundColor(Color.rgb(246,239,221));

        LinearLayout head=new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts=new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        title=txt(21,true);
        stage=txt(12,false);
        texts.addView(title); texts.addView(stage);
        head.addView(texts,new LinearLayout.LayoutParams(0,-2,1));

        timer=pill(); starTxt=pill();
        head.addView(timer);
        LinearLayout.LayoutParams spm=new LinearLayout.LayoutParams(-2,-2); spm.setMargins(dp(6),0,0,0);
        head.addView(starTxt,spm);
        root.addView(head);

        moodTxt=txt(14,true);
        moodTxt.setGravity(Gravity.CENTER);
        root.addView(moodTxt,new LinearLayout.LayoutParams(-1,dp(28)));

        LinearLayout needs=new LinearLayout(this);
        String[] names={"Faim","Eau","Propreté","Caresse","Bonheur","Sommeil"};
        for(int i=0;i<6;i++){
            LinearLayout box=new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            TextView n=txt(10,false); n.setText(names[i]); n.setGravity(Gravity.CENTER);
            box.addView(n);
            bars[i]=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
            bars[i].setMax(100);
            box.addView(bars[i],new LinearLayout.LayoutParams(-1,dp(8)));
            vals[i]=txt(9,false); vals[i].setGravity(Gravity.CENTER);
            box.addView(vals[i]);
            needs.addView(box,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(needs);

        skillsTxt=txt(11,false);
        skillsTxt.setGravity(Gravity.CENTER);
        root.addView(skillsTxt,new LinearLayout.LayoutParams(-1,dp(25)));

        eventTxt=txt(12,true);
        eventTxt.setGravity(Gravity.CENTER);
        eventTxt.setTextColor(Color.rgb(145,55,38));
        root.addView(eventTxt,new LinearLayout.LayoutParams(-1,dp(27)));

        scene=new FrameLayout(this);
        GradientDrawable sg=new GradientDrawable();
        sg.setColor(Color.WHITE); sg.setCornerRadius(dp(18));
        scene.setBackground(sg); scene.setClipToOutline(true);

        bg=new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        scene.addView(bg,new FrameLayout.LayoutParams(-1,-1));

        petView=new ImageView(this);
        petView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(235),dp(235));
        pp.gravity=Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;
        pp.bottomMargin=dp(4);
        scene.addView(petView,pp);
        root.addView(scene,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout controls=new LinearLayout(this);
        actionBtn=button("Actions");
        punishBtn=button("Punir");
        actionBtn.setOnClickListener(v->actions());
        punishBtn.setOnClickListener(v->punish());
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(46),2); cp.setMargins(dp(2),dp(4),dp(2),0);
        LinearLayout.LayoutParams pp2=new LinearLayout.LayoutParams(0,dp(46),1); pp2.setMargins(dp(2),dp(4),dp(2),0);
        controls.addView(actionBtn,cp); controls.addView(punishBtn,pp2);
        root.addView(controls);

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
        t.setTextSize(size);
        t.setTextColor(Color.rgb(55,47,34));
        if(bold)t.setTypeface(null,1);
        return t;
    }

    TextView pill(){
        TextView t=txt(12,false);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(10),dp(5),dp(10),dp(5));
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(91,103,60));
        g.setCornerRadius(dp(30));
        t.setBackground(g);
        return t;
    }

    Button button(String s){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(99,111,66));
        g.setCornerRadius(dp(13));
        b.setBackground(g);
        return b;
    }

    void addRoom(LinearLayout l,String label,String id){
        Button b=button(label);
        b.setTextSize(12);
        b.setOnClickListener(v->{room=id;setSprite();refreshRoom();save();});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);
        p.setMargins(dp(2),dp(4),dp(2),0);
        l.addView(b,p);
    }

    void refresh(){
        title.setText(pet+" • génération "+generation);
        stage.setText(stageName()+" • "+ageRules());
        timer.setText(stage()==Stage.ENDED?"Terminé":format(remain()));
        starTxt.setText("★ "+stars);

        float[] n={hunger,thirst,clean,affection,happy,sleep};
        for(int i=0;i<6;i++){
            int v=Math.round(n[i]);
            bars[i].setProgress(v);
            vals[i].setText(v+"%");
        }

        moodTxt.setText("Humeur : "+mood());
        skillsTxt.setText(String.format(Locale.FRANCE,
            "Compétences  •  Propreté %d  •  Obéissance %d  •  Adresse %d",
            Math.round(skillClean),Math.round(skillObedience),Math.round(skillAgility)));

        if(pendingMischief){
            eventTxt.setText("Bêtise : "+mischief);
            punishBtn.setText("Punir la bêtise");
        }else{
            eventTxt.setText("Il se comporte bien pour le moment.");
            punishBtn.setText("Punir");
        }

        refreshRoom();
        if(stage()==Stage.ENDED)endLife();
    }

    String ageRules(){
        if(stage()==Stage.CUB)return "activités douces";
        if(stage()==Stage.OLD)return "rythme tranquille";
        return "plein d'énergie";
    }

    String mood(){
        if(System.currentTimeMillis()<moodUntil && !moodOverride.isEmpty()) return moodOverride;
        if(pendingMischief) return "Coupable";
        if(hunger<18) return "Affamé";
        if(thirst<18) return "Assoiffé";
        if(sleep<18) return "Épuisé";
        if(clean<20) return "Sale et gêné";
        if(affection<20) return "En manque de câlins";
        if(happy<25) return "Triste";
        if(happy>82 && affection>65) return "Très heureux";
        if(sleep<35) return "Fatigué";
        if(hunger<35) return "Il a faim";
        return rnd.nextInt(5)==0 ? "Curieux" : "Calme";
    }

    void setMood(String m,long ms){
        moodOverride=m;
        moodUntil=System.currentTimeMillis()+ms;
    }

    void refreshRoom(){
        int r=room.equals("cuisine")?R.drawable.room_kitchen:
              room.equals("bain")?R.drawable.room_bathroom:
              room.equals("jardin")?R.drawable.room_garden:R.drawable.room_living;
        bg.setImageResource(r);
        actionBtn.setText("Actions • "+(room.equals("cuisine")?"Cuisine":room.equals("bain")?"Salle de bain":room.equals("jardin")?"Jardin":"Salon"));
        setSprite();
    }

    void setSprite(){
        Stage s=stage();
        if(s==Stage.ENDED)s=Stage.OLD;
        int res=s==Stage.CUB?R.drawable.leopard_cub:
                s==Stage.TEEN?R.drawable.leopard_teen:
                s==Stage.ADULT?R.drawable.leopard_adult:R.drawable.leopard_old;
        petView.setImageResource(res);
    }

    void autonomousMove(){
        if(stage()==Stage.ENDED)return;

        if(sleep<18){
            setMood("Épuisé",2500);
            petView.animate().cancel();
            petView.animate().translationY(dp(12)).rotation(-2).setDuration(900).start();
            return;
        }

        int width=scene.getWidth();
        if(width<=0)return;

        float max=Math.max(dp(80),width*0.29f);
        float target=(rnd.nextFloat()*2f-1f)*max;
        float dy=rnd.nextBoolean()?-dp(7):dp(2);
        boolean right=target>=petView.getTranslationX();

        petView.setScaleX(right?1f:-1f);
        long duration=stage()==Stage.OLD?2400:stage()==Stage.CUB?1700:1400;
        petView.animate()
            .translationX(target)
            .translationY(dy)
            .rotation(rnd.nextBoolean()?2f:-2f)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setDuration(duration)
            .withEndAction(()->petView.animate().translationY(0).rotation(0).setDuration(500).start())
            .start();

        if(rnd.nextInt(6)==0 && happy>50){
            setMood("Joueur",2200);
            petView.animate().scaleY(1.08f).setDuration(250).withEndAction(
                ()->petView.animate().scaleY(1f).setDuration(300).start()).start();
        }
    }

    void maybeMischief(){
        Stage s=stage();
        float ageFactor=s==Stage.CUB?0.55f:s==Stage.TEEN?0.45f:s==Stage.ADULT?0.30f:0.16f;
        float skillPenalty=(skillObedience+skillClean+skillAgility)/300f*0.30f;
        float needBonus=(happy<35 || affection<35)?0.16f:0f;
        float chance=Math.max(0.08f,ageFactor-skillPenalty+needBonus);
        if(rnd.nextFloat()>chance)return;

        int type=rnd.nextInt(5);
        if(type==0){
            mischief="il a renversé son bol.";
            clean=clamp(clean-12);
            skillAgility=clamp(skillAgility-1);
        }else if(type==1){
            mischief="il a griffé le canapé.";
            happy=clamp(happy+3);
        }else if(type==2){
            mischief="il a fait pipi au mauvais endroit.";
            clean=clamp(clean-24);
            skillClean=clamp(skillClean-1);
        }else if(type==3){
            mischief="il a cassé un pot de fleurs.";
            skillAgility=clamp(skillAgility-1.5f);
        }else{
            mischief="il a volé de la nourriture.";
            hunger=clamp(hunger+10);
            skillObedience=clamp(skillObedience-1);
        }
        pendingMischief=true;
        setMood("Coupable",60000);
        save();
        refresh();
        Toast.makeText(this,pet+" a fait une bêtise !",Toast.LENGTH_LONG).show();
    }

    void punish(){
        if(stage()==Stage.ENDED)return;
        if(pendingMischief){
            pendingMischief=false;
            mischief="";
            happy=clamp(happy-7);
            affection=clamp(affection-4);
            skillObedience=clamp(skillObedience+7);
            skillClean=clamp(skillClean+2);
            setMood("Vexé mais attentif",18000);
            Toast.makeText(this,"Il comprend que cette bêtise n'était pas permise.",Toast.LENGTH_LONG).show();
        }else{
            happy=clamp(happy-22);
            affection=clamp(affection-16);
            skillObedience=clamp(skillObedience-2);
            setMood("Triste et incompris",30000);
            Toast.makeText(this,"Il n'avait rien fait : son bonheur baisse fortement.",Toast.LENGTH_LONG).show();
        }
        save(); refresh();
    }

    void actions(){
        if(stage()==Stage.ENDED){endLife();return;}
        if(room.equals("cuisine"))kitchen();
        else if(room.equals("bain"))bath();
        else if(room.equals("jardin"))garden();
        else living();
    }

    void kitchen(){
        String[] x={"Donner de l'eau","Lait","Croquettes junior","Poulet","Poisson","Viande","Friandise","Apprendre à ne pas renverser"};
        new AlertDialog.Builder(this).setTitle("Cuisine").setItems(x,(d,w)->{
            Stage s=stage();
            if(w==0)act("Il boit",0,35,0,0,2,0,0,0,0,0);
            else if(w==1){
                if(s!=Stage.CUB){no("Le lait est réservé au léopardeau.");return;}
                act("Lait",25,12,0,3,5,2,0,0,0,0);
            }else if(w==2){
                if(s==Stage.OLD){no("Les croquettes junior ne lui conviennent plus.");return;}
                act("Croquettes",32,3,0,2,3,0,0,0,0,0);
            }else if(w==3||w==4){
                if(s==Stage.CUB){no("Disponible à partir de l'adolescence.");return;}
                act(w==3?"Poulet":"Poisson",36,3,0,2,5,0,0,0,0,0);
            }else if(w==5){
                if(s!=Stage.ADULT){no("Repas réservé à l'adulte.");return;}
                act("Viande",45,0,0,1,6,0,0,0,0,0);
            }else if(w==6){
                act("Friandise",10,0,0,4,12,0,0,0,0,0);
            }else{
                if(s==Stage.CUB){
                    act("Petit apprentissage",0,0,0,5,5,-3,1,4,4,2);
                }else{
                    act("Exercice d'adresse",0,-4,-2,4,7,-7,1,1,4,6);
                }
                if(rnd.nextInt(100)<Math.round(skillAgility)) stars++;
            }
        }).show();
    }

    void bath(){
        String[] x={"Brosser","Donner un bain","Nettoyer le couchage","Apprentissage propreté","Soin complet"};
        new AlertDialog.Builder(this).setTitle("Salle de bain").setItems(x,(d,w)->{
            if(w==0)act("Pelage brossé",0,0,24,6,8,0,0,1,0,0);
            else if(w==1)act("Tout propre",0,0,42,2,4,-4,0,2,0,0);
            else if(w==2)act("Couchage propre",0,0,18,3,5,0,0,2,1,0);
            else if(w==3){
                if(stage()==Stage.OLD){no("À cet âge, on privilégie les habitudes douces.");return;}
                act("Propreté travaillée",0,0,8,4,6,-4,1,8,3,0);
            }else{
                if(stage()==Stage.CUB||stage()==Stage.OLD){no("Soin complet réservé à l'ado et à l'adulte.");return;}
                act("Soin complet",0,0,28,8,12,4,1,4,2,0);
            }
        }).show();
    }

    void living(){
        String[] x={"Caresser","Câlin long","Musique","Dormir","Trouve la friandise","Attrape la balle","Féliciter","Changer le nom"};
        new AlertDialog.Builder(this).setTitle("Salon").setItems(x,(d,w)->{
            if(w==0)act("Caresse",0,0,0,18,12,2,0,0,0,0);
            else if(w==1)act("Gros câlin",0,0,0,28,18,4,0,0,0,0);
            else if(w==2)act("Moment calme",0,0,0,6,10,10,0,0,0,0);
            else if(w==3)act("Bonne sieste",-4,-5,0,2,4,52,0,0,0,0);
            else if(w==4)treatGame();
            else if(w==5){
                if(stage()==Stage.OLD){no("Trop fatigant pour un vieux léopard.");return;}
                ballGame();
            }else if(w==6){
                if(pendingMischief){
                    happy=clamp(happy+2);
                    skillObedience=clamp(skillObedience-2);
                    setMood("Il ne comprend plus les règles",12000);
                    no("Le féliciter juste après une bêtise brouille son apprentissage.");
                }else{
                    affection=clamp(affection+12);
                    happy=clamp(happy+12);
                    skillObedience=clamp(skillObedience+2);
                    stars++;
                    setMood("Fier",10000);
                    save(); refresh();
                }
            }else rename(false);
        }).show();
    }

    void garden(){
        String[] x={"Promenade","Rapporter la balle","Dressage","Saut","Parcours d'adresse","Concours","Repos au soleil"};
        new AlertDialog.Builder(this).setTitle("Jardin").setItems(x,(d,w)->{
            Stage s=stage();
            if(w==0)act("Promenade",0,-8,-4,5,14,-10,1,0,1,2);
            else if(w==1){
                if(s==Stage.CUB||s==Stage.OLD){no("Cette activité n'est pas adaptée à cet âge.");return;}
                act("Rapporte la balle",0,-8,-4,5,18,-16,2,0,2,5);
            }else if(w==2){
                if(s==Stage.CUB||s==Stage.OLD){no("Dressage complet réservé à l'ado et à l'adulte.");return;}
                act("Bravo !",0,-4,-2,5,16,-12,2,0,8,2);
            }else if(w==3){
                if(s==Stage.CUB||s==Stage.OLD){no("Saut indisponible à cet âge.");return;}
                act("Super saut",-5,-8,-2,4,18,-18,2,0,1,8);
            }else if(w==4){
                if(s==Stage.OLD){no("On évite le parcours d'adresse à cet âge.");return;}
                act("Parcours réussi",-3,-7,-2,5,16,-15,3,0,3,10);
            }else if(w==5){
                if(s!=Stage.ADULT){no("Les concours sont réservés à l'adulte.");return;}
                competition();
            }else act("Repos au soleil",0,-2,0,6,8,24,0,0,0,0);
        }).show();
    }

    void act(String msg,float h,float t,float c,float a,float ha,float sl,int st,float skC,float skO,float skA){
        hunger=clamp(hunger+h); thirst=clamp(thirst+t); clean=clamp(clean+c);
        affection=clamp(affection+a); happy=clamp(happy+ha); sleep=clamp(sleep+sl);
        skillClean=clamp(skillClean+skC); skillObedience=clamp(skillObedience+skO); skillAgility=clamp(skillAgility+skA);
        stars+=st;
        setMood(ha>=12?"Heureux":sl>=20?"Reposé":"Content",9000);
        petView.animate().scaleX(1.08f).scaleY(1.08f).setDuration(220).withEndAction(
            ()->petView.animate().scaleX(petView.getScaleX()<0?-1f:1f).scaleY(1f).setDuration(260).start()
        ).start();
        save(); refresh();
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    void no(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    void treatGame(){
        int win=rnd.nextInt(3);
        String[] x={"Panier 1","Panier 2","Panier 3"};
        new AlertDialog.Builder(this).setTitle("Trouve la friandise").setMessage("Une friandise est cachée.").setItems(x,(d,w)->{
            if(w==win){
                stars+=2; happy=clamp(happy+18); affection=clamp(affection+5); hunger=clamp(hunger+8);
                skillObedience=clamp(skillObedience+2);
                setMood("Très fier",10000);
                Toast.makeText(this,"Trouvé ! +2 ★",Toast.LENGTH_SHORT).show();
            }else{
                happy=clamp(happy-2);
                setMood("Curieux",7000);
                Toast.makeText(this,"Raté ! C'était le panier "+(win+1),Toast.LENGTH_SHORT).show();
            }
            save(); refresh();
        }).show();
    }

    void ballGame(){
        int score=Math.min(6,Math.max(1,Math.round(skillAgility/22f)+rnd.nextInt(4)));
        int reward=score>=5?3:1;
        stars+=reward;
        happy=clamp(happy+12);
        affection=clamp(affection+5);
        sleep=clamp(sleep-10);
        skillAgility=clamp(skillAgility+4);
        setMood("Joueur",10000);
        save(); refresh();
        new AlertDialog.Builder(this).setTitle("Attrape la balle").setMessage("Score : "+score+"/6\nRécompense : +"+reward+" ★").setPositiveButton("OK",null).show();
    }

    void competition(){
        int base=Math.round((happy+sleep+clean+skillObedience+skillAgility)/5f);
        int score=Math.max(0,Math.min(100,base+rnd.nextInt(25)-12));
        int reward=score>=82?10:score>=68?6:score>=55?3:1;
        stars+=reward; happy=clamp(happy+12); affection=clamp(affection+4);
        sleep=clamp(sleep-22); thirst=clamp(thirst-12); skillAgility=clamp(skillAgility+3); skillObedience=clamp(skillObedience+2);
        setMood(score>=68?"Fier":"Déterminé",12000);
        save(); refresh();
        new AlertDialog.Builder(this).setTitle("Concours").setMessage("Score : "+score+"/100\nRécompense : +"+reward+" ★").setPositiveButton("OK",null).show();
    }

    void rename(boolean first){
        EditText e=new EditText(this);
        e.setSingleLine(); e.setText(first?"":pet);
        new AlertDialog.Builder(this)
            .setTitle(first?"Bienvenue !":"Changer le nom")
            .setMessage(first?"Donne un nom à ton léopardeau.":null)
            .setView(e)
            .setPositiveButton("Valider",(d,w)->{
                String n=e.getText().toString().trim();
                pet=n.isEmpty()?"Léo":n;
                sp.edit().putBoolean("named",true).apply();
                save(); refresh();
            })
            .setNegativeButton(first?"Léo":"Annuler",(d,w)->{
                if(first){
                    pet="Léo";
                    sp.edit().putBoolean("named",true).apply();
                    save(); refresh();
                }
            }).setCancelable(!first).show();
    }

    void endLife(){
        if(endShown||isFinishing())return;
        endShown=true;
        actionBtn.setEnabled(false); punishBtn.setEnabled(false);
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
        generation++;
        born=last=lastGoodTick=n;
        hunger=85; thirst=85; clean=90; affection=90; happy=90; sleep=90;
        skillClean=5; skillObedience=5; skillAgility=5;
        stars=0; room="salon"; pendingMischief=false; mischief="";
        actionBtn.setEnabled(true); punishBtn.setEnabled(true);
        petView.setTranslationX(0); petView.setTranslationY(0); petView.setRotation(0);
        save(); setSprite(); refresh(); rename(true);
    }

    String format(long ms){
        long s=Math.max(0,ms/1000),h=s/3600,m=(s%3600)/60,sec=s%60;
        return h>0?String.format(Locale.FRANCE,"%dh %02d:%02d",h,m,sec):String.format(Locale.FRANCE,"%02d:%02d",m,sec);
    }

    int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
}
