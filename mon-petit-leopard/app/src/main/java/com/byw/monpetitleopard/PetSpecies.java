package com.byw.monpetitleopard;

/** Species affects presentation only; all companions share the same life cycle. */
final class PetSpecies {
    static final String LEOPARD="leopard";
    static final String WOLF="wolf";
    static final String TIGER="tiger";
    static final String LION="lion";

    static String normalize(String species){
        if(species!=null){
            if(WOLF.equalsIgnoreCase(species.trim()))return WOLF;
            if(TIGER.equalsIgnoreCase(species.trim()))return TIGER;
            if(LION.equalsIgnoreCase(species.trim()))return LION;
        }
        return LEOPARD;
    }

    static boolean isWolf(String species){return WOLF.equals(normalize(species));}
    static boolean isTiger(String species){return TIGER.equals(normalize(species));}
    static boolean isLion(String species){return LION.equals(normalize(species));}

    /** A lion's sex selects an independent sprite pack; it must never be guessed. */
    static String requireLionSex(String sex){
        if("male".equals(sex)||"female".equals(sex))return sex;
        throw new IllegalArgumentException("Le sexe du lion doit être male ou female : "+sex);
    }

    static String resourcePrefix(String species,String sex,MainActivity.PetStage stage){
        return species+(isLion(species)?"_"+requireLionSex(sex):"")+"_"+
            stage.name().toLowerCase(java.util.Locale.ROOT)+"_";
    }

    static String label(String species){
        if(isLion(species))return "Lion";
        if(isTiger(species))return "Tigre";
        return isWolf(species)?"Loup":"Léopard";
    }

    static String label(String species,String sex){
        if(isLion(species))return "female".equals(requireLionSex(sex))?"Lionne":"Lion";
        return label(species);
    }

    static String pluralLabel(String species){
        if(isLion(species))return "Lions et lionnes";
        if(isTiger(species))return "Tigres";
        return isWolf(species)?"Loups":"Léopards";
    }

    static String emoji(String species){
        if(isLion(species))return "🦁";
        if(isTiger(species))return "🐅";
        return isWolf(species)?"🐺":"🐆";
    }

    static String defaultName(String species){
        if(isLion(species))return "Simba";
        if(isTiger(species))return "Tigrou";
        return isWolf(species)?"Lou":"Léo";
    }

    static String defaultName(String species,String sex){
        if(isLion(species))return "female".equals(requireLionSex(sex))?"Nala":"Simba";
        return defaultName(species);
    }

    static String stageLabel(String species,MainActivity.PetStage stage){
        if(isLion(species)){
            switch(stage){
                case CUB:return "Lionceau";
                case TEEN:return "Lion ado";
                case ADULT:return "Lion adulte";
                default:return "Vieux lion";
            }
        }
        if(isTiger(species)){
            switch(stage){
                case CUB:return "Tigreau";
                case TEEN:return "Tigre ado";
                case ADULT:return "Tigre adulte";
                default:return "Vieux tigre";
            }
        }
        if(isWolf(species)){
            switch(stage){
                case CUB:return "Louveteau";
                case TEEN:return "Loup ado";
                case ADULT:return "Loup adulte";
                default:return "Vieux loup";
            }
        }
        switch(stage){
            case CUB:return "Léopardeau";
            case TEEN:return "Ado";
            case ADULT:return "Adulte";
            default:return "Vieux";
        }
    }

    static String stageLabel(String species,String sex,MainActivity.PetStage stage){
        if(isLion(species)&&"female".equals(requireLionSex(sex))){
            switch(stage){
                case CUB:return "Lionceau femelle";
                case TEEN:return "Lionne ado";
                case ADULT:return "Lionne adulte";
                default:return "Vieille lionne";
            }
        }
        return stageLabel(species,stage);
    }

    static int iconRes(String species,String sex,long age){
        if(isLion(species)){
            if("female".equals(requireLionSex(sex))){
                if(age<MainActivity.CUB)return R.drawable.lion_female_cub_idle_down;
                if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.lion_female_teen_idle_down;
                if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.lion_female_adult_idle_down;
                return R.drawable.lion_female_old_idle_down;
            }
            if(age<MainActivity.CUB)return R.drawable.lion_male_cub_idle_down;
            if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.lion_male_teen_idle_down;
            if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.lion_male_adult_idle_down;
            return R.drawable.lion_male_old_idle_down;
        }
        return iconRes(species,age);
    }

    static int iconRes(String species,long age){
        if(isLion(species))throw new IllegalArgumentException("Le portrait du lion exige son sexe");
        if(isTiger(species)){
            if(age<MainActivity.CUB)return R.drawable.tiger_cub_idle_down;
            if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.tiger_teen_idle_down;
            if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.tiger_adult_idle_down;
            return R.drawable.tiger_old_idle_down;
        }
        if(isWolf(species)){
            if(age<MainActivity.CUB)return R.drawable.wolf_cub_idle_down;
            if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.wolf_teen_idle_down;
            if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.wolf_adult_idle_down;
            return R.drawable.wolf_old_idle_down;
        }
        if(age<MainActivity.CUB)return R.drawable.leopard_cub_idle_down;
        if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.leopard_teen_idle_down;
        if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.leopard_adult_idle_down;
        return R.drawable.leopard_old_idle_down;
    }

    static int promenadeTokenRes(String species,String sex,long age){
        if(isLion(species)){
            if("female".equals(requireLionSex(sex))){
                if(age<MainActivity.CUB)return R.drawable.lion_female_cub_promenade_token;
                if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.lion_female_teen_promenade_token;
                if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.lion_female_adult_promenade_token;
                return R.drawable.lion_female_old_promenade_token;
            }
            if(age<MainActivity.CUB)return R.drawable.lion_male_cub_promenade_token;
            if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.lion_male_teen_promenade_token;
            if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.lion_male_adult_promenade_token;
            return R.drawable.lion_male_old_promenade_token;
        }
        return promenadeTokenRes(species,age);
    }

    static int promenadeTokenRes(String species,long age){
        if(isLion(species))throw new IllegalArgumentException("Le portrait de promenade du lion exige son sexe");
        if(isTiger(species)){
            if(age<MainActivity.CUB)return R.drawable.tiger_cub_promenade_token;
            if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.tiger_teen_promenade_token;
            if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.tiger_adult_promenade_token;
            return R.drawable.tiger_old_promenade_token;
        }
        if(!isWolf(species))return R.drawable.promenade_token;
        if(age<MainActivity.CUB)return R.drawable.wolf_cub_promenade_token;
        if(age<MainActivity.CUB+MainActivity.TEEN)return R.drawable.wolf_teen_promenade_token;
        if(age<MainActivity.CUB+MainActivity.TEEN+MainActivity.ADULT)return R.drawable.wolf_adult_promenade_token;
        return R.drawable.wolf_old_promenade_token;
    }

    private PetSpecies(){}
}
