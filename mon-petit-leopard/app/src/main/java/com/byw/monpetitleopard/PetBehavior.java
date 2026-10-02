package com.byw.monpetitleopard;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class PetBehavior {
    static final int HATE=-1,NEUTRAL=0,LIKE=1;
    static final long REPETITION_WINDOW_MS=12L*60L*1000L;

    final MainActivity a;
    final SharedPreferences sp;
    final Random rnd;
    String likedFood,hatedFood,likedToy,hatedToy;

    static final class Result {
        final float factor;
        final int preference;
        final int repetitions;
        Result(float factor,int preference,int repetitions){
            this.factor=factor;this.preference=preference;this.repetitions=repetitions;
        }
        boolean noEffect(){return factor<=0f;}
    }

    PetBehavior(MainActivity a){
        this.a=a;
        this.sp=a.sp;
        long seed=sp.getLong("personalitySeed",0L);
        if(seed==0L){
            seed=System.nanoTime()^(a.profileSlot*1000003L);
            sp.edit().putLong("personalitySeed",seed).apply();
        }
        rnd=new Random(seed);
        ensurePreferences();
    }

    void ensurePreferences(){
        likedFood=sp.getString("likedFood","");
        hatedFood=sp.getString("hatedFood","");
        likedToy=sp.getString("likedToy","");
        hatedToy=sp.getString("hatedToy","");
        if(!likedFood.isEmpty()&&!hatedFood.isEmpty()&&!likedToy.isEmpty()&&!hatedToy.isEmpty())return;

        List<String> foods=new ArrayList<>();
        Collections.addAll(foods,"kibble","wet","chicken","fish","steak","biscuit","treats","apple","banana","watermelon","carrot","berries");
        Collections.shuffle(foods,rnd);
        likedFood=foods.get(0);
        hatedFood=foods.get(1);

        List<String> toys=new ArrayList<>();
        Collections.addAll(toys,"tennis","yarn","mouse","plush","rope","scratch");
        Collections.shuffle(toys,rnd);
        likedToy=toys.get(0);
        hatedToy=toys.get(1);

        sp.edit()
            .putString("likedFood",likedFood).putString("hatedFood",hatedFood)
            .putString("likedToy",likedToy).putString("hatedToy",hatedToy)
            .apply();
    }

    boolean neutralCubFood(ObjectSystem.Item item){
        return a.stage()==MainActivity.Stage.CUB &&
            ("bottle".equals(item.id)||"milk".equals(item.id)||"junior".equals(item.id));
    }

    int preference(ObjectSystem.Item item){
        if(item==null)return NEUTRAL;
        if(neutralCubFood(item))return NEUTRAL;
        if(isFood(item)){
            if(item.id.equals(likedFood))return LIKE;
            if(item.id.equals(hatedFood))return HATE;
        }
        if(isToy(item)){
            if(item.id.equals(likedToy))return LIKE;
            if(item.id.equals(hatedToy))return HATE;
        }
        return NEUTRAL;
    }

    Result begin(String actionKey,ObjectSystem.Item item){
        if("sleep".equals(actionKey))return new Result(1f,NEUTRAL,0);
        long now=System.currentTimeMillis();
        String lastKey=sp.getString("repeatKey","");
        long lastAt=sp.getLong("repeatAt",0L);
        int count=(actionKey.equals(lastKey)&&now-lastAt<=REPETITION_WINDOW_MS)
            ?sp.getInt("repeatCount",0)+1:1;
        sp.edit().putString("repeatKey",actionKey).putLong("repeatAt",now)
            .putInt("repeatCount",count).apply();

        float factor=count<=2?1f:count==3?.65f:count==4?.30f:0f;
        return new Result(factor,preference(item),count);
    }

    float positiveFactor(Result r){
        if(r==null)return 1f;
        return r.factor*(r.preference==LIKE?1.35f:1f);
    }

    float happinessDelta(ObjectSystem.Item item,Result r){
        if(item==null||r==null)return 0f;
        if(r.preference==HATE)return -Math.max(5f,Math.abs(item.happy)*.75f+3f);
        return item.happy*positiveFactor(r);
    }

    float hungerDelta(ObjectSystem.Item item,Result r){
        if(item==null||r==null)return 0f;
        if(r.preference==HATE)return item.hunger*r.factor*.35f;
        return item.hunger*positiveFactor(r);
    }

    float affectionDelta(ObjectSystem.Item item,Result r){
        if(item==null||r==null)return 0f;
        if(r.preference==HATE)return 0f;
        return item.affection*positiveFactor(r);
    }

    void react(ObjectSystem.Item item,Result r){
        if(item==null||r==null)return;
        if(r.preference==HATE){
            a.showFaceMoodNow(3,4200L);
            a.toast("😠 "+a.pet+" n’aime vraiment pas "+item.name.toLowerCase()+".");
        }else if(r.preference==LIKE && r.factor>0f){
            a.showFaceMoodNow(1,4200L);
            a.toast("💚 "+a.pet+" adore "+item.name.toLowerCase()+" !");
        }else if(r.noEffect()){
            a.showFaceMoodNow(5,3600L);
            a.toast("🥱 "+a.pet+" s’est lassé de cette action.");
        }else if(r.factor<1f){
            a.toast("🙂 "+a.pet+" commence à se lasser : l’effet diminue.");
        }
    }

    boolean isFood(ObjectSystem.Item i){
        return i!=null&&("food".equals(i.kind)||"snack".equals(i.kind)||"treat".equals(i.kind));
    }

    boolean isToy(ObjectSystem.Item i){
        return i!=null&&("toy".equals(i.kind)||"rope".equals(i.kind)||"scratcher".equals(i.kind));
    }

}
