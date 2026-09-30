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
        // Le moindre déplacement vertical significatif doit rester lisible à l'écran.
        // Le seuil 0,45 évite qu'une diagonale montante soit dessinée de profil.
        if(Math.abs(dy)>0.005f && Math.abs(dy)>=Math.abs(dx)*0.45f)
            return dy<0?UP:DOWN;
        return dx<0?LEFT:RIGHT;
    }

    static float mirror(int direction){
        return direction==RIGHT?-1f:1f;
    }

    private SpriteMotion(){}
}
