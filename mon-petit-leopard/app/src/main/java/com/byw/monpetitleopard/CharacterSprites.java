package com.byw.monpetitleopard;

final class CharacterSprites {
    static final boolean FACE_ATLAS_REVIEWED=true;

    static final class Pack {
        final MainActivity.PetStage stage;
        final String zone;
        final int idle,happy,tired,sleep;
        final int walkSide,walkFront,walkBack;
        final int faceMoods,faceFrameSize,faceFrameCount;
        final int sideFrames,frontFrames,backFrames;

        Pack(MainActivity.PetStage stage,String zone,
             int idle,int happy,int tired,int sleep,
             int walkSide,int walkFront,int walkBack,
             int faceMoods,int faceFrameSize,int faceFrameCount,
             int sideFrames,int frontFrames,int backFrames){
            this.stage=stage;this.zone=zone;
            this.idle=idle;this.happy=happy;this.tired=tired;this.sleep=sleep;
            this.walkSide=walkSide;this.walkFront=walkFront;this.walkBack=walkBack;
            this.faceMoods=faceMoods;this.faceFrameSize=faceFrameSize;this.faceFrameCount=faceFrameCount;
            this.sideFrames=sideFrames;this.frontFrames=frontFrames;this.backFrames=backFrames;
        }
        int mood(MainActivity.PetMood mood){
            switch(mood){case HAPPY:return happy;case TIRED:return tired;case SLEEP:return sleep;case IDLE:return idle;}
            throw new IllegalStateException("Humeur inconnue pour "+stage+": "+mood);
        }
        int walk(MainActivity.WalkMode mode){
            switch(mode){case FRONT:return walkFront;case BACK:return walkBack;case SIDE:return walkSide;}
            throw new IllegalStateException("Marche inconnue pour "+stage+": "+mode);
        }
        int frameCount(MainActivity.WalkMode mode){
            switch(mode){case FRONT:return frontFrames;case BACK:return backFrames;case SIDE:return sideFrames;}
            throw new IllegalStateException("Mode de marche inconnu: "+mode);
        }
        boolean ownsMood(int res){return res==idle||res==happy||res==tired||res==sleep;}
        boolean ownsWalk(int res){return res==walkSide||res==walkFront||res==walkBack;}
        boolean hasFaceMoods(){return faceMoods!=0&&faceFrameSize>0&&faceFrameCount>0;}
        int[] allResources(){
            return hasFaceMoods()?new int[]{idle,happy,tired,sleep,walkSide,walkFront,walkBack,faceMoods}
                    :new int[]{idle,happy,tired,sleep,walkSide,walkFront,walkBack};
        }
    }

    private static final Pack CUB=new Pack(MainActivity.PetStage.CUB,"res-cub",
        R.drawable.leopard_cub_idle,R.drawable.leopard_cub_happy,R.drawable.leopard_cub_tired,R.drawable.leopard_cub_sleep,
        R.drawable.leopard_cub_walk_side,R.drawable.leopard_cub_walk_front,R.drawable.leopard_cub_walk_back,
        R.drawable.leopard_cub_face_moods,320,11,4,4,4);
    private static final Pack TEEN=new Pack(MainActivity.PetStage.TEEN,"res-teen",
        R.drawable.leopard_teen_idle,R.drawable.leopard_teen_happy,R.drawable.leopard_teen_tired,R.drawable.leopard_teen_sleep,
        R.drawable.leopard_teen_walk_side,R.drawable.leopard_teen_walk_front,R.drawable.leopard_teen_walk_back,
        R.drawable.leopard_teen_face_moods,320,11,4,4,4);
    private static final Pack ADULT=new Pack(MainActivity.PetStage.ADULT,"res-adult",
        R.drawable.leopard_adult_idle,R.drawable.leopard_adult_happy,R.drawable.leopard_adult_tired,R.drawable.leopard_adult_sleep,
        R.drawable.leopard_adult_walk_side,R.drawable.leopard_adult_walk_front,R.drawable.leopard_adult_walk_back,
        R.drawable.leopard_adult_face_moods,320,11,4,4,4);
    private static final Pack OLD=new Pack(MainActivity.PetStage.OLD,"res-old",
        R.drawable.leopard_old_idle,R.drawable.leopard_old_happy,R.drawable.leopard_old_tired,R.drawable.leopard_old_sleep,
        R.drawable.leopard_old_walk_side,R.drawable.leopard_old_walk_front,R.drawable.leopard_old_walk_back,
        R.drawable.leopard_old_face_moods,320,11,4,4,4);

    static Pack forStage(MainActivity.PetStage stage){
        switch(stage){case CUB:return CUB;case TEEN:return TEEN;case ADULT:return ADULT;case OLD:return OLD;}
        throw new IllegalStateException("Aucun pack pour l'âge "+stage);
    }
    private CharacterSprites(){}
}
