package com.byw.monpetitleopard;

/**
 * Direction de déplacement dans l'espace normalisé de la scène.
 *
 * dx/dy sont des deltas normalisés (petNX/petNY). On ne les repondère jamais
 * par la largeur/hauteur écran : cela ferait changer FRONT/BACK selon le téléphone.
 */
final class SpriteMotion {
    static final int LEFT=0,RIGHT=1,UP=2,DOWN=3;

    static int direction(float dx,float dy,float width,float height){
        // Priorité stricte au déplacement horizontal :
        // si le léopard se déplace vers la gauche/droite, on affiche WALK_LEFT/RIGHT,
        // même si le trajet est légèrement diagonal. FRONT/BACK ne sont utilisés
        // que pour un déplacement réellement vertical.
        if(Math.abs(dx)>0.004f)
            return dx<0?LEFT:RIGHT;
        if(Math.abs(dy)>0.004f)
            return dy<0?UP:DOWN;
        return DOWN;
    }

    static float mirror(int direction){
        return direction==RIGHT?-1f:1f;
    }

    private SpriteMotion(){}
}
