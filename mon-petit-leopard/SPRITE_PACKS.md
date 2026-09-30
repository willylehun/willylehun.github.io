# Packs de sprites — Mon Petit Léopard

Les sprites du personnage sont maintenant séparés physiquement et logiquement par âge.

## Zones Android

- `app/src/main/res-cub/drawable-nodpi/` — **Léopardeau**
- `app/src/main/res-teen/drawable-nodpi/` — **Ado**
- `app/src/main/res-adult/drawable-nodpi/` — **Adulte**
- `app/src/main/res-old/drawable-nodpi/` — **Vieux**

Les décors restent dans `app/src/main/res/drawable-nodpi/`. Aucun fichier `leopard_*` ne doit rester dans cette zone commune.

## Règles

1. Aucun fallback inter-âge. Si un asset d'un âge est absent ou invalide, l'animation est bloquée au lieu d'utiliser un autre âge.
2. Les quatre poses statiques de chaque pack sont exactement en **640 × 640**.
3. Les strips de marche ont une hauteur de **640 px** :
   - SIDE : 5 frames de 640 × 640 ;
   - FRONT : 4 frames de 640 × 640 ;
   - BACK : 4 frames de 640 × 640.
4. Gauche et droite utilisent **le même strip SIDE**. La droite est obtenue uniquement par miroir horizontal : aucun second sprite ne peut changer le coloris.
5. Vers le haut = **BACK (dos)**. Vers le bas = **FRONT (face)**.
6. Les 12 humeurs face-joueur du léopardeau (images 1 à 11 + 13 de sa planche) appartiennent uniquement au pack CUB. Ado, adulte et vieux ne doivent jamais réutiliser cette planche.
7. `CharacterSprites.java` est l'unique catalogue de ressources par âge.
8. Le workflow GitHub vérifie la séparation physique avant chaque build.

Les anciens dossiers de travail sont conservés sous `archive/legacy-assets/` afin qu'ils ne puissent plus être confondus avec les ressources réellement compilées.
