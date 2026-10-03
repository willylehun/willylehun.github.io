package com.byw.monpetitleopard;

final class CharacterSprites {
    static final boolean FACE_ATLAS_REVIEWED=true;
    static final int FRAME_SIZE=256;

    static final class Pack {
        final String species;
        final String sex;
        final MainActivity.PetStage stage;
        final String zone;
        final int idleDown,idleLeft,idleRight,idleUp;
        final int walkDown,walkLeft,walkRight,walkUp;
        final int jump,eat,sleep,moods;

        Pack(MainActivity.PetStage stage,String zone,
             int idleDown,int idleLeft,int idleRight,int idleUp,
             int walkDown,int walkLeft,int walkRight,int walkUp,
             int jump,int eat,int sleep,int moods){
            this("leopard",stage,zone,idleDown,idleLeft,idleRight,idleUp,
                walkDown,walkLeft,walkRight,walkUp,jump,eat,sleep,moods);
        }

        Pack(String species,MainActivity.PetStage stage,String zone,
             int idleDown,int idleLeft,int idleRight,int idleUp,
             int walkDown,int walkLeft,int walkRight,int walkUp,
             int jump,int eat,int sleep,int moods){
            this(species,"",stage,zone,idleDown,idleLeft,idleRight,idleUp,walkDown,walkLeft,walkRight,walkUp,jump,eat,sleep,moods);
        }

