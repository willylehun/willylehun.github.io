package com.byw.monpetitleopard;

final class CareSprites {
    static final int FRAME_SIZE=256;

    static final class Pack {
        final String species;
        final MainActivity.PetStage stage;
        final int groomFoam,soap,comb,towel;

        Pack(MainActivity.PetStage stage,int groomFoam,int soap,int comb,int towel){
            this("leopard",stage,groomFoam,soap,comb,towel);
        }

        Pack(String species,MainActivity.PetStage stage,int groomFoam,int soap,int comb,int towel){
            this.species=species;
            this.stage=stage;
            this.groomFoam=groomFoam;
            this.soap=soap;
            this.comb=comb;
            this.towel=towel;
        }

        int action(String id){
            if("groom_foam".equals(id))return groomFoam;
            if("soap".equals(id))return soap;
            if("comb".equals(id))return comb;
            if("towel".equals(id))return towel;
            return 0;
        }

        int expectedWidth(int res){
            return res==groomFoam||res==soap||res==comb||res==towel?FRAME_SIZE:-1;
        }

        int[] allResources(){
            return new int[]{groomFoam,soap,comb,towel};
        }
    }

    private static final Pack CUB=new Pack(MainActivity.PetStage.CUB,
        R.drawable.leopard_cub_groom_foam,R.drawable.leopard_cub_soap,
        R.drawable.leopard_cub_comb,R.drawable.leopard_cub_towel);

    private static final Pack TEEN=new Pack(MainActivity.PetStage.TEEN,
        R.drawable.leopard_teen_groom_foam,R.drawable.leopard_teen_soap,
        R.drawable.leopard_teen_comb,R.drawable.leopard_teen_towel);

    private static final Pack ADULT=new Pack(MainActivity.PetStage.ADULT,
        R.drawable.leopard_adult_groom_foam,R.drawable.leopard_adult_soap,
        R.drawable.leopard_adult_comb,R.drawable.leopard_adult_towel);

    private static final Pack OLD=new Pack(MainActivity.PetStage.OLD,
        R.drawable.leopard_old_groom_foam,R.drawable.leopard_old_soap,
        R.drawable.leopard_old_comb,R.drawable.leopard_old_towel);

    private static final Pack WOLF_CUB=new Pack("wolf",MainActivity.PetStage.CUB,
        R.drawable.wolf_cub_groom_foam,R.drawable.wolf_cub_soap,
        R.drawable.wolf_cub_comb,R.drawable.wolf_cub_towel);

    private static final Pack WOLF_TEEN=new Pack("wolf",MainActivity.PetStage.TEEN,
        R.drawable.wolf_teen_groom_foam,R.drawable.wolf_teen_soap,
        R.drawable.wolf_teen_comb,R.drawable.wolf_teen_towel);

    private static final Pack WOLF_ADULT=new Pack("wolf",MainActivity.PetStage.ADULT,
        R.drawable.wolf_adult_groom_foam,R.drawable.wolf_adult_soap,
        R.drawable.wolf_adult_comb,R.drawable.wolf_adult_towel);

    private static final Pack WOLF_OLD=new Pack("wolf",MainActivity.PetStage.OLD,
        R.drawable.wolf_old_groom_foam,R.drawable.wolf_old_soap,
        R.drawable.wolf_old_comb,R.drawable.wolf_old_towel);

    private static final Pack TIGER_CUB=new Pack("tiger",MainActivity.PetStage.CUB,
        R.drawable.tiger_cub_groom_foam,R.drawable.tiger_cub_soap,
        R.drawable.tiger_cub_comb,R.drawable.tiger_cub_towel);

    private static final Pack TIGER_TEEN=new Pack("tiger",MainActivity.PetStage.TEEN,
        R.drawable.tiger_teen_groom_foam,R.drawable.tiger_teen_soap,
        R.drawable.tiger_teen_comb,R.drawable.tiger_teen_towel);

    private static final Pack TIGER_ADULT=new Pack("tiger",MainActivity.PetStage.ADULT,
        R.drawable.tiger_adult_groom_foam,R.drawable.tiger_adult_soap,
        R.drawable.tiger_adult_comb,R.drawable.tiger_adult_towel);

    private static final Pack TIGER_OLD=new Pack("tiger",MainActivity.PetStage.OLD,
        R.drawable.tiger_old_groom_foam,R.drawable.tiger_old_soap,
        R.drawable.tiger_old_comb,R.drawable.tiger_old_towel);

    static Pack forStage(String species,MainActivity.PetStage stage){
        if("leopard".equals(species))return forStage(stage);
        if("tiger".equals(species)){
            switch(stage){
                case CUB:return TIGER_CUB;
                case TEEN:return TIGER_TEEN;
                case ADULT:return TIGER_ADULT;
                case OLD:return TIGER_OLD;
            }
            throw new IllegalStateException("Aucun pack de soins tigre pour l'âge "+stage);
        }
        if(!"wolf".equals(species))
            throw new IllegalArgumentException("Espèce inconnue : "+species);
        switch(stage){
            case CUB:return WOLF_CUB;
            case TEEN:return WOLF_TEEN;
            case ADULT:return WOLF_ADULT;
            case OLD:return WOLF_OLD;
        }
        throw new IllegalStateException("Aucun pack de soins loup pour l'âge "+stage);
    }

    static Pack forStage(MainActivity.PetStage stage){
        switch(stage){
            case CUB:return CUB;
            case TEEN:return TEEN;
            case ADULT:return ADULT;
            case OLD:return OLD;
        }
        throw new IllegalStateException("Aucun pack de soins pour l'âge "+stage);
    }

    static int bottle(MainActivity.PetStage stage){
        return stage==MainActivity.PetStage.CUB?R.drawable.leopard_cub_bottle:0;
    }

    static int bottle(String species,MainActivity.PetStage stage){
        if("leopard".equals(species))return bottle(stage);
        if("tiger".equals(species))return stage==MainActivity.PetStage.CUB?R.drawable.tiger_cub_bottle:0;
        if(!"wolf".equals(species))
            throw new IllegalArgumentException("Espèce inconnue : "+species);
        return stage==MainActivity.PetStage.CUB?R.drawable.wolf_cub_bottle:0;
    }

    private CareSprites(){}
}
