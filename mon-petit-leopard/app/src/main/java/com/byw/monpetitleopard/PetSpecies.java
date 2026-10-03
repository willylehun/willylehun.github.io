package com.byw.monpetitleopard;

/** Species affects presentation only; all companions share the same life cycle. */
final class PetSpecies {
    static final String LEOPARD="leopard";
    static final String WOLF="wolf";
    static final String TIGER="tiger";

    static String normalize(String species){
        if(species!=null){
            if(WOLF.equalsIgnoreCase(species.trim()))return WOLF;
            if(TIGER.equalsIgnoreCase(species.trim()))return TIGER;
        }
        return LEOPARD;
    }

    static boolean isWolf(String species){return WOLF.equals(normalize(species));}
    static boolean isTiger(String species){return TIGER.equals(normalize(species));}

    static String label(String species){
        if(isTiger(species))return "Tigre";
        return isWolf(species)?"Loup":"Léopard";
    }

    static String pluralLabel(String species){
        if(isTiger(species))return "Tigres";
        return isWolf(species)?"Loups":"Léopards";
    }

    static String emoji(String species){
        if(isTiger(species))return "🐅";
        return isWolf(species)?"🐺":"🐆";
    }

    static String defaultName(String species){
        if(isTiger(species))return "Tigrou";
        return isWolf(species)?"Lou":"Léo";
    }

    static String stageLabel(String species,MainActivity.PetStage stage){
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

    static int iconRes(String species,long age){
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

    static int promenadeTokenRes(String species,long age){
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
