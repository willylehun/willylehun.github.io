package com.byw.monpetitleopard;

import android.content.SharedPreferences;

/** Temporary developer control: advance only the selected animal's age. */
final class DevGrowth {
    // Set to false to remove the button and disable manual growth together.
    static final boolean ENABLED=true;

    static MainActivity.PetStage nextStage(long born,long now){
        if(!ENABLED)return null;
        long age=Math.max(0L,now-born);
        if(age<MainActivity.CUB)return MainActivity.PetStage.TEEN;
        if(age<MainActivity.CUB+MainActivity.TEEN)return MainActivity.PetStage.ADULT;
        if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return MainActivity.PetStage.OLD;
        // Old and completed life cycles never advance to death through this control.
        return null;
    }

    static long advance(SharedPreferences profile,long now){
        long born=profile.getLong("born",now);
        MainActivity.PetStage next=nextStage(born,now);
        if(next==null)return born;

        long age=MainActivity.CUB;
        if(next==MainActivity.PetStage.ADULT)age+=MainActivity.TEEN;
        else if(next==MainActivity.PetStage.OLD)age+=MainActivity.TEEN+MainActivity.ADULT;
        long advancedBorn=now-age;
        // Keep needs, sleep, promenade timers, personality and every other profile
        // unchanged. Normal elapsed time still drives those systems independently.
        profile.edit().putLong("born",advancedBorn).apply();
        return advancedBorn;
    }

    private DevGrowth(){}
}