        Pack(String species,String sex,MainActivity.PetStage stage,String zone,
             int idleDown,int idleLeft,int idleRight,int idleUp,
             int walkDown,int walkLeft,int walkRight,int walkUp,
             int jump,int eat,int sleep,int moods){
            this.species=species;
            this.sex=sex;
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

    private static final Pack WOLF_CUB=new Pack("wolf",MainActivity.PetStage.CUB,"res-wolf-cub",
        R.drawable.wolf_cub_idle_down,R.drawable.wolf_cub_idle_left,
        R.drawable.wolf_cub_idle_right,R.drawable.wolf_cub_idle_up,
        R.drawable.wolf_cub_walk_down,R.drawable.wolf_cub_walk_left,
        R.drawable.wolf_cub_walk_right,R.drawable.wolf_cub_walk_up,
        R.drawable.wolf_cub_jump,R.drawable.wolf_cub_eat,
        R.drawable.wolf_cub_sleep,R.drawable.wolf_cub_moods);

    private static final Pack WOLF_TEEN=new Pack("wolf",MainActivity.PetStage.TEEN,"res-wolf-teen",
        R.drawable.wolf_teen_idle_down,R.drawable.wolf_teen_idle_left,
        R.drawable.wolf_teen_idle_right,R.drawable.wolf_teen_idle_up,
        R.drawable.wolf_teen_walk_down,R.drawable.wolf_teen_walk_left,
        R.drawable.wolf_teen_walk_right,R.drawable.wolf_teen_walk_up,
        R.drawable.wolf_teen_jump,R.drawable.wolf_teen_eat,
        R.drawable.wolf_teen_sleep,R.drawable.wolf_teen_moods);

    private static final Pack WOLF_ADULT=new Pack("wolf",MainActivity.PetStage.ADULT,"res-wolf-adult",
        R.drawable.wolf_adult_idle_down,R.drawable.wolf_adult_idle_left,
        R.drawable.wolf_adult_idle_right,R.drawable.wolf_adult_idle_up,
        R.drawable.wolf_adult_walk_down,R.drawable.wolf_adult_walk_left,
        R.drawable.wolf_adult_walk_right,R.drawable.wolf_adult_walk_up,
        R.drawable.wolf_adult_jump,R.drawable.wolf_adult_eat,
        R.drawable.wolf_adult_sleep,R.drawable.wolf_adult_moods);

    private static final Pack WOLF_OLD=new Pack("wolf",MainActivity.PetStage.OLD,"res-wolf-old",
        R.drawable.wolf_old_idle_down,R.drawable.wolf_old_idle_left,
        R.drawable.wolf_old_idle_right,R.drawable.wolf_old_idle_up,
        R.drawable.wolf_old_walk_down,R.drawable.wolf_old_walk_left,
        R.drawable.wolf_old_walk_right,R.drawable.wolf_old_walk_up,
        R.drawable.wolf_old_jump,R.drawable.wolf_old_eat,
        R.drawable.wolf_old_sleep,R.drawable.wolf_old_moods);

    private static final Pack TIGER_CUB=new Pack("tiger",MainActivity.PetStage.CUB,"res-tiger-cub",
        R.drawable.tiger_cub_idle_down,R.drawable.tiger_cub_idle_left,
        R.drawable.tiger_cub_idle_right,R.drawable.tiger_cub_idle_up,
        R.drawable.tiger_cub_walk_down,R.drawable.tiger_cub_walk_left,
        R.drawable.tiger_cub_walk_right,R.drawable.tiger_cub_walk_up,
        R.drawable.tiger_cub_jump,R.drawable.tiger_cub_eat,
        R.drawable.tiger_cub_sleep,R.drawable.tiger_cub_moods);

    private static final Pack TIGER_TEEN=new Pack("tiger",MainActivity.PetStage.TEEN,"res-tiger-teen",
        R.drawable.tiger_teen_idle_down,R.drawable.tiger_teen_idle_left,
        R.drawable.tiger_teen_idle_right,R.drawable.tiger_teen_idle_up,
        R.drawable.tiger_teen_walk_down,R.drawable.tiger_teen_walk_left,
        R.drawable.tiger_teen_walk_right,R.drawable.tiger_teen_walk_up,
        R.drawable.tiger_teen_jump,R.drawable.tiger_teen_eat,
        R.drawable.tiger_teen_sleep,R.drawable.tiger_teen_moods);

    private static final Pack TIGER_ADULT=new Pack("tiger",MainActivity.PetStage.ADULT,"res-tiger-adult",
        R.drawable.tiger_adult_idle_down,R.drawable.tiger_adult_idle_left,
        R.drawable.tiger_adult_idle_right,R.drawable.tiger_adult_idle_up,
        R.drawable.tiger_adult_walk_down,R.drawable.tiger_adult_walk_left,
        R.drawable.tiger_adult_walk_right,R.drawable.tiger_adult_walk_up,
        R.drawable.tiger_adult_jump,R.drawable.tiger_adult_eat,
        R.drawable.tiger_adult_sleep,R.drawable.tiger_adult_moods);

    private static final Pack TIGER_OLD=new Pack("tiger",MainActivity.PetStage.OLD,"res-tiger-old",
        R.drawable.tiger_old_idle_down,R.drawable.tiger_old_idle_left,
        R.drawable.tiger_old_idle_right,R.drawable.tiger_old_idle_up,
        R.drawable.tiger_old_walk_down,R.drawable.tiger_old_walk_left,
        R.drawable.tiger_old_walk_right,R.drawable.tiger_old_walk_up,
        R.drawable.tiger_old_jump,R.drawable.tiger_old_eat,
        R.drawable.tiger_old_sleep,R.drawable.tiger_old_moods);

    private static final Pack FOX_CUB=new Pack("fox",MainActivity.PetStage.CUB,"res-fox-cub",
        R.drawable.fox_cub_idle_down,R.drawable.fox_cub_idle_left,
        R.drawable.fox_cub_idle_right,R.drawable.fox_cub_idle_up,
        R.drawable.fox_cub_walk_down,R.drawable.fox_cub_walk_left,
        R.drawable.fox_cub_walk_right,R.drawable.fox_cub_walk_up,
        R.drawable.fox_cub_jump,R.drawable.fox_cub_eat,
        R.drawable.fox_cub_sleep,R.drawable.fox_cub_moods);

    private static final Pack FOX_TEEN=new Pack("fox",MainActivity.PetStage.TEEN,"res-fox-teen",
        R.drawable.fox_teen_idle_down,R.drawable.fox_teen_idle_left,
        R.drawable.fox_teen_idle_right,R.drawable.fox_teen_idle_up,
        R.drawable.fox_teen_walk_down,R.drawable.fox_teen_walk_left,
        R.drawable.fox_teen_walk_right,R.drawable.fox_teen_walk_up,
        R.drawable.fox_teen_jump,R.drawable.fox_teen_eat,
        R.drawable.fox_teen_sleep,R.drawable.fox_teen_moods);

    private static final Pack FOX_ADULT=new Pack("fox",MainActivity.PetStage.ADULT,"res-fox-adult",
        R.drawable.fox_adult_idle_down,R.drawable.fox_adult_idle_left,
        R.drawable.fox_adult_idle_right,R.drawable.fox_adult_idle_up,
        R.drawable.fox_adult_walk_down,R.drawable.fox_adult_walk_left,
        R.drawable.fox_adult_walk_right,R.drawable.fox_adult_walk_up,
        R.drawable.fox_adult_jump,R.drawable.fox_adult_eat,
        R.drawable.fox_adult_sleep,R.drawable.fox_adult_moods);

    private static final Pack FOX_OLD=new Pack("fox",MainActivity.PetStage.OLD,"res-fox-old",
        R.drawable.fox_old_idle_down,R.drawable.fox_old_idle_left,
        R.drawable.fox_old_idle_right,R.drawable.fox_old_idle_up,
        R.drawable.fox_old_walk_down,R.drawable.fox_old_walk_left,
        R.drawable.fox_old_walk_right,R.drawable.fox_old_walk_up,
        R.drawable.fox_old_jump,R.drawable.fox_old_eat,
        R.drawable.fox_old_sleep,R.drawable.fox_old_moods);

    private static final Pack BEAR_CUB=new Pack("bear",MainActivity.PetStage.CUB,"res-bear-cub",
        R.drawable.bear_cub_idle_down,R.drawable.bear_cub_idle_left,
        R.drawable.bear_cub_idle_right,R.drawable.bear_cub_idle_up,
        R.drawable.bear_cub_walk_down,R.drawable.bear_cub_walk_left,
        R.drawable.bear_cub_walk_right,R.drawable.bear_cub_walk_up,
        R.drawable.bear_cub_jump,R.drawable.bear_cub_eat,
        R.drawable.bear_cub_sleep,R.drawable.bear_cub_moods);

    private static final Pack BEAR_TEEN=new Pack("bear",MainActivity.PetStage.TEEN,"res-bear-teen",
        R.drawable.bear_teen_idle_down,R.drawable.bear_teen_idle_left,
        R.drawable.bear_teen_idle_right,R.drawable.bear_teen_idle_up,
        R.drawable.bear_teen_walk_down,R.drawable.bear_teen_walk_left,
        R.drawable.bear_teen_walk_right,R.drawable.bear_teen_walk_up,
        R.drawable.bear_teen_jump,R.drawable.bear_teen_eat,
        R.drawable.bear_teen_sleep,R.drawable.bear_teen_moods);

    private static final Pack BEAR_ADULT=new Pack("bear",MainActivity.PetStage.ADULT,"res-bear-adult",
        R.drawable.bear_adult_idle_down,R.drawable.bear_adult_idle_left,
        R.drawable.bear_adult_idle_right,R.drawable.bear_adult_idle_up,
        R.drawable.bear_adult_walk_down,R.drawable.bear_adult_walk_left,
        R.drawable.bear_adult_walk_right,R.drawable.bear_adult_walk_up,
        R.drawable.bear_adult_jump,R.drawable.bear_adult_eat,
        R.drawable.bear_adult_sleep,R.drawable.bear_adult_moods);

    private static final Pack BEAR_OLD=new Pack("bear",MainActivity.PetStage.OLD,"res-bear-old",
        R.drawable.bear_old_idle_down,R.drawable.bear_old_idle_left,
        R.drawable.bear_old_idle_right,R.drawable.bear_old_idle_up,
        R.drawable.bear_old_walk_down,R.drawable.bear_old_walk_left,
        R.drawable.bear_old_walk_right,R.drawable.bear_old_walk_up,
        R.drawable.bear_old_jump,R.drawable.bear_old_eat,
        R.drawable.bear_old_sleep,R.drawable.bear_old_moods);

    private static final Pack LION_MALE_CUB=new Pack("lion","male",MainActivity.PetStage.CUB,"res-lion-male-cub",
        R.drawable.lion_male_cub_idle_down,R.drawable.lion_male_cub_idle_left,
        R.drawable.lion_male_cub_idle_right,R.drawable.lion_male_cub_idle_up,
        R.drawable.lion_male_cub_walk_down,R.drawable.lion_male_cub_walk_left,
        R.drawable.lion_male_cub_walk_right,R.drawable.lion_male_cub_walk_up,
        R.drawable.lion_male_cub_jump,R.drawable.lion_male_cub_eat,
        R.drawable.lion_male_cub_sleep,R.drawable.lion_male_cub_moods);

    private static final Pack LION_MALE_TEEN=new Pack("lion","male",MainActivity.PetStage.TEEN,"res-lion-male-teen",
        R.drawable.lion_male_teen_idle_down,R.drawable.lion_male_teen_idle_left,
        R.drawable.lion_male_teen_idle_right,R.drawable.lion_male_teen_idle_up,
        R.drawable.lion_male_teen_walk_down,R.drawable.lion_male_teen_walk_left,
        R.drawable.lion_male_teen_walk_right,R.drawable.lion_male_teen_walk_up,
        R.drawable.lion_male_teen_jump,R.drawable.lion_male_teen_eat,
        R.drawable.lion_male_teen_sleep,R.drawable.lion_male_teen_moods);

    private static final Pack LION_MALE_ADULT=new Pack("lion","male",MainActivity.PetStage.ADULT,"res-lion-male-adult",
        R.drawable.lion_male_adult_idle_down,R.drawable.lion_male_adult_idle_left,
        R.drawable.lion_male_adult_idle_right,R.drawable.lion_male_adult_idle_up,
        R.drawable.lion_male_adult_walk_down,R.drawable.lion_male_adult_walk_left,
        R.drawable.lion_male_adult_walk_right,R.drawable.lion_male_adult_walk_up,
        R.drawable.lion_male_adult_jump,R.drawable.lion_male_adult_eat,
        R.drawable.lion_male_adult_sleep,R.drawable.lion_male_adult_moods);

    private static final Pack LION_MALE_OLD=new Pack("lion","male",MainActivity.PetStage.OLD,"res-lion-male-old",
        R.drawable.lion_male_old_idle_down,R.drawable.lion_male_old_idle_left,
        R.drawable.lion_male_old_idle_right,R.drawable.lion_male_old_idle_up,
        R.drawable.lion_male_old_walk_down,R.drawable.lion_male_old_walk_left,
        R.drawable.lion_male_old_walk_right,R.drawable.lion_male_old_walk_up,
        R.drawable.lion_male_old_jump,R.drawable.lion_male_old_eat,
        R.drawable.lion_male_old_sleep,R.drawable.lion_male_old_moods);

    private static final Pack LION_FEMALE_CUB=new Pack("lion","female",MainActivity.PetStage.CUB,"res-lion-female-cub",
        R.drawable.lion_female_cub_idle_down,R.drawable.lion_female_cub_idle_left,
        R.drawable.lion_female_cub_idle_right,R.drawable.lion_female_cub_idle_up,
        R.drawable.lion_female_cub_walk_down,R.drawable.lion_female_cub_walk_left,
        R.drawable.lion_female_cub_walk_right,R.drawable.lion_female_cub_walk_up,
        R.drawable.lion_female_cub_jump,R.drawable.lion_female_cub_eat,
        R.drawable.lion_female_cub_sleep,R.drawable.lion_female_cub_moods);

    private static final Pack LION_FEMALE_TEEN=new Pack("lion","female",MainActivity.PetStage.TEEN,"res-lion-female-teen",
        R.drawable.lion_female_teen_idle_down,R.drawable.lion_female_teen_idle_left,
        R.drawable.lion_female_teen_idle_right,R.drawable.lion_female_teen_idle_up,
        R.drawable.lion_female_teen_walk_down,R.drawable.lion_female_teen_walk_left,
        R.drawable.lion_female_teen_walk_right,R.drawable.lion_female_teen_walk_up,
        R.drawable.lion_female_teen_jump,R.drawable.lion_female_teen_eat,
        R.drawable.lion_female_teen_sleep,R.drawable.lion_female_teen_moods);

    private static final Pack LION_FEMALE_ADULT=new Pack("lion","female",MainActivity.PetStage.ADULT,"res-lion-female-adult",
        R.drawable.lion_female_adult_idle_down,R.drawable.lion_female_adult_idle_left,
        R.drawable.lion_female_adult_idle_right,R.drawable.lion_female_adult_idle_up,
        R.drawable.lion_female_adult_walk_down,R.drawable.lion_female_adult_walk_left,
        R.drawable.lion_female_adult_walk_right,R.drawable.lion_female_adult_walk_up,
        R.drawable.lion_female_adult_jump,R.drawable.lion_female_adult_eat,
        R.drawable.lion_female_adult_sleep,R.drawable.lion_female_adult_moods);

    private static final Pack LION_FEMALE_OLD=new Pack("lion","female",MainActivity.PetStage.OLD,"res-lion-female-old",
        R.drawable.lion_female_old_idle_down,R.drawable.lion_female_old_idle_left,
        R.drawable.lion_female_old_idle_right,R.drawable.lion_female_old_idle_up,
        R.drawable.lion_female_old_walk_down,R.drawable.lion_female_old_walk_left,
        R.drawable.lion_female_old_walk_right,R.drawable.lion_female_old_walk_up,
        R.drawable.lion_female_old_jump,R.drawable.lion_female_old_eat,
        R.drawable.lion_female_old_sleep,R.drawable.lion_female_old_moods);

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
            throw new IllegalStateException("Aucun pack tigre pour l'âge "+stage);
        }
        if("fox".equals(species)){
            switch(stage){
                case CUB:return FOX_CUB;
                case TEEN:return FOX_TEEN;
                case ADULT:return FOX_ADULT;
                case OLD:return FOX_OLD;
            }
            throw new IllegalStateException("Aucun pack renard pour l'âge "+stage);
        }
        if("bear".equals(species)){
            switch(stage){
                case CUB:return BEAR_CUB;
                case TEEN:return BEAR_TEEN;
                case ADULT:return BEAR_ADULT;
                case OLD:return BEAR_OLD;
            }
            throw new IllegalStateException("Aucun pack ours pour l'âge "+stage);
        }
        if(!"wolf".equals(species))
            throw new IllegalArgumentException("Espèce inconnue : "+species);
        switch(stage){
            case CUB:return WOLF_CUB;
            case TEEN:return WOLF_TEEN;
            case ADULT:return WOLF_ADULT;
            case OLD:return WOLF_OLD;
        }
        throw new IllegalStateException("Aucun pack loup pour l'âge "+stage);
    }

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
