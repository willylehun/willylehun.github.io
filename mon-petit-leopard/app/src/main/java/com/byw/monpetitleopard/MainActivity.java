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
    long born,last;
    float hunger=85, thirst=85, clean=90, happy=90, energy=90;
    int stars=0,generation=1; String pet="Léo",room="salon"; boolean endShown=false;
    TextView title,stage,timer,starTxt; ProgressBar[] bars=new ProgressBar[5]; TextView[] vals=new TextView[5];
    ImageView bg,petView; Button actionBtn;
    enum Stage {CUB,TEEN,ADULT,OLD,ENDED}

    @Override public void onCreate(Bundle b){super.onCreate(b);sp=getSharedPreferences("pet",MODE_PRIVATE);load();build();tickNeeds();refresh();if(!sp.getBoolean("named",false))rename(true);}
    @Override protected void onResume(){super.onResume();tickNeeds();handler.post(ticker);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(ticker);tickNeeds();save();}
    final Runnable ticker=new Runnable(){public void run(){tickNeeds();refresh();handler.postDelayed(this,1000);}};

    void load(){long n=System.currentTimeMillis();born=sp.getLong("born",n);last=sp.getLong("last",n);hunger=sp.getFloat("hunger",85);thirst=sp.getFloat("thirst",85);clean=sp.getFloat("clean",90);happy=sp.getFloat("happy",90);energy=sp.getFloat("energy",90);stars=sp.getInt("stars",0);generation=sp.getInt("generation",1);pet=sp.getString("name","Léo");room=sp.getString("room","salon");if(!sp.contains("born"))save();}
    void save(){sp.edit().putLong("born",born).putLong("last",last).putFloat("hunger",hunger).putFloat("thirst",thirst).putFloat("clean",clean).putFloat("happy",happy).putFloat("energy",energy).putInt("stars",stars).putInt("generation",generation).putString("name",pet).putString("room",room).apply();}
    float clamp(float v){return Math.max(0,Math.min(100,v));}
    void tickNeeds(){long n=System.currentTimeMillis(),d=Math.max(0,n-last);float m=d/60000f;if(m>0&&stage()!=Stage.ENDED){hunger-=.45f*m;thirst-=.55f*m;clean-=.13f*m;happy-=.18f*m;energy-=.22f*m;hunger=clamp(hunger);thirst=clamp(thirst);clean=clamp(clean);happy=clamp(happy);energy=clamp(energy);}last=n;}
    Stage stage(){long a=System.currentTimeMillis()-born;if(a<CUB)return Stage.CUB;if(a<CUB+TEEN)return Stage.TEEN;if(a<CUB+TEEN+ADULT)return Stage.ADULT;if(a<LIFE)return Stage.OLD;return Stage.ENDED;}
    long remain(){long a=System.currentTimeMillis()-born;if(a<CUB)return CUB-a;if(a<CUB+TEEN)return CUB+TEEN-a;if(a<CUB+TEEN+ADULT)return CUB+TEEN+ADULT-a;if(a<LIFE)return LIFE-a;return 0;}
    String stageName(){switch(stage()){case CUB:return "Léopardeau";case TEEN:return "Ado";case ADULT:return "Adulte";case OLD:return "Vieux";default:return "Cycle terminé";}}

    void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(10),dp(8),dp(10),dp(8));root.setBackgroundColor(Color.rgb(246,239,221));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);title=txt(22,true);stage=txt(13,false);texts.addView(title);texts.addView(stage);head.addView(texts,new LinearLayout.LayoutParams(0,-2,1));timer=pill();starTxt=pill();head.addView(timer);LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-2,-2);slp.setMargins(dp(6),0,0,0);head.addView(starTxt,slp);root.addView(head);
        LinearLayout needs=new LinearLayout(this);String[] names={"Faim","Eau","Propreté","Bonheur","Énergie"};for(int i=0;i<5;i++){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);TextView n=txt(11,false);n.setText(names[i]);n.setGravity(Gravity.CENTER);box.addView(n);bars[i]=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bars[i].setMax(100);box.addView(bars[i],new LinearLayout.LayoutParams(-1,dp(9)));vals[i]=txt(10,false);vals[i].setGravity(Gravity.CENTER);box.addView(vals[i]);needs.addView(box,new LinearLayout.LayoutParams(0,-2,1));}root.addView(needs);
        FrameLayout scene=new FrameLayout(this);GradientDrawable sg=new GradientDrawable();sg.setColor(Color.WHITE);sg.setCornerRadius(dp(18));scene.setBackground(sg);scene.setClipToOutline(true);bg=new ImageView(this);bg.setScaleType(ImageView.ScaleType.CENTER_CROP);scene.addView(bg,new FrameLayout.LayoutParams(-1,-1));petView=new ImageView(this);petView.setScaleType(ImageView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(250),dp(250));pp.gravity=Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;scene.addView(petView,pp);root.addView(scene,new LinearLayout.LayoutParams(-1,0,1));
        actionBtn=button("Actions");actionBtn.setOnClickListener(v->actions());root.addView(actionBtn,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout rooms=new LinearLayout(this);addRoom(rooms,"Salon","salon");addRoom(rooms,"Cuisine","cuisine");addRoom(rooms,"Bain","bain");addRoom(rooms,"Jardin","jardin");root.addView(rooms);setContentView(root);
    }
    TextView txt(int size,boolean bold){TextView t=new TextView(this);t.setTextSize(size);t.setTextColor(Color.rgb(55,47,34));if(bold)t.setTypeface(null,1);return t;}
    TextView pill(){TextView t=txt(13,false);t.setTextColor(Color.WHITE);t.setPadding(dp(10),dp(6),dp(10),dp(6));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(91,103,60));g.setCornerRadius(dp(30));t.setBackground(g);return t;}
    Button button(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(Color.WHITE);GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(99,111,66));g.setCornerRadius(dp(13));b.setBackground(g);return b;}
    void addRoom(LinearLayout l,String label,String id){Button b=button(label);b.setTextSize(12);b.setOnClickListener(v->{room=id;setSprite("idle");refreshRoom();save();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(2),dp(4),dp(2),0);l.addView(b,p);}

    void refresh(){title.setText(pet+" • génération "+generation);stage.setText(stageName());timer.setText(stage()==Stage.ENDED?"Terminé":format(remain()));starTxt.setText("★ "+stars);float[] n={hunger,thirst,clean,happy,energy};for(int i=0;i<5;i++){int v=Math.round(n[i]);bars[i].setProgress(v);vals[i].setText(v+"%");}refreshRoom();if(stage()==Stage.ENDED)endLife();}
    void refreshRoom(){int r=room.equals("cuisine")?R.drawable.room_kitchen:room.equals("bain")?R.drawable.room_bathroom:room.equals("jardin")?R.drawable.room_garden:R.drawable.room_living;bg.setImageResource(r);actionBtn.setText("Actions • "+(room.equals("cuisine")?"Cuisine":room.equals("bain")?"Salle de bain":room.equals("jardin")?"Jardin":"Salon"));if(petView.getDrawable()==null)setSprite("idle");}
    void setSprite(String action){Stage s=stage();if(s==Stage.ENDED)s=Stage.OLD;int res=s==Stage.CUB?R.drawable.leopard_cub_strip:s==Stage.TEEN?R.drawable.leopard_teen_strip:s==Stage.ADULT?R.drawable.leopard_adult_strip:R.drawable.leopard_old_strip;int idx=action.equals("eat")?1:action.equals("sleep")?2:action.equals("walk")?3:action.equals("play")?4:0;Bitmap strip=BitmapFactory.decodeResource(getResources(),res);if(strip==null)return;int w=strip.getWidth()/5;Bitmap frame=Bitmap.createBitmap(strip,idx*w,0,w,strip.getHeight());petView.setImageDrawable(new BitmapDrawable(getResources(),frame));}
    void animate(String a){setSprite(a);handler.postDelayed(()->{if(!isFinishing())setSprite("idle");},2200);}

    void actions(){if(stage()==Stage.ENDED){endLife();return;}if(room.equals("cuisine"))kitchen();else if(room.equals("bain"))bath();else if(room.equals("jardin"))garden();else living();}
    void kitchen(){String[] x={"💧 Donner de l'eau","🥛 Lait","🥣 Croquettes junior","🍗 Poulet","🐟 Poisson","🥩 Viande","🍪 Friandise"};new AlertDialog.Builder(this).setTitle("Cuisine").setItems(x,(d,w)->{Stage s=stage();if(w==0)act("Il boit","eat",0,35,0,2,0,0);else if(w==1){if(s!=Stage.CUB){no("Le lait est réservé au léopardeau.");return;}act("Lait","eat",25,12,0,5,0,0);}else if(w==2){if(s==Stage.OLD){no("Trop vieux pour les croquettes junior.");return;}act("Croquettes","eat",32,3,0,3,0,0);}else if(w==3||w==4){if(s==Stage.CUB){no("Disponible à partir de l'adolescence.");return;}act(w==3?"Poulet":"Poisson","eat",36,3,0,5,0,0);}else if(w==5){if(s!=Stage.ADULT){no("Repas réservé à l'adulte.");return;}act("Viande","eat",45,0,0,6,0,0);}else act("Friandise","eat",10,0,0,12,0,0);}).show();}
    void bath(){String[] x={"🪮 Brosser","🛁 Donner un bain","🧺 Nettoyer le couchage","✨ Soin complet"};new AlertDialog.Builder(this).setTitle("Salle de bain").setItems(x,(d,w)->{if(w==0)act("Pelage brossé","idle",0,0,24,8,0,0);else if(w==1)act("Tout propre","play",0,0,42,4,-4,0);else if(w==2)act("Couchage propre","idle",0,0,18,5,0,0);else{if(stage()==Stage.CUB||stage()==Stage.OLD){no("Soin complet réservé à l'ado et à l'adulte.");return;}act("Soin complet","idle",0,0,28,12,4,1);}}).show();}
    void living(){String[] x={"🤍 Caresser","🎵 Musique","😴 Dormir","🧺 Trouve la friandise","⚽ Attrape la balle","🏷️ Changer le nom"};new AlertDialog.Builder(this).setTitle("Salon").setItems(x,(d,w)->{if(w==0)act("Câlin","idle",0,0,0,16,3,0);else if(w==1)act("Moment calme","sleep",0,0,0,10,10,0);else if(w==2)act("Bonne sieste","sleep",-4,-5,0,4,48,0);else if(w==3)treatGame();else if(w==4){if(stage()==Stage.OLD){no("Trop fatigant pour un vieux léopard.");return;}ballGame();}else rename(false);}).show();}
    void garden(){String[] x={"🌿 Promenade","🎾 Rapporter la balle","🎓 Dressage","🦘 Saut","🏆 Concours","🌸 Repos"};new AlertDialog.Builder(this).setTitle("Jardin").setItems(x,(d,w)->{Stage s=stage();if(w==0)act("Promenade","walk",0,-8,-4,14,-10,1);else if(w==1){if(s==Stage.CUB||s==Stage.OLD){no("Cette activité n'est pas adaptée à cet âge.");return;}act("Rapporte la balle","play",0,-8,-4,18,-16,2);}else if(w==2){if(s==Stage.CUB||s==Stage.OLD){no("Dressage réservé à l'ado et à l'adulte.");return;}act("Bravo !","play",0,-4,-2,16,-12,2);}else if(w==3){if(s==Stage.CUB||s==Stage.OLD){no("Saut indisponible à cet âge.");return;}act("Super saut","play",-5,-8,-2,18,-18,2);}else if(w==4){if(s!=Stage.ADULT){no("Les concours sont réservés à l'adulte.");return;}competition();}else act("Repos au soleil","sleep",0,-2,0,8,24,0);}).show();}
    void act(String msg,String spr,float h,float t,float c,float ha,float e,int st){hunger=clamp(hunger+h);thirst=clamp(thirst+t);clean=clamp(clean+c);happy=clamp(happy+ha);energy=clamp(energy+e);stars+=st;animate(spr);save();refresh();Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    void no(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    void treatGame(){int win=rnd.nextInt(3);String[] x={"Panier 1","Panier 2","Panier 3"};new AlertDialog.Builder(this).setTitle("Trouve la friandise").setMessage("Une friandise est cachée.").setItems(x,(d,w)->{if(w==win){stars+=2;happy=clamp(happy+18);hunger=clamp(hunger+8);animate("play");Toast.makeText(this,"Trouvé ! +2 ★",Toast.LENGTH_SHORT).show();}else Toast.makeText(this,"Raté ! C'était le panier "+(win+1),Toast.LENGTH_SHORT).show();save();refresh();}).show();}
    void ballGame(){int score=rnd.nextInt(4)+3;int reward=score>=5?3:1;stars+=reward;happy=clamp(happy+12);energy=clamp(energy-10);animate("play");save();refresh();new AlertDialog.Builder(this).setTitle("Attrape la balle").setMessage("Score : "+score+"/6\nRécompense : +"+reward+" ★").setPositiveButton("OK",null).show();}
    void competition(){int score=Math.max(0,Math.min(100,Math.round((happy+energy+clean)/3)+rnd.nextInt(31)-15));int reward=score>=82?10:score>=68?6:score>=55?3:1;stars+=reward;happy=clamp(happy+12);energy=clamp(energy-22);thirst=clamp(thirst-12);animate("play");save();refresh();new AlertDialog.Builder(this).setTitle("Concours").setMessage("Score : "+score+"/100\nRécompense : +"+reward+" ★").setPositiveButton("OK",null).show();}
    void rename(boolean first){EditText e=new EditText(this);e.setSingleLine();e.setText(first?"":pet);new AlertDialog.Builder(this).setTitle(first?"Bienvenue !":"Changer le nom").setMessage(first?"Donne un nom à ton léopardeau.":null).setView(e).setPositiveButton("Valider",(d,w)->{String n=e.getText().toString().trim();pet=n.isEmpty()?"Léo":n;sp.edit().putBoolean("named",true).apply();save();refresh();}).setNegativeButton(first?"Léo":"Annuler",(d,w)->{if(first){pet="Léo";sp.edit().putBoolean("named",true).apply();save();refresh();}}).setCancelable(!first).show();}
    void endLife(){if(endShown||isFinishing())return;endShown=true;actionBtn.setEnabled(false);AlertDialog a=new AlertDialog.Builder(this).setTitle("Une belle vie").setMessage(pet+" a terminé son cycle de 13 heures réelles.\n\nTu peux maintenant adopter un nouveau léopardeau.").setPositiveButton("Adopter",(d,w)->newGeneration()).setCancelable(false).create();a.setOnDismissListener(d->endShown=false);a.show();}
    void newGeneration(){long n=System.currentTimeMillis();generation++;born=last=n;hunger=85;thirst=85;clean=90;happy=90;energy=90;stars=0;room="salon";actionBtn.setEnabled(true);save();setSprite("idle");refresh();rename(true);}
    String format(long ms){long s=Math.max(0,ms/1000),h=s/3600,m=(s%3600)/60,sec=s%60;return h>0?String.format(Locale.FRANCE,"%dh %02d:%02d",h,m,sec):String.format(Locale.FRANCE,"%02d:%02d",m,sec);}
    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
