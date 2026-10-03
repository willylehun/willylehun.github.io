package com.byw.monpetitleopard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;
import java.util.Set;
import java.util.Random;

final class PetProfileStore {
    static final int MAX_PROFILES=6;
    static final String EXTRA_SLOT="profile_slot";
    private static final String META_PREFS="pet_profiles_v079";
    private static final String MIGRATED="migration_v079_done";

    static void ensureMigrated(Context context){
        SharedPreferences meta=context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE);
        if(meta.getBoolean(MIGRATED,false))return;

        SharedPreferences legacy=context.getSharedPreferences("pet",Context.MODE_PRIVATE);
        if(!legacy.getAll().isEmpty()){
            SharedPreferences target=context.getSharedPreferences(petPrefsName(0),Context.MODE_PRIVATE);
            if(target.getAll().isEmpty())copyAll(legacy,target);
            String name=legacy.getString("name","Léo");
            if(name==null||name.trim().isEmpty())name="Léo";
            target.edit().putString("name",name).putBoolean("named",true).apply();
            PetBehavior.ensurePersonality(target,0);
            meta.edit()
                .putBoolean(existsKey(0),true)
                .putString(nameKey(0),name)
                .putString(sexKey(0),"")
                .putString(typeKey(0),"leopard")
                .putLong(createdKey(0),target.getLong("born",System.currentTimeMillis()))
                .apply();
        }
        meta.edit().putBoolean(MIGRATED,true).apply();
    }

    private static void copyAll(SharedPreferences from,SharedPreferences to){
        SharedPreferences.Editor e=to.edit();
        for(Map.Entry<String,?> entry:from.getAll().entrySet()){
            String k=entry.getKey();
            Object v=entry.getValue();
            if(v instanceof String)e.putString(k,(String)v);
            else if(v instanceof Integer)e.putInt(k,(Integer)v);
            else if(v instanceof Long)e.putLong(k,(Long)v);
            else if(v instanceof Float)e.putFloat(k,(Float)v);
            else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
            else if(v instanceof Set){
                @SuppressWarnings("unchecked")
                Set<String> set=(Set<String>)v;
                e.putStringSet(k,set);
            }
        }
        e.apply();
    }

    static String petPrefsName(int slot){return "pet_"+slot;}

    static boolean validSlot(int slot){return slot>=0&&slot<MAX_PROFILES;}

    static boolean exists(Context context,int slot){
        if(!validSlot(slot))return false;
        return context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE)
            .getBoolean(existsKey(slot),false);
    }

    static int count(Context context){
        int n=0;
        for(int i=0;i<MAX_PROFILES;i++)if(exists(context,i))n++;
        return n;
    }

    static int firstEmpty(Context context){
        for(int i=0;i<MAX_PROFILES;i++)if(!exists(context,i))return i;
        return -1;
    }

    static String name(Context context,int slot){
        String name=context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE)
            .getString(nameKey(slot),PetSpecies.defaultName(species(context,slot)));
        return name==null||name.trim().isEmpty()?PetSpecies.defaultName(species(context,slot)):name;
    }

    static String sex(Context context,int slot){
        return context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE)
            .getString(sexKey(slot),"");
    }

    static String species(Context context,int slot){
        return PetSpecies.normalize(context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE)
            .getString(typeKey(slot),PetSpecies.LEOPARD));
    }

    static String speciesLabel(Context context,int slot){
        return PetSpecies.label(species(context,slot));
    }

    static boolean reproductionAgeEligible(Context context,int slot){
        if(!exists(context,slot))return false;
        SharedPreferences pet=context.getSharedPreferences(petPrefsName(slot),Context.MODE_PRIVATE);
        long born=pet.getLong("born",System.currentTimeMillis());
        long age=Math.max(0L,System.currentTimeMillis()-born);
        return age>=MainActivity.CUB && age<MainActivity.LIFE;
    }

    static boolean compatibleParents(Context context,int a,int b){
        if(a==b||!exists(context,a)||!exists(context,b))return false;
        if(!reproductionAgeEligible(context,a)||!reproductionAgeEligible(context,b))return false;
        String sa=sex(context,a),sb=sex(context,b);
        if(sa==null||sb==null||sa.isEmpty()||sb.isEmpty()||sa.equals(sb))return false;
        if(!(("male".equals(sa)&&"female".equals(sb))||
                ("female".equals(sa)&&"male".equals(sb))))return false;
        return species(context,a).equals(species(context,b));
    }

    static void setSex(Context context,int slot,String sex){
        if(!validSlot(slot)||!exists(context,slot))return;
        String cleanSex=("female".equals(sex)||"male".equals(sex))?sex:"";
        context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE).edit()
            .putString(sexKey(slot),cleanSex).apply();
        context.getSharedPreferences(petPrefsName(slot),Context.MODE_PRIVATE).edit()
            .putString("sex",cleanSex).apply();
    }

    static void updateName(Context context,int slot,String name){
        if(!validSlot(slot)||!exists(context,slot))return;
        String clean=(name==null||name.trim().isEmpty())?PetSpecies.defaultName(species(context,slot)):name.trim();
        context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE).edit()
            .putString(nameKey(slot),clean).apply();
        context.getSharedPreferences(petPrefsName(slot),Context.MODE_PRIVATE).edit()
            .putString("name",clean).putBoolean("named",true).apply();
    }

    static void createLeopard(Context context,int slot,String sex,String name){
        createAnimal(context,slot,PetSpecies.LEOPARD,sex,name);
    }

    static void createAnimal(Context context,int slot,String species,String sex,String name){
        createProfile(context,slot,PetSpecies.normalize(species),sex,name,-1,-1);
    }

    static String createOffspring(Context context,int slot,int parentA,int parentB,String name){
        if(!compatibleParents(context,parentA,parentB))
            throw new IllegalArgumentException("parents incompatibles");
        String type=species(context,parentA);
        String sex=new Random(System.nanoTime()+slot*37L).nextBoolean()?"male":"female";
        createProfile(context,slot,type,sex,name,parentA,parentB);
        return sex;
    }

    private static synchronized void createProfile(Context context,int slot,String type,String sex,String name,int parentA,int parentB){
        if(!validSlot(slot))throw new IllegalArgumentException("slot invalide");
        if(exists(context,slot))throw new IllegalStateException("emplacement déjà occupé");
        if((parentA>=0||parentB>=0)&&!compatibleParents(context,parentA,parentB))
            throw new IllegalArgumentException("parents incompatibles");
        long now=System.currentTimeMillis();
        String cleanType=PetSpecies.normalize(type);
        String clean=(name==null||name.trim().isEmpty())?PetSpecies.defaultName(cleanType):name.trim();
        String cleanSex=("female".equals(sex)||"male".equals(sex))?sex:"";
        SharedPreferences pet=context.getSharedPreferences(petPrefsName(slot),Context.MODE_PRIVATE);
        SharedPreferences.Editor pe=pet.edit().clear()
            .putLong("born",now).putLong("last",now)
            .putString("name",clean).putString("sex",cleanSex)
            .putBoolean("named",true).putInt("generation",1);
        if(parentA>=0)pe.putInt("parentA",parentA);
        if(parentB>=0)pe.putInt("parentB",parentB);
        pe.apply();
        PetBehavior.ensurePersonality(pet,slot);

        context.getSharedPreferences(META_PREFS,Context.MODE_PRIVATE).edit()
            .putBoolean(existsKey(slot),true)
            .putString(nameKey(slot),clean)
            .putString(sexKey(slot),cleanSex)
            .putString(typeKey(slot),cleanType)
            .putLong(createdKey(slot),now)
            .apply();
    }

    static int iconRes(Context context,int slot){
        if(!exists(context,slot))return PetSpecies.iconRes(PetSpecies.LEOPARD,0L);
        SharedPreferences p=context.getSharedPreferences(petPrefsName(slot),Context.MODE_PRIVATE);
        long born=p.getLong("born",System.currentTimeMillis());
        long age=Math.max(0L,System.currentTimeMillis()-born);
        return PetSpecies.iconRes(species(context,slot),age);
    }

    static String sexLabel(String sex){
        if("female".equals(sex))return "♀ Femelle";
        if("male".equals(sex))return "♂ Mâle";
        return "Sexe à choisir";
    }

    private static String existsKey(int i){return "slot_"+i+"_exists";}
    private static String nameKey(int i){return "slot_"+i+"_name";}
    private static String sexKey(int i){return "slot_"+i+"_sex";}
    private static String typeKey(int i){return "slot_"+i+"_type";}
    private static String createdKey(int i){return "slot_"+i+"_created";}

    private PetProfileStore(){}
}
