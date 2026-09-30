# Packs de sprites — Mon Petit Léopard

Les sprites du personnage sont séparés physiquement et logiquement par âge.

## Zones Android

- `app/src/main/res-cub/drawable-nodpi/` — **Léopardeau**
- `app/src/main/res-teen/drawable-nodpi/` — **Ado**
- `app/src/main/res-adult/drawable-nodpi/` — **Adulte**
- `app/src/main/res-old/drawable-nodpi/` — **Vieux**

Les décors restent dans `app/src/main/res/drawable-nodpi/`. Aucun `leopard_*` ne doit rester dans cette zone commune.

## Références visuelles retrouvées

- CUB / Léopardeau : `1000006717.png`
- TEEN / Ado : `1000006719.png`
- ADULT / Adulte : `1000006718.png`
- OLD / Vieux : `1000006720.png`

Ces quatre planches 7×7 ont été comparées aux packs compilés. Aucun échange de ressources entre âges n'est autorisé.

## Contrat

1. Aucun fallback inter-âge. Une ressource absente/invalide bloque l'animation au lieu de prendre un autre âge.
2. Les poses statiques font **640×640**.
3. SIDE, FRONT et BACK font **4×640×640**, soit **2560×640**.
4. Gauche et droite utilisent le **même SIDE** ; la droite est seulement son miroir horizontal.
5. Haut = **BACK / dos**. Bas = **FRONT / face**. La direction est calculée dans les coordonnées normalisées de la scène.
6. Le CUB possède en plus `leopard_cub_face_moods.webp` : **12 vues de face 80×80**, issues des images **1–11 + 13** de `1000006717.png`.
7. Cette planche d'humeurs appartient exclusivement au CUB. TEEN, ADULT et OLD ne peuvent pas la charger.
8. `CharacterSprites.java` est la source unique de vérité et les tests CI vérifient dimensions, séparation et directions.

L'ancien atlas CUB tronqué reste archivé sous `archive/invalid-assets/` et n'est jamais compilé.


## Comportement v0.5.6
- Les quatre âges ont leur propre atlas d'humeurs face-joueur, rangé dans leur zone de ressources.
- Le haut utilise **BACK (dos)** et le bas **FRONT (face)** ; gauche/droite partagent exactement le même SIDE.
- Après une marche de dos ou de côté, le temps face au joueur est calculé pour garantir au moins **70 %** de temps face hors sommeil.
- Le sommeil naturel dure **au minimum 1 min 30**. Une fin naturelle du cycle force la jauge Sommeil à **100 %** ; une interaction du joueur peut réveiller le léopard plus tôt.
- Les réactions heureuses utilisent l'atlas face et n'affichent plus l'ancien PNG happy dont certaines oreilles étaient tronquées.
