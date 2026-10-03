package com.byw.monpetitleopard;

final class GameSprites {
    static final int FRAME_SIZE=256;

    static final class Pack {
        final String species;
        final String sex;
        final MainActivity.PetStage stage;
        final int runDown,runLeft,runRight,runUp;
        final int fetchBall,fetchTennis,fetchYarn,fetchMouse,fetchPlush,ropePlay;
        final int ropeFrames;

        Pack(MainActivity.PetStage stage,
             int runDown,int runLeft,int runRight,int runUp,
             int fetchBall,int fetchTennis,int fetchYarn,int fetchMouse,int fetchPlush,
             int ropePlay){
            this("leopard",stage,runDown,runLeft,runRight,runUp,
                fetchBall,fetchTennis,fetchYarn,fetchMouse,fetchPlush,ropePlay,5);
        }

        Pack(String species,MainActivity.PetStage stage,
             int runDown,int runLeft,int runRight,int runUp,
             int fetchBall,int fetchTennis,int fetchYarn,int fetchMouse,int fetchPlush,
             int ropePlay,int ropeFrames){
            this(species,"",stage,runDown,runLeft,runRight,runUp,fetchBall,fetchTennis,fetchYarn,fetchMouse,fetchPlush,ropePlay,ropeFrames);
        }

        Pack(String species,String sex,MainActivity.PetStage stage,
             int runDown,int runLeft,int runRight,int runUp,
             int fetchBall,int fetchTennis,int fetchYarn,int fetchMouse,int fetchPlush,
             int ropePlay,int ropeFrames){
            this.species=species;
            this.sex=sex;
            this.stage=stage;
            this.runDown=runDown;this.runLeft=runLeft;this.runRight=runRight;this.runUp=runUp;
            this.fetchBall=fetchBall;this.fetchTennis=fetchTennis;this.fetchYarn=fetchYarn;
            this.fetchMouse=fetchMouse;this.fetchPlush=fetchPlush;this.ropePlay=ropePlay;
            this.ropeFrames=ropeFrames;
        }

        int run(MainActivity.TravelDirection direction){
            switch(direction){
                case LEFT:return runLeft;
                case RIGHT:return runRight;
                case UP:return runUp;
                case DOWN:
                default:return runDown;
            }
        }

        int fetch(String id){
            if("ball".equals(id))return fetchBall;
            if("tennis".equals(id))return fetchTennis;
            if("yarn".equals(id))return fetchYarn;
            if("mouse".equals(id))return fetchMouse;
            if("plush".equals(id))return fetchPlush;
            return fetchBall;
        }

        boolean ownsRun(int res){
            return res==runDown||res==runLeft||res==runRight||res==runUp;
        }

        boolean ownsPose(int res){
            return res==fetchBall||res==fetchTennis||res==fetchYarn||
                   res==fetchMouse||res==fetchPlush||res==ropePlay;
        }

        int expectedWidth(int res){
            if(ownsRun(res))return FRAME_SIZE*6;
            if(res==ropePlay)return FRAME_SIZE*ropeFrames;
            if(ownsPose(res))return FRAME_SIZE;
            return -1;
        }

        int[] allResources(){
            return new int[]{runDown,runLeft,runRight,runUp,
                fetchBall,fetchTennis,fetchYarn,fetchMouse,fetchPlush,ropePlay};
        }
    }

    private static final Pack CUB=new Pack(MainActivity.PetStage.CUB,
        R.drawable.leopard_cub_run_down,R.drawable.leopard_cub_run_left,
        R.drawable.leopard_cub_run_right,R.drawable.leopard_cub_run_up,
        R.drawable.leopard_cub_fetch_ball,R.drawable.leopard_cub_fetch_tennis,
        R.drawable.leopard_cub_fetch_yarn,R.drawable.leopard_cub_fetch_mouse,
        R.drawable.leopard_cub_fetch_plush,R.drawable.leopard_cub_rope_play);

