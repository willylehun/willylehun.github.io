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
        float ax=Math.abs(dx), ay=Math.abs(dy);

        // WALK_UP / WALK_DOWN uniquement quand la trajectoire est vraiment verticale.
        // Une diagonale qui part clairement à gauche/droite reste en profil.
        if(ay>0.004f && ax<=ay*.35f)
            return dy<0?UP:DOWN;
        if(ax>0.004f)
            return dx<0?LEFT:RIGHT;
        if(ay>0.004f)
            return dy<0?UP:DOWN;
        return DOWN;
    }

    static float mirror(int direction){
        return direction==RIGHT?-1f:1f;
    }

    private SpriteMotion(){}
}
