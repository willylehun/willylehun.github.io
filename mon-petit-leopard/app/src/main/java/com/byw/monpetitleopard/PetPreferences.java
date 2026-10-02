package com.byw.monpetitleopard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Goûts stables et propres à chaque profil animal. */
final class PetPreferences {
    static final int DISLIKE=-1,NEUTRAL=0,LIKE=1;

    private static final String[] FOODS={
        "kibble","wet","chicken","fish","steak","biscuit","treats",
        "apple","banana","watermelon","carrot","berries"
    };
    private static final String[] TOYS={"tennis","yarn","mouse","plush","rope","scratch"};

    static int food(MainActivity a,String id){
        if(a.stage()==MainActivity.Stage.CUB &&
            ("milk".equals(id)||"bottle".equals(id)||"junior".equals(id)))return NEUTRAL;
        if("milk".equals(id)||"bottle".equals(id)||"junior".equals(id))return NEUTRAL;
        return preference(a.profileSlot,a.pet,id,FOODS,0x51A7);
    }

    static int toy(MainActivity a,String id){
        return preference(a.profileSlot,a.pet,id,TOYS,0x2B19);
    }

    private static int preference(int slot,String name,String id,String[] pool,int salt){
        Set<String> known=new HashSet<>(Arrays.asList(pool));
        if(!known.contains(id))return NEUTRAL;
        int seed=(name==null?0:name.hashCode())*31+slot*997+salt;
        int like=Math.floorMod(seed,pool.length);
        int dislike=Math.floorMod(seed*17+5,pool.length);
        if(dislike==like)dislike=(dislike+1)%pool.length;
        if(id.equals(pool[like]))return LIKE;
        if(id.equals(pool[dislike]))return DISLIKE;
        return NEUTRAL;
    }

    static float hungerMultiplier(int pref){
        return pref==LIKE?1.30f:pref==DISLIKE?.55f:1f;
    }

    static float happyDelta(int pref,float base){
        if(pref==LIKE)return Math.max(base,0f)*1.55f+4f;
        if(pref==DISLIKE)return -Math.max(6f,Math.abs(base)*.75f);
        return base;
    }

    static String label(int pref){
        if(pref==LIKE)return "❤️ adore";
        if(pref==DISLIKE)return "😠 déteste";
        return "• neutre";
    }

    private PetPreferences(){}
}
