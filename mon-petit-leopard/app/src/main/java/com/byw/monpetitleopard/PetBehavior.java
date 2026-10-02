package com.byw.monpetitleopard;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

final class PetBehavior {
    enum Preference {LOVE,NEUTRAL,DISLIKE}

    static final long BOREDOM_RESET_MS=10L*60L*1000L;
    private static final String PERSONALITY_READY="personality_v080_ready";
    private static final String FOOD_LIKES="personality_food_likes";
    private static final String FOOD_DISLIKES="personality_food_dislikes";
    private static final String TOY_LIKES="personality_toy_likes";
    private static final String TOY_DISLIKES="personality_toy_dislikes";

    private static final String[] FOOD_IDS={
        "kibble","wet","chicken","fish","steak",
        "biscuit","treats","apple","banana","watermelon","carrot","berries"
    };
    private static final String[] TOY_IDS={"tennis","yarn","mouse","plush","rope"};

    static void ensurePersonality(SharedPreferences sp,int slot){
        if(sp.getBoolean(PERSONALITY_READY,false))return;
        long born=sp.getLong("born",System.currentTimeMillis());
        long seed=born ^ (0x9E3779B97F4A7C15L*(slot+11L));
        Random rnd=new Random(seed);

        ArrayList<String> foods=new ArrayList<>(Arrays.asList(FOOD_IDS));
        Collections.shuffle(foods,rnd);
        Set<String> foodLikes=new HashSet<>(foods.subList(0,Math.min(3,foods.size())));
        Set<String> foodDislikes=new HashSet<>(foods.subList(
            Math.min(3,foods.size()),Math.min(6,foods.size())));

        ArrayList<String> toys=new ArrayList<>(Arrays.asList(TOY_IDS));
        Collections.shuffle(toys,rnd);
        Set<String> toyLikes=new HashSet<>(toys.subList(0,Math.min(2,toys.size())));
        Set<String> toyDislikes=new HashSet<>(toys.subList(
            Math.min(2,toys.size()),Math.min(4,toys.size())));

        sp.edit()
            .putString(FOOD_LIKES,join(foodLikes))
            .putString(FOOD_DISLIKES,join(foodDislikes))
            .putString(TOY_LIKES,join(toyLikes))
            .putString(TOY_DISLIKES,join(toyDislikes))
            .putBoolean(PERSONALITY_READY,true)
            .apply();
    }

    static Preference foodPreference(SharedPreferences sp,String id){
        if("bottle".equals(id)||"milk".equals(id)||"junior".equals(id))
            return Preference.NEUTRAL;
        if(contains(sp.getString(FOOD_LIKES,""),id))return Preference.LOVE;
        if(contains(sp.getString(FOOD_DISLIKES,""),id))return Preference.DISLIKE;
        return Preference.NEUTRAL;
    }

    static Preference toyPreference(SharedPreferences sp,String id){
        if(contains(sp.getString(TOY_LIKES,""),id))return Preference.LOVE;
        if(contains(sp.getString(TOY_DISLIKES,""),id))return Preference.DISLIKE;
        return Preference.NEUTRAL;
    }

    static float registerRepeat(SharedPreferences sp,String family){
        if(family==null||family.isEmpty()||"sleep".equals(family))return 1f;
        long now=System.currentTimeMillis();
        String last=sp.getString("repeat_family","");
        long lastAt=sp.getLong("repeat_at",0L);
        int count=sp.getInt("repeat_count",0);
        if(!family.equals(last)||lastAt<=0L||now-lastAt>BOREDOM_RESET_MS)count=1;
        else count++;
        sp.edit().putString("repeat_family",family).putInt("repeat_count",count)
            .putLong("repeat_at",now).apply();
        if(count<=1)return 1f;
        if(count==2)return .75f;
        if(count==3)return .40f;
        if(count==4)return .15f;
        return 0f;
    }

    static int repeatCount(SharedPreferences sp){return sp.getInt("repeat_count",0);}

    static void resetPersonality(SharedPreferences sp,int slot){
        sp.edit()
            .remove(PERSONALITY_READY)
            .remove(FOOD_LIKES).remove(FOOD_DISLIKES)
            .remove(TOY_LIKES).remove(TOY_DISLIKES)
            .apply();
        ensurePersonality(sp,slot);
    }

    static void resetRepetition(SharedPreferences sp){
        sp.edit().remove("repeat_family").remove("repeat_count").remove("repeat_at").apply();
    }

    static float positive(float value,float factor){
        return value>0f?value*Math.max(0f,Math.min(1f,factor)):value;
    }

    static int rewardStars(int stars,float factor){
        return factor>=.75f?stars:0;
    }

    static String boredomText(String pet,float factor){
        if(factor<=0f)return pet+" s’en est lassé : cette action n’a plus d’effet.";
        if(factor<.20f)return pet+" est presque lassé : l’effet est très faible.";
        if(factor<.50f)return pet+" commence à se lasser : l’effet diminue.";
        if(factor<1f)return pet+" apprécie encore, mais moins qu’avant.";
        return "";
    }

    private static String join(Set<String> values){
        ArrayList<String> sorted=new ArrayList<>(values);
        Collections.sort(sorted);
        return String.join(",",sorted);
    }

    private static boolean contains(String csv,String id){
        if(csv==null||csv.isEmpty()||id==null||id.isEmpty())return false;
        for(String part:csv.split(","))if(id.equals(part))return true;
        return false;
    }

    private PetBehavior(){}
}