    private static final Pack TEEN=new Pack(MainActivity.PetStage.TEEN,
        R.drawable.leopard_teen_run_down,R.drawable.leopard_teen_run_left,
        R.drawable.leopard_teen_run_right,R.drawable.leopard_teen_run_up,
        R.drawable.leopard_teen_fetch_ball,R.drawable.leopard_teen_fetch_tennis,
        R.drawable.leopard_teen_fetch_yarn,R.drawable.leopard_teen_fetch_mouse,
        R.drawable.leopard_teen_fetch_plush,R.drawable.leopard_teen_rope_play);

    private static final Pack ADULT=new Pack(MainActivity.PetStage.ADULT,
        R.drawable.leopard_adult_run_down,R.drawable.leopard_adult_run_left,
        R.drawable.leopard_adult_run_right,R.drawable.leopard_adult_run_up,
        R.drawable.leopard_adult_fetch_ball,R.drawable.leopard_adult_fetch_tennis,
        R.drawable.leopard_adult_fetch_yarn,R.drawable.leopard_adult_fetch_mouse,
        R.drawable.leopard_adult_fetch_plush,R.drawable.leopard_adult_rope_play);

    private static final Pack OLD=new Pack(MainActivity.PetStage.OLD,
        R.drawable.leopard_old_run_down,R.drawable.leopard_old_run_left,
        R.drawable.leopard_old_run_right,R.drawable.leopard_old_run_up,
        R.drawable.leopard_old_fetch_ball,R.drawable.leopard_old_fetch_tennis,
        R.drawable.leopard_old_fetch_yarn,R.drawable.leopard_old_fetch_mouse,
        R.drawable.leopard_old_fetch_plush,R.drawable.leopard_old_rope_play);

    private static final Pack WOLF_CUB=new Pack("wolf",MainActivity.PetStage.CUB,
        R.drawable.wolf_cub_run_down,R.drawable.wolf_cub_run_left,
        R.drawable.wolf_cub_run_right,R.drawable.wolf_cub_run_up,
        R.drawable.wolf_cub_fetch_ball,R.drawable.wolf_cub_fetch_tennis,
        R.drawable.wolf_cub_fetch_yarn,R.drawable.wolf_cub_fetch_mouse,
        R.drawable.wolf_cub_fetch_plush,R.drawable.wolf_cub_rope_play,1);

    private static final Pack WOLF_TEEN=new Pack("wolf",MainActivity.PetStage.TEEN,
        R.drawable.wolf_teen_run_down,R.drawable.wolf_teen_run_left,
        R.drawable.wolf_teen_run_right,R.drawable.wolf_teen_run_up,
        R.drawable.wolf_teen_fetch_ball,R.drawable.wolf_teen_fetch_tennis,
        R.drawable.wolf_teen_fetch_yarn,R.drawable.wolf_teen_fetch_mouse,
        R.drawable.wolf_teen_fetch_plush,R.drawable.wolf_teen_rope_play,1);

    private static final Pack WOLF_ADULT=new Pack("wolf",MainActivity.PetStage.ADULT,
        R.drawable.wolf_adult_run_down,R.drawable.wolf_adult_run_left,
        R.drawable.wolf_adult_run_right,R.drawable.wolf_adult_run_up,
        R.drawable.wolf_adult_fetch_ball,R.drawable.wolf_adult_fetch_tennis,
        R.drawable.wolf_adult_fetch_yarn,R.drawable.wolf_adult_fetch_mouse,
        R.drawable.wolf_adult_fetch_plush,R.drawable.wolf_adult_rope_play,1);

    private static final Pack WOLF_OLD=new Pack("wolf",MainActivity.PetStage.OLD,
        R.drawable.wolf_old_run_down,R.drawable.wolf_old_run_left,
        R.drawable.wolf_old_run_right,R.drawable.wolf_old_run_up,
        R.drawable.wolf_old_fetch_ball,R.drawable.wolf_old_fetch_tennis,
        R.drawable.wolf_old_fetch_yarn,R.drawable.wolf_old_fetch_mouse,
        R.drawable.wolf_old_fetch_plush,R.drawable.wolf_old_rope_play,1);

