package com.byw.monpetitleopard;

final class GardenSprites {
    static final int FRAME_SIZE=256;

    static final class Pack {
        final String species;
        final MainActivity.PetStage stage;
        final int scratcherPlay;
        final int scratcherFrames;

        Pack(MainActivity.PetStage stage,int scratcherPlay){
            this("leopard",stage,scratcherPlay,2);
        }

        Pack(String species,MainActivity.PetStage stage,int scratcherPlay,int scratcherFrames){
            this.species=species;
            this.stage=stage;
            this.scratcherPlay=scratcherPlay;
            this.scratcherFrames=scratcherFrames;
        }

        int expectedWidth(int res){
            return res==scratcherPlay?FRAME_SIZE*scratcherFrames:-1;
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

    private static final Pack WOLF_CUB=new Pack("wolf",MainActivity.PetStage.CUB,
        R.drawable.wolf_cub_scratcher_play,1);
    private static final Pack WOLF_TEEN=new Pack("wolf",MainActivity.PetStage.TEEN,
        R.drawable.wolf_teen_scratcher_play,1);
    private static final Pack WOLF_ADULT=new Pack("wolf",MainActivity.PetStage.ADULT,
        R.drawable.wolf_adult_scratcher_play,1);
    private static final Pack WOLF_OLD=new Pack("wolf",MainActivity.PetStage.OLD,
        R.drawable.wolf_old_scratcher_play,1);

    static Pack forStage(String species,MainActivity.PetStage stage){
        if("leopard".equals(species))return forStage(stage);
        if(!"wolf".equals(species))
            throw new IllegalArgumentException("Espèce inconnue : "+species);
        switch(stage){
            case CUB:return WOLF_CUB;
            case TEEN:return WOLF_TEEN;
            case ADULT:return WOLF_ADULT;
            case OLD:return WOLF_OLD;
        }
        throw new IllegalStateException("Aucun pack jardin loup pour l'âge "+stage);
    }

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
