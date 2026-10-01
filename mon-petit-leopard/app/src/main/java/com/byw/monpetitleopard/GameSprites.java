package com.byw.monpetitleopard;

final class GameSprites {
    static final int FRAME_SIZE=256;

    static final class Pack {
        final MainActivity.PetStage stage;
        final int runDown,runLeft,runRight,runUp;
        final int fetchBall,fetchTennis,fetchYarn,fetchMouse,fetchPlush,ropePlay;

        Pack(MainActivity.PetStage stage,
             int runDown,int runLeft,int runRight,int runUp,
             int fetchBall,int fetchTennis,int fetchYarn,int fetchMouse,int fetchPlush,
             int ropePlay){
            this.stage=stage;
            this.runDown=runDown;this.runLeft=runLeft;this.runRight=runRight;this.runUp=runUp;
            this.fetchBall=fetchBall;this.fetchTennis=fetchTennis;this.fetchYarn=fetchYarn;
            this.fetchMouse=fetchMouse;this.fetchPlush=fetchPlush;this.ropePlay=ropePlay;
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
            if(res==ropePlay)return FRAME_SIZE*5;
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
