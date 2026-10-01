package com.byw.monpetitleopard;

final class GardenSprites {
    static final int FRAME_SIZE=256;

    static final class Pack {
        final MainActivity.PetStage stage;
        final int scratcherPlay;

        Pack(MainActivity.PetStage stage,int scratcherPlay){
            this.stage=stage;
            this.scratcherPlay=scratcherPlay;
        }

        int expectedWidth(int res){
            return res==scratcherPlay?FRAME_SIZE*2:-1;
        }

        int[] allResources(){return new int[]{scratcherPlay};}
    }

    private static final Pack CUB=new Pack(MainActivity.PetStage.CUB,
        R.drawable.leopard_cub_scratcher_play);
    private static final Pack TEEN=new Pack(MainActivity.PetStage.TEEN,
        R.drawable.leopard_teen_scratcher_play);
    private static final Pack ADULT=new Pack(MainActivity.PetStage.ADULT,
        R.drawable.leopard_adult_scratcher_play);
    private static final Pack OLD=new Pack(MainActivity.PetStage.OLD,
        R.drawable.leopard_old_scratcher_play);

    static Pack forStage(MainActivity.PetStage stage){
        switch(stage){
            case CUB:return CUB;
            case TEEN:return TEEN;
            case ADULT:return ADULT;
            case OLD:return OLD;
        }
        throw new IllegalStateException("Aucun pack jardin pour l'âge "+stage);
    }

    private GardenSprites(){}
}
