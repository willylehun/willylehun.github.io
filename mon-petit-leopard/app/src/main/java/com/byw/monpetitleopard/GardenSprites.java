package com.byw.monpetitleopard;

final class GardenSprites {
    static final int FRAME_SIZE=256;

    static final class Pack {
        final String species;
        final String sex;
        final MainActivity.PetStage stage;
        final int scratcherPlay;
        final int scratcherFrames;

        Pack(MainActivity.PetStage stage,int scratcherPlay){
            this("leopard",stage,scratcherPlay,2);
        }

        Pack(String species,MainActivity.PetStage stage,int scratcherPlay,int scratcherFrames){
            this(species,"",stage,scratcherPlay,scratcherFrames);
        }

        Pack(String species,String sex,MainActivity.PetStage stage,int scratcherPlay,int scratcherFrames){
            this.species=species;
            this.sex=sex;
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

    private static final Pack TIGER_CUB=new Pack("tiger",MainActivity.PetStage.CUB,
        R.drawable.tiger_cub_scratcher_play,1);
    private static final Pack TIGER_TEEN=new Pack("tiger",MainActivity.PetStage.TEEN,
        R.drawable.tiger_teen_scratcher_play,1);
    private static final Pack TIGER_ADULT=new Pack("tiger",MainActivity.PetStage.ADULT,
        R.drawable.tiger_adult_scratcher_play,1);
    private static final Pack TIGER_OLD=new Pack("tiger",MainActivity.PetStage.OLD,
        R.drawable.tiger_old_scratcher_play,1);

    private static final Pack FOX_CUB=new Pack("fox",MainActivity.PetStage.CUB,
        R.drawable.fox_cub_scratcher_play,1);
    private static final Pack FOX_TEEN=new Pack("fox",MainActivity.PetStage.TEEN,
        R.drawable.fox_teen_scratcher_play,1);
    private static final Pack FOX_ADULT=new Pack("fox",MainActivity.PetStage.ADULT,
        R.drawable.fox_adult_scratcher_play,1);
    private static final Pack FOX_OLD=new Pack("fox",MainActivity.PetStage.OLD,
        R.drawable.fox_old_scratcher_play,1);

    private static final Pack BEAR_CUB=new Pack("bear",MainActivity.PetStage.CUB,
        R.drawable.bear_cub_scratcher_play,1);
    private static final Pack BEAR_TEEN=new Pack("bear",MainActivity.PetStage.TEEN,
        R.drawable.bear_teen_scratcher_play,1);
    private static final Pack BEAR_ADULT=new Pack("bear",MainActivity.PetStage.ADULT,
        R.drawable.bear_adult_scratcher_play,1);
    private static final Pack BEAR_OLD=new Pack("bear",MainActivity.PetStage.OLD,
        R.drawable.bear_old_scratcher_play,1);

    private static final Pack LION_MALE_CUB=new Pack("lion","male",MainActivity.PetStage.CUB,
        R.drawable.lion_male_cub_scratcher_play,1);
    private static final Pack LION_MALE_TEEN=new Pack("lion","male",MainActivity.PetStage.TEEN,
        R.drawable.lion_male_teen_scratcher_play,1);
    private static final Pack LION_MALE_ADULT=new Pack("lion","male",MainActivity.PetStage.ADULT,
        R.drawable.lion_male_adult_scratcher_play,1);
    private static final Pack LION_MALE_OLD=new Pack("lion","male",MainActivity.PetStage.OLD,
        R.drawable.lion_male_old_scratcher_play,1);

    private static final Pack LION_FEMALE_CUB=new Pack("lion","female",MainActivity.PetStage.CUB,
        R.drawable.lion_female_cub_scratcher_play,1);
    private static final Pack LION_FEMALE_TEEN=new Pack("lion","female",MainActivity.PetStage.TEEN,
        R.drawable.lion_female_teen_scratcher_play,1);
    private static final Pack LION_FEMALE_ADULT=new Pack("lion","female",MainActivity.PetStage.ADULT,
        R.drawable.lion_female_adult_scratcher_play,1);
    private static final Pack LION_FEMALE_OLD=new Pack("lion","female",MainActivity.PetStage.OLD,
        R.drawable.lion_female_old_scratcher_play,1);

    static Pack forStage(String species,String sex,MainActivity.PetStage stage){
        if(!"lion".equals(species))return forStage(species,stage);
        boolean female="female".equals(PetSpecies.requireLionSex(sex));
        switch(stage){
            case CUB:return female?LION_FEMALE_CUB:LION_MALE_CUB;
            case TEEN:return female?LION_FEMALE_TEEN:LION_MALE_TEEN;
            case ADULT:return female?LION_FEMALE_ADULT:LION_MALE_ADULT;
            case OLD:return female?LION_FEMALE_OLD:LION_MALE_OLD;
        }
        throw new IllegalStateException("Aucun pack lion pour l'âge "+stage);
    }

    static Pack forStage(String species,MainActivity.PetStage stage){
        if("lion".equals(species))throw new IllegalArgumentException("Le pack du lion exige son sexe");
        if("leopard".equals(species))return forStage(stage);
        if("tiger".equals(species)){
            switch(stage){
                case CUB:return TIGER_CUB;
                case TEEN:return TIGER_TEEN;
                case ADULT:return TIGER_ADULT;
                case OLD:return TIGER_OLD;
            }
            throw new IllegalStateException("Aucun pack jardin tigre pour l'âge "+stage);
        }
        if("fox".equals(species)){
            switch(stage){
                case CUB:return FOX_CUB;
                case TEEN:return FOX_TEEN;
                case ADULT:return FOX_ADULT;
                case OLD:return FOX_OLD;
            }
            throw new IllegalStateException("Aucun pack jardin renard pour l'âge "+stage);
        }
        if("bear".equals(species)){
            switch(stage){
                case CUB:return BEAR_CUB;
                case TEEN:return BEAR_TEEN;
                case ADULT:return BEAR_ADULT;
                case OLD:return BEAR_OLD;
            }
            throw new IllegalStateException("Aucun pack jardin ours pour l'âge "+stage);
        }
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