    private static final Pack TIGER_CUB=new Pack("tiger",MainActivity.PetStage.CUB,
        R.drawable.tiger_cub_run_down,R.drawable.tiger_cub_run_left,
        R.drawable.tiger_cub_run_right,R.drawable.tiger_cub_run_up,
        R.drawable.tiger_cub_fetch_ball,R.drawable.tiger_cub_fetch_tennis,
        R.drawable.tiger_cub_fetch_yarn,R.drawable.tiger_cub_fetch_mouse,
        R.drawable.tiger_cub_fetch_plush,R.drawable.tiger_cub_rope_play,1);

    private static final Pack TIGER_TEEN=new Pack("tiger",MainActivity.PetStage.TEEN,
        R.drawable.tiger_teen_run_down,R.drawable.tiger_teen_run_left,
        R.drawable.tiger_teen_run_right,R.drawable.tiger_teen_run_up,
        R.drawable.tiger_teen_fetch_ball,R.drawable.tiger_teen_fetch_tennis,
        R.drawable.tiger_teen_fetch_yarn,R.drawable.tiger_teen_fetch_mouse,
        R.drawable.tiger_teen_fetch_plush,R.drawable.tiger_teen_rope_play,1);

    private static final Pack TIGER_ADULT=new Pack("tiger",MainActivity.PetStage.ADULT,
        R.drawable.tiger_adult_run_down,R.drawable.tiger_adult_run_left,
        R.drawable.tiger_adult_run_right,R.drawable.tiger_adult_run_up,
        R.drawable.tiger_adult_fetch_ball,R.drawable.tiger_adult_fetch_tennis,
        R.drawable.tiger_adult_fetch_yarn,R.drawable.tiger_adult_fetch_mouse,
        R.drawable.tiger_adult_fetch_plush,R.drawable.tiger_adult_rope_play,1);

    private static final Pack TIGER_OLD=new Pack("tiger",MainActivity.PetStage.OLD,
        R.drawable.tiger_old_run_down,R.drawable.tiger_old_run_left,
        R.drawable.tiger_old_run_right,R.drawable.tiger_old_run_up,
        R.drawable.tiger_old_fetch_ball,R.drawable.tiger_old_fetch_tennis,
        R.drawable.tiger_old_fetch_yarn,R.drawable.tiger_old_fetch_mouse,
        R.drawable.tiger_old_fetch_plush,R.drawable.tiger_old_rope_play,1);

    private static final Pack LION_MALE_CUB=new Pack("lion","male",MainActivity.PetStage.CUB,
        R.drawable.lion_male_cub_run_down,R.drawable.lion_male_cub_run_left,
        R.drawable.lion_male_cub_run_right,R.drawable.lion_male_cub_run_up,
        R.drawable.lion_male_cub_fetch_ball,R.drawable.lion_male_cub_fetch_tennis,
        R.drawable.lion_male_cub_fetch_yarn,R.drawable.lion_male_cub_fetch_mouse,
        R.drawable.lion_male_cub_fetch_plush,R.drawable.lion_male_cub_rope_play,1);

    private static final Pack LION_MALE_TEEN=new Pack("lion","male",MainActivity.PetStage.TEEN,
        R.drawable.lion_male_teen_run_down,R.drawable.lion_male_teen_run_left,
        R.drawable.lion_male_teen_run_right,R.drawable.lion_male_teen_run_up,
        R.drawable.lion_male_teen_fetch_ball,R.drawable.lion_male_teen_fetch_tennis,
        R.drawable.lion_male_teen_fetch_yarn,R.drawable.lion_male_teen_fetch_mouse,
        R.drawable.lion_male_teen_fetch_plush,R.drawable.lion_male_teen_rope_play,1);

    private static final Pack LION_MALE_ADULT=new Pack("lion","male",MainActivity.PetStage.ADULT,
        R.drawable.lion_male_adult_run_down,R.drawable.lion_male_adult_run_left,
        R.drawable.lion_male_adult_run_right,R.drawable.lion_male_adult_run_up,
        R.drawable.lion_male_adult_fetch_ball,R.drawable.lion_male_adult_fetch_tennis,
        R.drawable.lion_male_adult_fetch_yarn,R.drawable.lion_male_adult_fetch_mouse,
        R.drawable.lion_male_adult_fetch_plush,R.drawable.lion_male_adult_rope_play,1);

