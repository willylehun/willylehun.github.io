package com.byw.monpetitleopard;

final class CareSprites {
    static final int FRAME_SIZE=256;

    static final class Pack {
        final MainActivity.PetStage stage;
        final int groomFoam,soap,comb,towel;

        Pack(MainActivity.PetStage stage,int groomFoam,int soap,int comb,int towel){
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

    private CareSprites(){}
}
