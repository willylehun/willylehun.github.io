package com.byw.monpetitleopard;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class ObjectSystem {
    static class Item {
        final String id,name,icon,room,group,kind;
        final int hunger,water,clean,affection,happy,energy,stars,frame;
        final boolean cub,teen,adult,old;

        Item(String id,String name,String icon,String room,String group,String kind,
             int hunger,int water,int clean,int affection,int happy,int energy,int stars,int frame,
             boolean cub,boolean teen,boolean adult,boolean old){
            this.id=id; this.name=name; this.icon=icon; this.room=room; this.group=group; this.kind=kind;
            this.hunger=hunger; this.water=water; this.clean=clean; this.affection=affection;
            this.happy=happy; this.energy=energy; this.stars=stars; this.frame=frame;
            this.cub=cub; this.teen=teen; this.adult=adult; this.old=old;
        }
    }

    final MainActivity a;
    final ArrayList<Item> items=new ArrayList<>();
    final Random rnd=new Random();

    ObjectSystem(MainActivity a){
        this.a=a;

        // CUISINE — aucun ustensile.
        add("water","Remplir la gamelle d’eau","💧","cuisine","Boissons","waterbowl",0,0,0,0,0,0,0,1,true,true,true,true);
        add("bottle","Biberon","🍼","cuisine","Boissons","food",24,12,0,3,6,0,0,1,true,false,false,false);
        add("milk","Lait","🥛","cuisine","Boissons","food",18,14,0,2,5,0,0,1,true,true,false,false);

        add("junior","Croquettes junior","🥣","cuisine","Repas","food",34,3,0,0,3,0,0,1,true,true,false,false);
        add("kibble","Croquettes","🟤","cuisine","Repas","food",38,2,0,0,3,0,0,1,false,true,true,true);
        add("wet","Pâtée","🥫","cuisine","Repas","food",42,5,0,1,5,0,0,1,false,true,true,true);
        add("chicken","Poulet","🍗","cuisine","Repas","food",40,2,0,1,6,0,0,1,false,true,true,true);
        add("fish","Poisson","🐟","cuisine","Repas","food",38,3,0,1,7,0,0,1,false,true,true,true);
        add("steak","Viande","🥩","cuisine","Repas","food",48,0,0,1,8,0,0,1,false,false,true,false);

        add("biscuit","Biscuit","🦴","cuisine","Friandises & snacks","treat",10,0,0,5,12,0,0,11,true,true,true,true);
        add("treats","Friandises","🍪","cuisine","Friandises & snacks","treat",8,0,0,7,14,0,0,11,true,true,true,true);
        add("apple","Pomme","🍎","cuisine","Friandises & snacks","snack",8,2,0,0,5,0,0,1,false,true,true,true);
        add("banana","Banane","🍌","cuisine","Friandises & snacks","snack",10,1,0,0,5,0,0,1,false,true,true,true);
        add("watermelon","Pastèque","🍉","cuisine","Friandises & snacks","snack",6,8,0,0,5,0,0,1,false,true,true,true);
        add("carrot","Carotte","🥕","cuisine","Friandises & snacks","snack",7,1,0,0,4,0,0,1,false,true,true,true);
        add("berries","Baies","🫐","cuisine","Friandises & snacks","snack",5,2,0,0,5,0,0,1,false,true,true,true);

        // SALLE DE BAIN — uniquement les quatre éléments demandés.
        add("groom","Toilettage","🪮","bain","Soins","care",0,0,24,7,8,0,0,11,true,true,true,true);
        add("soap","Savon","🧼","bain","Soins","bath",0,0,32,2,3,-3,0,4,true,true,true,true);
        add("comb","Peigne","🪮","bain","Soins","care",0,0,18,6,5,0,0,11,true,true,true,true);
        add("towel","Serviette","🧺","bain","Soins","care",0,0,12,8,6,3,0,11,true,true,true,true);

        // SALON
        add("tennis","Balle de tennis","🎾","salon","Jouets","toy",0,-4,-2,5,16,-12,1,10,true,true,true,true);
        add("yarn","Pelote","🧶","salon","Jouets","toy",0,-2,-2,4,12,-7,1,10,true,true,true,true);
        add("mouse","Souris","🐭","salon","Jouets","toy",0,-3,-2,5,14,-9,1,10,true,true,true,true);
        add("plush","Peluche","🧸","salon","Jouets","toy",0,0,0,9,10,-3,0,11,true,true,true,true);
        add("rope","Corde","🪢","salon","Jouets","rope",0,-4,-3,5,15,-13,1,10,true,true,true,true);

        add("bed","Repos","🛏️","salon","Repos","rest",-3,-3,0,5,7,42,0,9,true,true,true,true);

        // JARDIN — promenade, jouets du salon, griffoir et repos au soleil.
        add("walk","Promenade","🌿","jardin","Jardin","activity",0,-7,-3,4,14,-10,1,10,true,true,true,true);
        add("scratch","Griffoir","🐾","jardin","Jardin","scratcher",0,-2,-1,3,9,-7,1,6,true,true,true,true);
        add("sun","Repos au soleil","☀️","jardin","Jardin","rest",0,-2,0,3,8,28,0,9,true,true,true,true);

        add("tennis","Balle de tennis","🎾","jardin","Jouets","toy",0,-4,-2,5,16,-12,1,10,true,true,true,true);
        add("yarn","Pelote","🧶","jardin","Jouets","toy",0,-2,-2,4,12,-7,1,10,true,true,true,true);
        add("mouse","Souris","🐭","jardin","Jouets","toy",0,-3,-2,5,14,-9,1,10,true,true,true,true);
        add("plush","Peluche","🧸","jardin","Jouets","toy",0,0,0,9,10,-3,0,11,true,true,true,true);
        add("rope","Corde","🪢","jardin","Jouets","rope",0,-4,-3,5,15,-13,1,10,true,true,true,true);
    }

    void add(String id,String name,String icon,String room,String group,String kind,
             int h,int w,int c,int af,int happy,int energy,int stars,int frame,
             boolean cub,boolean teen,boolean adult,boolean old){
        items.add(new Item(id,name,icon,room,group,kind,h,w,c,af,happy,energy,stars,frame,cub,teen,adult,old));
    }

    Item findById(String id,String room){
        for(Item i:items){
            if(i.id.equals(id) && (room==null || i.room.equals(room)))return i;
        }
        return null;
    }

    boolean allowed(Item i){
        switch(a.stage()){
            case CUB:return i.cub;
            case TEEN:return i.teen;
            case ADULT:return i.adult;
            case OLD:return i.old;
            default:return false;
        }
    }

    ArrayList<Item> itemsFor(String room,String group){
        ArrayList<Item> out=new ArrayList<>();
        for(Item i:items){
            if(i.room.equals(room) && (group==null || i.group.equals(group))) out.add(i);
        }
        return out;
    }

    void openMenu(){
        if(a.room.equals("bain")){
            openItems("Salle de bain",itemsFor("bain",null));
        } else if(a.room.equals("jardin")){
            String[] groups={"Jouets","Jardin"};
            new AlertDialog.Builder(a).setTitle("Jardin").setItems(groups,(d,w)->
                openItems(groups[w],itemsFor("jardin",groups[w]))).show();
        } else if(a.room.equals("cuisine")){
            String[] groups={"Boissons","Repas","Friandises & snacks"};
            new AlertDialog.Builder(a).setTitle("Cuisine").setItems(groups,(d,w)->openItems(groups[w],itemsFor("cuisine",groups[w]))).show();
        } else {
            String[] groups={"Jouets","Repos"};
            new AlertDialog.Builder(a).setTitle("Salon").setItems(groups,(d,w)->openItems(groups[w],itemsFor("salon",groups[w]))).show();
        }
    }

    void openItems(String title,ArrayList<Item> list){
        CharSequence[] labels=new CharSequence[list.size()];
        for(int n=0;n<list.size();n++){
            Item i=list.get(n);
            int res=toyMenuDrawable(i.id);
            if(res!=0){
                SpannableString row=new SpannableString("   "+i.name+(allowed(i)?"":"  🔒"));
                try{
                    Drawable d=a.getDrawable(res);
                    int s=a.dp(30);
                    d.setBounds(0,0,s,s);
                    row.setSpan(new ImageSpan(d,ImageSpan.ALIGN_CENTER),0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    labels[n]=row;
                }catch(Throwable ignored){
                    labels[n]=i.icon+"  "+i.name+(allowed(i)?"":"  🔒");
                }
            }else{
                labels[n]=i.icon+"  "+i.name+(allowed(i)?"":"  🔒");
            }
        }
        new AlertDialog.Builder(a).setTitle(title).setItems(labels,(d,w)->use(list.get(w))).show();
    }

    int toyMenuDrawable(String id){
        if("tennis".equals(id))return R.drawable.toy_tennis_art;
        if("yarn".equals(id))return R.drawable.toy_yarn_art;
        if("mouse".equals(id))return R.drawable.toy_mouse_art;
        if("plush".equals(id))return R.drawable.toy_plush_art;
        if("rope".equals(id))return R.drawable.toy_rope_art;
        if("scratch".equals(id))return R.drawable.garden_scratcher;
        return 0;
    }

    void use(Item i){
        if(!allowed(i)){
            a.toast("Cet objet n’est pas adapté à l’âge actuel.");
            return;
        }

        if("waterbowl".equals(i.kind)){
            a.sp.edit().putBoolean("waterBowlAvailable",true).putFloat("waterBowlAmount",100f).apply();
            a.toast("💧 La gamelle d’eau est pleine et reste à disposition dans la cuisine.");
            a.addHistory("Gamelle d’eau remplie.");
            a.save();
            a.refresh();
            return;
        }

        if("walk".equals(i.id)){
            a.startPromenade(i);
            return;
        }

        if("scratch".equals(i.id)){
            PetBehavior.Result result=a.behavior.begin("scratch",i);
            if(result.noEffect()){a.behavior.react(i,result);return;}
            a.gardenActionResult=result;
            if(a.gardenGames!=null)a.gardenGames.startScratcher(i);
            return;
        }

        if(("salon".equals(i.room)||"jardin".equals(i.room)) && ("tennis".equals(i.id)
                ||"yarn".equals(i.id)||"mouse".equals(i.id)||"plush".equals(i.id))){
            PetBehavior.Result result=a.behavior.begin("toy:"+i.id,i);
            if(result.preference==PetBehavior.HATE){a.behavior.react(i,result);a.happy=a.clamp(a.happy-7f);a.save();a.refresh();return;}
            if(result.noEffect()){a.behavior.react(i,result);return;}
            a.gameActionResult=result;
            if(a.games!=null)a.games.startFetch(i);
            return;
        }

        if("rope".equals(i.kind)){
            PetBehavior.Result result=a.behavior.begin("toy:"+i.id,i);
            if(result.preference==PetBehavior.HATE){a.behavior.react(i,result);a.happy=a.clamp(a.happy-7f);a.save();a.refresh();return;}
            if(result.noEffect()){a.behavior.react(i,result);return;}
            a.gameActionResult=result;
            if(a.games!=null)a.games.startRope(i);
            return;
        }

        if("rest".equals(i.kind)){
            a.wakeForAction();
            a.beginAutoSleep();
            a.toast("😴 "+a.pet+" se repose.");
            return;
        }

        PetBehavior.Result result=a.behavior.begin("item:"+i.id,i);
        if(result.noEffect()){
            a.behavior.react(i,result);
            return;
        }
        if(a.behavior.isFood(i) && result.preference==PetBehavior.HATE){
            a.happy=a.clamp(a.happy+a.behavior.happinessDelta(i,result));
            a.clean=a.clamp(a.clean-1f);
            a.showFaceMoodNow(3,4200L);
            a.behavior.react(i,result);
            a.save();a.refresh();
            return;
        }

        if(i.id.equals("groom")||i.id.equals("comb")){
            a.skillClean=a.clamp(a.skillClean+1.2f);
        }
        if(i.kind.equals("toy")||i.kind.equals("activity")){
            a.skillCare=a.clamp(a.skillCare+.5f);
        }

        if(i.kind.equals("treat") && a.hunger>88){
            a.clean=a.clamp(a.clean-2);
            a.happy=a.clamp(a.happy-2);
        }

        String animation=null;
        if(i.id.equals("bottle"))animation="bottle";
        else if(i.id.equals("groom"))animation="groom_foam";
        else if(i.id.equals("soap"))animation="soap";
        else if(i.id.equals("comb"))animation="comb";
        else if(i.id.equals("towel"))animation="towel";
        else if(i.kind.equals("food")||i.kind.equals("snack")||i.kind.equals("treat"))animation="eat";
        else if(i.kind.equals("toy")||i.kind.equals("activity"))animation="jump";
        float h=a.behavior.isFood(i)?a.behavior.hungerDelta(i,result):i.hunger*result.factor;
        float af=a.behavior.affectionDelta(i,result);
        float joy=a.behavior.happinessDelta(i,result);
        float dirt=a.behavior.isFood(i)?-Math.max(2f,Math.abs(i.hunger)*.10f):i.clean*result.factor;
        a.act(i.name,i.frame,h,i.water*result.factor,dirt,af,joy,i.energy*result.factor,
            result.factor>=.8f?i.stars:0,animation);
        a.behavior.react(i,result);
        a.save();
        a.refresh();
    }

    String mischief(){
        String[] choices;
        if(a.room.equals("cuisine")){
            choices=new String[]{"a renversé sa gamelle","a répandu les croquettes","a renversé une bouteille","a fouillé les friandises"};
        } else if(a.room.equals("bain")){
            choices=new String[]{"a fait pipi par terre","a fait une crotte","a déroulé le papier toilette","a renversé les serviettes"};
        } else if(a.room.equals("jardin")){
            choices=new String[]{"a déterré une plante","a cassé un pot","a renversé l’arrosoir","a mis de la terre partout"};
        } else {
            choices=new String[]{"a griffé le canapé","a déchiré un coussin","a mâchouillé une chaussure","a déroulé la pelote de laine","a abîmé un livre","a éventré un jouet"};
        }
        return choices[rnd.nextInt(choices.length)];
    }

    String incidentIcon(String incident){
        String s=incident==null?"":incident.toLowerCase(Locale.ROOT);
        if(s.contains("pipi"))return "💦";
        if(s.contains("crotte"))return "💩";
        if(s.contains("papier"))return "🧻";
        if(s.contains("pot")||s.contains("plante"))return "🪴";
        if(s.contains("gamelle")||s.contains("croquette"))return "🥣";
        if(s.contains("coussin")||s.contains("canapé"))return "🛋️";
        if(s.contains("chauss"))return "👟";
        if(s.contains("livre"))return "📕";
        if(s.contains("pelote"))return "🧶";
        if(s.contains("jouet"))return "🧸";
        if(s.contains("bouteille")||s.contains("arrosoir"))return "💧";
        if(s.contains("terre"))return "🟫";
        return "⚠️";
    }
}
