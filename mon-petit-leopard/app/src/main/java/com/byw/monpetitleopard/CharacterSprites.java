package com.byw.monpetitleopard;

final class CharacterSprites {
    static final boolean FACE_ATLAS_REVIEWED=true;
    static final int FRAME_SIZE=256;

    static final class Pack {
        final MainActivity.PetStage stage;
        final String zone;
        final int idleDown,idleLeft,idleRight,idleUp;
        final int walkDown,walkLeft,walkRight,walkUp;
        final int jump,eat,sleep,moods;

        Pack(MainActivity.PetStage stage,String zone,
             int idleDown,int idleLeft,int idleRight,int idleUp,
             int walkDown,int walkLeft,int walkRight,int walkUp,
             int jump,int eat,int sleep,int moods){
            this.stage=stage;this.zone=zone;
            this.idleDown=idleDown;this.idleLeft=idleLeft;this.idleRight=idleRight;this.idleUp=idleUp;
            this.walkDown=walkDown;this.walkLeft=walkLeft;this.walkRight=walkRight;this.walkUp=walkUp;
            this.jump=jump;this.eat=eat;this.sleep=sleep;this.moods=moods;
        }

        int idle(MainActivity.TravelDirection direction){
            switch(direction){
                case LEFT:return idleLeft;
                case RIGHT:return idleRight;
                case UP:return idleUp;
                case DOWN:
                default:return idleDown;
            }
        }

        int walk(MainActivity.TravelDirection direction){
            switch(direction){
                case LEFT:return walkLeft;
                case RIGHT:return walkRight;
                case UP:return walkUp;
                case DOWN:
                default:return walkDown;
            }
        }

        boolean ownsIdle(int res){
            return res==idleDown||res==idleLeft||res==idleRight||res==idleUp;
        }

        boolean ownsWalk(int res){
            return res==walkDown||res==walkLeft||res==walkRight||res==walkUp;
        }

        boolean hasFaceMoods(){return moods!=0;}

        int expectedWidth(int res){
            if(ownsIdle(res))return FRAME_SIZE;
            if(ownsWalk(res))return FRAME_SIZE*6;
            if(res==jump)return FRAME_SIZE*5;
            if(res==eat||res==sleep)return FRAME_SIZE*3;
            if(res==moods)return FRAME_SIZE*12;
            return -1;
        }

        int[] allResources(){
            return new int[]{idleDown,idleLeft,idleRight,idleUp,
                walkDown,walkLeft,walkRight,walkUp,jump,eat,sleep,moods};
        }
    }

    private static final Pack CUB=new Pack(MainActivity.PetStage.CUB,"res-cub",
        R.drawable.leopard_cub_idle_down,R.drawable.leopard_cub_idle_left,
        R.drawable.leopard_cub_idle_right,R.drawable.leopard_cub_idle_up,
        R.drawable.leopard_cub_walk_down,R.drawable.leopard_cub_walk_left,
        R.drawable.leopard_cub_walk_right,R.drawable.leopard_cub_walk_up,
        R.drawable.leopard_cub_jump,R.drawable.leopard_cub_eat,
        R.drawable.leopard_cub_sleep,R.drawable.leopard_cub_moods);

    private static final Pack TEEN=new Pack(MainActivity.PetStage.TEEN,"res-teen",
        R.drawable.leopard_teen_idle_down,R.drawable.leopard_teen_idle_left,
        R.drawable.leopard_teen_idle_right,R.drawable.leopard_teen_idle_up,
        R.drawable.leopard_teen_walk_down,R.drawable.leopard_teen_walk_left,
        R.drawable.leopard_teen_walk_right,R.drawable.leopard_teen_walk_up,
        R.drawable.leopard_teen_jump,R.drawable.leopard_teen_eat,
        R.drawable.leopard_teen_sleep,R.drawable.leopard_teen_moods);

    private static final Pack ADULT=new Pack(MainActivity.PetStage.ADULT,"res-adult",
        R.drawable.leopard_adult_idle_down,R.drawable.leopard_adult_idle_left,
        R.drawable.leopard_adult_idle_right,R.drawable.leopard_adult_idle_up,
        R.drawable.leopard_adult_walk_down,R.drawable.leopard_adult_walk_left,
        R.drawable.leopard_adult_walk_right,R.drawable.leopard_adult_walk_up,
        R.drawable.leopard_adult_jump,R.drawable.leopard_adult_eat,
        R.drawable.leopard_adult_sleep,R.drawable.leopard_adult_moods);

    private static final Pack OLD=new Pack(MainActivity.PetStage.OLD,"res-old",
        R.drawable.leopard_old_idle_down,R.drawable.leopard_old_idle_left,
        R.drawable.leopard_old_idle_right,R.drawable.leopard_old_idle_up,
        R.drawable.leopard_old_walk_down,R.drawable.leopard_old_walk_left,
        R.drawable.leopard_old_walk_right,R.drawable.leopard_old_walk_up,
        R.drawable.leopard_old_jump,R.drawable.leopard_old_eat,
        R.drawable.leopard_old_sleep,R.drawable.leopard_old_moods);

    static Pack forStage(MainActivity.PetStage stage){
        switch(stage){
            case CUB:return CUB;
            case TEEN:return TEEN;
            case ADULT:return ADULT;
            case OLD:return OLD;
        }
        throw new IllegalStateException("Aucun pack pour l'âge "+stage);
    }

    private CharacterSprites(){}
}
