package com.byw.monpetitleopard;
final class SpriteMotion {
    static final int LEFT=0,RIGHT=1,UP=2,DOWN=3;
    static int direction(float dx,float dy,float width,float height){
        float x=dx*Math.max(1f,width), y=dy*Math.max(1f,height);
        if(Math.abs(y)>Math.abs(x))return y<0?UP:DOWN;
        return x<0?LEFT:RIGHT;
    }
    static float mirror(int direction){return direction==RIGHT?-1f:1f;}
    private SpriteMotion(){}
}
