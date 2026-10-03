# Packs de sprites — Mon Petit Léopard

## Contrat actuel — v0.8.6

Les deux espèces utilisent le même moteur de jeu et des ressources indépendantes.
Le nom `leopard` reste celui des sauvegardes historiques ; `wolf` identifie le loup.

| Espèce | Louveteau / léopardeau | Ado | Adulte | Vieux |
| --- | --- | --- | --- | --- |
| Léopard | `res-cub` | `res-teen` | `res-adult` | `res-old` |
| Loup | `res-wolf-cub` | `res-wolf-teen` | `res-wolf-adult` | `res-wolf-old` |

Chaque frame runtime fait **256 × 256 px**, avec des marges transparentes.
Haut montre le dos, bas montre la face. Les marches gauche et droite utilisent
leurs images distinctes. Les chargeurs refusent les espèces inconnues et ne
substituent jamais une espèce ou un âge lorsqu'une ressource manque.

| Animation | Léopard | Loup |
| --- | ---: | ---: |
| Idle, par direction | 1 | 1 |
| Marche et course, par direction | 6 | 6 |
| Saut | 5 | 5 |
| Mange / dort | 3 | 3 |
| Humeurs | 12 | 12 |
| Rapport d'objet / soin | 1 | 1 |
| Corde | 5 | 1, traction animée par le moteur |
| Griffoir | 2 | 1, mouvement animé par le moteur |
| Biberon, petit uniquement | 1 | 1 |

Les huit planches loup originales sont conservées dans `source-assets/wolf-v086/`.
`tools/prepare_v086_wolf_assets.py` prépare leurs découpes et la vraie transparence,
normalise les canevas et crée quatre portraits tête pour le jeton de promenade.
La course reprend les six poses de marche de la même espèce et du même âge, jouées
à la cadence du jeu. Les poses uniques de corde et de griffoir restent des poses
uniques : le moteur fournit le mouvement sans inventer de nouvelles images.

Le choix d'animal, l'en-tête, les étapes sexe/nom et le jeton de promenade suivent
l'espèce et l'âge du profil. Les besoins, restrictions par âge, objets, récompenses,
goûts, lassitude et durées restent communs aux deux espèces. Les activités visuelles
en cours sont arrêtées au changement d'âge pour ne conserver aucun ancien sprite.

Les tests `test_wolf_behavior.py` et `validate_wolf_sprite_packs.py` complètent les
contrats existants. Le workflow prépare les deux espèces avant les tests et le
build. Le manifeste `wolf-sprite-manifest.json` décrit la provenance des découpes.

## Historique des contrats antérieurs

Les sections ci-dessous décrivent les anciennes étapes de normalisation. Le
contrat runtime actuel de 256 px ci-dessus remplace leurs dimensions historiques.

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


## Comportement v0.5.7
- Les quatre âges ont leur propre atlas d'humeurs face-joueur, rangé dans leur zone de ressources.
- Le haut utilise **BACK (dos)** et le bas **FRONT (face)** ; gauche/droite partagent exactement le même SIDE.
- Après une marche de dos ou de côté, le temps face au joueur est calculé pour garantir au moins **70 %** de temps face hors sommeil.
- Le sommeil naturel dure **au minimum 1 min 30**. Une fin naturelle du cycle force la jauge Sommeil à **100 %** ; une interaction du joueur peut réveiller le léopard plus tôt.
- Les réactions heureuses utilisent l'atlas face et n'affichent plus l'ancien PNG happy dont certaines oreilles étaient tronquées.


## Préparation runtime v0.5.7
Les sources v0.5.6 sont figées sous `source-assets/v056/`. Avant chaque build, `tools/prepare_v057_runtime_assets.py` :
- remargine toutes les poses et marches pour protéger les pointes d'oreilles ;
- produit **5 phases CUB** pour SIDE / FRONT / BACK sans mélanger les orientations ;
- nettoie et remargine les atlas d'humeurs face ;
- conserve salon et jardin dans des zones de déplacement sans mobilier ;
- recadre le jardin avant les barrières de premier plan.

Dans le moteur : déplacement haut = BACK/dos, bas = FRONT/face, gauche/droite = même SIDE avec miroir.
