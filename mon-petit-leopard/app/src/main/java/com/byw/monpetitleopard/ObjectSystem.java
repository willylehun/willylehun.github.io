package com.byw.monpetitleopard;
import android.app.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import java.util.*;

public class ObjectSystem{
 static class I{String n,ic,r,k;int h,t,c,a,j,e,s,f;boolean[] age;
 I(String n,String ic,String r,String k,int h,int t,int c,int a,int j,int e,int s,int f,boolean...age){this.n=n;this.ic=ic;this.r=r;this.k=k;this.h=h;this.t=t;this.c=c;this.a=a;this.j=j;this.e=e;this.s=s;this.f=f;this.age=age;}}
 final MainActivity m;final ArrayList<I> all=new ArrayList<>();final Random q=new Random();
 ObjectSystem(MainActivity m){this.m=m;
  // cuisine
  x("Gamelle d’eau","💧","cuisine","food",0,42,0,0,2,0,0,1,1,1,1,1);x("Biberon","🍼","cuisine","food",24,12,0,3,6,0,0,1,1,0,0,0);
  x("Lait","🥛","cuisine","food",18,14,0,2,5,0,0,1,1,1,0,0);x("Croquettes junior","🥣","cuisine","food",34,3,0,0,3,0,0,1,1,1,0,0);
  x("Croquettes","🟤","cuisine","food",38,2,0,0,3,0,0,1,0,1,1,1);x("Pâtée","🥫","cuisine","food",42,5,0,1,5,0,0,1,0,1,1,1);
  x("Poulet","🍗","cuisine","food",40,2,0,1,6,0,0,1,0,1,1,1);x("Poisson","🐟","cuisine","food",38,3,0,1,7,0,0,1,0,1,1,1);
  x("Viande","🥩","cuisine","food",48,0,0,1,8,0,0,1,0,0,1,0);x("Biscuit","🦴","cuisine","treat",10,0,0,5,12,0,0,11,1,1,1,1);
  x("Friandises","🍪","cuisine","treat",8,0,0,7,14,0,0,11,1,1,1,1);x("Pomme","🍎","cuisine","snack",8,2,0,0,5,0,0,1,0,1,1,1);
  x("Banane","🍌","cuisine","snack",10,1,0,0,5,0,0,1,0,1,1,1);x("Pastèque","🍉","cuisine","snack",6,8,0,0,5,0,0,1,0,1,1,1);
  x("Carotte","🥕","cuisine","snack",7,1,0,0,4,0,0,1,0,1,1,1);x("Baies","🫐","cuisine","snack",5,2,0,0,5,0,0,1,0,1,1,1);
  // bain
  x("Brosse","🪮","bain","care",0,0,22,7,8,0,0,11,1,1,1,1);x("Peigne","🪮","bain","care",0,0,15,5,5,0,0,11,1,1,1,1);
  x("Shampoing","🧴","bain","bath",0,0,38,2,2,-4,0,4,1,1,1,1);x("Mousse de bain","🫧","bain","bath",0,0,30,4,8,-3,0,4,1,1,1,1);
  x("Savon","🧼","bain","bath",0,0,26,1,2,-2,0,4,1,1,1,1);x("Éponge","🧽","bain","bath",0,0,24,3,3,-2,0,4,1,1,1,1);
  x("Serviette","🧺","bain","care",0,0,12,6,5,2,0,11,1,1,1,1);x("Lingettes","🧻","bain","care",0,0,14,1,1,0,0,0,1,1,1,1);
  x("Tapis de propreté","⬜","bain","train",0,0,8,2,4,-2,1,6,1,1,0,0);x("Coupe-griffes","✂️","bain","care",0,0,8,0,-2,-2,1,6,0,1,1,1);
  x("Spray nettoyant","🧴","bain","clean",0,0,20,0,0,0,0,0,1,1,1,1);
  // salon
  x("Balle léopard","⚽","salon","toy",0,-3,-2,5,14,-10,1,10,1,1,1,0);x("Balle de tennis","🎾","salon","toy",0,-4,-2,5,16,-12,1,10,0,1,1,0);
  x("Pelote de laine","🧶","salon","toy",0,-2,-2,4,12,-7,1,10,1,1,1,0);x("Souris jouet","🐭","salon","toy",0,-3,-2,5,14,-9,1,10,1,1,1,0);
  x("Peluche","🧸","salon","toy",0,0,0,9,10,-3,0,11,1,1,1,1);x("Corde","🪢","salon","toy",0,-4,-3,5,15,-13,1,10,0,1,1,0);
  x("Poisson jouet","🐟","salon","toy",0,-3,-2,5,13,-8,1,10,1,1,1,0);x("Tunnel","🟢","salon","toy",0,-4,-2,4,16,-14,2,10,0,1,1,0);
  x("Clicker","🟩","salon","train",0,0,0,3,7,-5,1,6,0,1,1,0);x("Sifflet","📣","salon","train",0,0,0,2,5,-4,1,6,0,1,1,0);
  x("Cocon","🛏️","salon","rest",-3,-3,0,5,7,42,0,9,1,1,1,1);
  // jardin
  x("Promenade","🌿","jardin","activity",0,-7,-3,4,14,-10,1,10,1,1,1,1);x("Rapporter la balle","🎾","jardin","activity",0,-8,-4,5,18,-16,2,10,0,1,1,0);
  x("Anneau d’obstacle","⭕","jardin","train",-3,-7,-3,3,18,-18,2,10,0,1,1,0);x("Canne à plume","🪶","jardin","toy",0,-4,-2,4,15,-11,1,10,1,1,1,0);
  x("Griffoir","🪵","jardin","train",0,-2,-1,3,9,-7,1,6,1,1,1,1);x("Concours","🏅","jardin","contest",0,-12,-4,4,12,-22,4,10,0,0,1,0);
  x("Repos au soleil","☀️","jardin","rest",0,-2,0,3,8,28,0,9,1,1,1,1);
 }
 void x(String n,String ic,String r,String k,int h,int t,int c,int a,int j,int e,int s,int f,int...z){all.add(new I(n,ic,r,k,h,t,c,a,j,e,s,f,z[0]>0,z[1]>0,z[2]>0,z[3]>0));}
 boolean ok(I i){int z=m.stage()==MainActivity.Stage.CUB?0:m.stage()==MainActivity.Stage.TEEN?1:m.stage()==MainActivity.Stage.ADULT?2:3;return m.stage()!=MainActivity.Stage.ENDED&&i.age[z];}
 ArrayList<I> here(){ArrayList<I> z=new ArrayList<>();for(I i:all)if(i.r.equals(m.room))z.add(i);return z;}
 void menu(){ArrayList<I> z=here();String[] s=new String[z.size()];for(int i=0;i<z.size();i++)s[i]=z.get(i).ic+"  "+z.get(i).n+(ok(z.get(i))?"":"  🔒");
  new AlertDialog.Builder(m).setTitle(m.room.equals("cuisine")?"Frigo & repas":m.room.equals("bain")?"Soins & hygiène":m.room.equals("jardin")?"Jardin":"Jouets & repos").setItems(s,(d,w)->use(z.get(w))).show();}
 void use(I i){if(!ok(i)){m.no("Cet objet n’est pas adapté à cet âge.");return;}if(i.k.equals("contest")){m.competition();return;}
  if(i.k.equals("train")){m.skillObedience=m.clamp(m.skillObedience+2);m.skillCare=m.clamp(m.skillCare+1);}
  if(i.n.contains("propreté"))m.skillClean=m.clamp(m.skillClean+(m.stage()==MainActivity.Stage.CUB?5:3));
  if(i.k.equals("toy")||i.k.equals("activity"))m.skillCare=m.clamp(m.skillCare+.5f);
  if(i.k.equals("treat")&&m.hunger>88){m.clean=m.clamp(m.clean-2);m.happy=m.clamp(m.happy-2);}
  m.act(i.n,i.f,i.h,i.t,i.c,i.a,i.j,i.e,i.s);m.save();m.refresh();}
 TextView icon(String s){TextView v=new TextView(m);v.setText(s);v.setTextSize(30);v.setGravity(Gravity.CENTER);GradientDrawable g=new GradientDrawable();g.setColor(Color.argb(210,255,250,238));g.setCornerRadius(m.dp(16));g.setStroke(m.dp(1),Color.argb(80,60,50,35));v.setBackground(g);v.setElevation(m.dp(3));return v;}
 void render(FrameLayout l){if(l==null)return;l.removeAllViews();ArrayList<I> z=here();Collections.shuffle(z,q);for(int n=0;n<Math.min(4,z.size());n++){I i=z.get(n);TextView v=icon(i.ic);v.setAlpha(ok(i)?.96f:.5f);v.setOnClickListener(x->use(i));FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(m.dp(52),m.dp(52));p.gravity=Gravity.BOTTOM|Gravity.LEFT;p.leftMargin=m.dp(18+n*70);p.bottomMargin=m.dp(12+(n%2)*15);l.addView(v,p);}incident(l);}
 void incident(FrameLayout l){if(m.incident.isEmpty())return;String s=m.incident.toLowerCase(Locale.ROOT),ic=s.contains("pipi")?"💦":s.contains("crotte")?"💩":s.contains("papier")?"🧻":s.contains("pot")||s.contains("plante")?"🪴":s.contains("gamelle")?"🥣":s.contains("coussin")?"🛋️":s.contains("chauss")?"👟":s.contains("livre")?"📕":s.contains("pelote")?"🧶":"⚠️";TextView v=icon(ic);v.setTextSize(36);v.setOnClickListener(x->incidentMenu());FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(m.dp(66),m.dp(66));p.gravity=Gravity.BOTTOM|Gravity.RIGHT;p.rightMargin=m.dp(20);p.bottomMargin=m.dp(16);l.addView(v,p);}
 void incidentMenu(){String[] z={"🧽 Nettoyer / réparer","⚠ Punir","🤍 Rassurer et nettoyer","Plus tard"};new AlertDialog.Builder(m).setTitle("Bêtise : "+m.incident).setItems(z,(d,w)->{if(w==0)clean(false);else if(w==1)m.punish();else if(w==2)clean(true);}).show();}
 void clean(boolean soft){if(m.incident.isEmpty())return;String old=m.incident;m.incident="";m.clean=m.clamp(m.clean+18);m.skillClean=m.clamp(m.skillClean+1.5f);if(soft){m.affection=m.clamp(m.affection+5);m.happy=m.clamp(m.happy+3);}m.stars++;m.showAction(soft?11:0,1600);m.save();m.refresh();Toast.makeText(m,"Nettoyé : "+old+"  +1 ★",Toast.LENGTH_SHORT).show();}
 String mischief(){String[] z;if(m.room.equals("cuisine"))z=new String[]{"a renversé sa gamelle","a répandu les croquettes","a renversé une bouteille","a fouillé les friandises"};else if(m.room.equals("bain"))z=new String[]{"a fait pipi par terre","a fait une crotte","a déroulé le papier toilette","a renversé les serviettes"};else if(m.room.equals("jardin"))z=new String[]{"a déterré une plante","a cassé un pot","a renversé l’arrosoir","a mis de la terre partout"};else z=new String[]{"a griffé le canapé","a déchiré un coussin","a mâchouillé une chaussure","a déroulé la pelote de laine","a abîmé un livre","a éventré un jouet"};return z[q.nextInt(z.length)];}
}