    private static final Pack LION_MALE_OLD=new Pack("lion","male",MainActivity.PetStage.OLD,
        R.drawable.lion_male_old_run_down,R.drawable.lion_male_old_run_left,
        R.drawable.lion_male_old_run_right,R.drawable.lion_male_old_run_up,
        R.drawable.lion_male_old_fetch_ball,R.drawable.lion_male_old_fetch_tennis,
        R.drawable.lion_male_old_fetch_yarn,R.drawable.lion_male_old_fetch_mouse,
        R.drawable.lion_male_old_fetch_plush,R.drawable.lion_male_old_rope_play,1);

    private static final Pack LION_FEMALE_CUB=new Pack("lion","female",MainActivity.PetStage.CUB,
        R.drawable.lion_female_cub_run_down,R.drawable.lion_female_cub_run_left,
        R.drawable.lion_female_cub_run_right,R.drawable.lion_female_cub_run_up,
        R.drawable.lion_female_cub_fetch_ball,R.drawable.lion_female_cub_fetch_tennis,
        R.drawable.lion_female_cub_fetch_yarn,R.drawable.lion_female_cub_fetch_mouse,
        R.drawable.lion_female_cub_fetch_plush,R.drawable.lion_female_cub_rope_play,1);

    private static final Pack LION_FEMALE_TEEN=new Pack("lion","female",MainActivity.PetStage.TEEN,
        R.drawable.lion_female_teen_run_down,R.drawable.lion_female_teen_run_left,
        R.drawable.lion_female_teen_run_right,R.drawable.lion_female_teen_run_up,
        R.drawable.lion_female_teen_fetch_ball,R.drawable.lion_female_teen_fetch_tennis,
        R.drawable.lion_female_teen_fetch_yarn,R.drawable.lion_female_teen_fetch_mouse,
        R.drawable.lion_female_teen_fetch_plush,R.drawable.lion_female_teen_rope_play,1);

    private static final Pack LION_FEMALE_ADULT=new Pack("lion","female",MainActivity.PetStage.ADULT,
        R.drawable.lion_female_adult_run_down,R.drawable.lion_female_adult_run_left,
        R.drawable.lion_female_adult_run_right,R.drawable.lion_female_adult_run_up,
        R.drawable.lion_female_adult_fetch_ball,R.drawable.lion_female_adult_fetch_tennis,
        R.drawable.lion_female_adult_fetch_yarn,R.drawable.lion_female_adult_fetch_mouse,
        R.drawable.lion_female_adult_fetch_plush,R.drawable.lion_female_adult_rope_play,1);

    private static final Pack LION_FEMALE_OLD=new Pack("lion","female",MainActivity.PetStage.OLD,
        R.drawable.lion_female_old_run_down,R.drawable.lion_female_old_run_left,
        R.drawable.lion_female_old_run_right,R.drawable.lion_female_old_run_up,
        R.drawable.lion_female_old_fetch_ball,R.drawable.lion_female_old_fetch_tennis,
        R.drawable.lion_female_old_fetch_yarn,R.drawable.lion_female_old_fetch_mouse,
        R.drawable.lion_female_old_fetch_plush,R.drawable.lion_female_old_rope_play,1);

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
            throw new IllegalStateException("Aucun pack de jeu tigre pour l'âge "+stage);
        }
        if(!"wolf".equals(species))
            throw new IllegalArgumentException("Espèce inconnue : "+species);
        switch(stage){
            case CUB:return WOLF_CUB;
            case TEEN:return WOLF_TEEN;
            case ADULT:return WOLF_ADULT;
            case OLD:return WOLF_OLD;
        }
        throw new IllegalStateException("Aucun pack de jeu loup pour l'âge "+stage);
    }

    static Pack forStage(MainActivity.PetStage stage){
        switch(stage){
            case CUB:return CUB;
            case TEEN:return TEEN;
            case ADULT:return ADULT;
            case OLD:return OLD;
        }
        throw new IllegalStateException("Aucun pack de jeu pour l'âge "+stage);
    }

    private GameSprites(){}
}
