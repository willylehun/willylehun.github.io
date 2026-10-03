# Packs de sprites — Mon Petit Léopard

## Contrat actuel — v0.8.9

Les trois espèces utilisent le même moteur de jeu et des ressources indépendantes.
Le nom `leopard` reste celui des sauvegardes historiques ; `wolf` identifie le loup
et `tiger` le tigre. L'ajout du tigre ne migre ni n'efface les profils existants.

| Espèce | Petit | Ado | Adulte | Vieux |
| --- | --- | --- | --- | --- |
| Léopard | `res-cub` | `res-teen` | `res-adult` | `res-old` |
| Loup | `res-wolf-cub` | `res-wolf-teen` | `res-wolf-adult` | `res-wolf-old` |
| Tigre | `res-tiger-cub` | `res-tiger-teen` | `res-tiger-adult` | `res-tiger-old` |

Chaque frame runtime fait **256 × 256 px**, avec des marges transparentes.
Haut montre le dos, bas montre la face. Les marches gauche et droite utilisent
leurs images distinctes. Les chargeurs refusent les espèces inconnues et ne
substituent jamais une espèce ou un âge lorsqu'une ressource manque.

| Animation | Léopard | Loup | Tigre |
| --- | ---: | ---: | ---: |
| Idle, par direction | 1 | 1 | 1 |
| Marche et course, par direction | 6 | 6 | 6 |
| Saut | 5 | 5 | 5 |
| Mange / dort | 3 | 3 | 3 |
| Humeurs | 12 | 12 | 12 |
| Rapport d'objet / soin | 1 | 1 | 1 |
| Corde | 5 | 1, traction animée par le moteur | 1, traction animée par le moteur |
| Griffoir | 2 | 1, mouvement animé par le moteur | 1, mouvement animé par le moteur |
| Biberon, petit uniquement | 1 | 1 | 1 |

Les huit planches loup originales sont conservées dans `source-assets/wolf-v086/`.
`tools/prepare_v086_wolf_assets.py` prépare leurs découpes et la vraie transparence,
normalise les canevas et crée quatre portraits tête pour le jeton de promenade.
La course reprend les six poses de marche de la même espèce et du même âge, jouées
à la cadence du jeu. Les poses uniques de corde et de griffoir restent des poses
uniques : le moteur fournit le mouvement sans inventer de nouvelles images.

Le choix d'animal, l'en-tête, les étapes sexe/nom et le jeton de promenade suivent
l'espèce et l'âge du profil. Les besoins, restrictions par âge, objets, récompenses,
goûts, lassitude et durées restent communs aux trois espèces. Les activités visuelles
en cours sont arrêtées au changement d'âge pour ne conserver aucun ancien sprite.

Le test `test_wolf_behavior.py` conserve son nom historique et vérifie désormais
les trois espèces. Les validateurs `validate_wolf_sprite_packs.py` et
`validate_tiger_sprite_packs.py` complètent les contrats du léopard. Le workflow
prépare les trois espèces avant les tests et le build. Les manifestes par espèce
décrivent la provenance des découpes.

### Intégration du tigre v0.8.9

Le choix initial propose Léopard, Loup et Tigre, puis le sexe et le nom. Les six
emplacements restent communs aux trois espèces. Le tigre utilise les quatre stades
existants : Tigreau, Tigre ado, Tigre adulte et Vieux tigre. Les naissances entre
deux tigres compatibles créent un tigreau avec son propre profil.

Les huit PNG fournis sont conservés sans modification dans
`source-assets/tiger-v089/`. `tools/prepare_v089_tiger_assets.py` extrait les poses
avec des coordonnées propres à chaque planche et prépare la transparence réelle.
Les quatre dossiers contiennent 109 ressources de jeu et quatre portraits de
promenade au total ; `tiger-sprite-manifest.json` conserve les sources, découpes
et empreintes. Les poses gauche mal étiquetées sur les planches sont orientées
correctement ; les marches gauche et droite restent leurs séquences distinctes.

Le tigre emprunte les mêmes chemins d'action que le léopard pour l'alimentation,
la gamelle d'eau, les soins, les caresses, les jeux du salon et du jardin, la corde,
le griffoir, le sommeil et la promenade. Aucun coefficient de besoin, goût, durée,
récompense ou restriction par âge n'est ajouté pour cette espèce.

### Retour de promenade, sommeil et test de croissance v0.8.8

La visibilité de l'animal à la maison est centralisée dans
`MainActivity.updatePetVisibility()`. Les poses, les jeux et les callbacks de
placement respectent tous l'absence du profil pendant une promenade active.
Le départ annule les activités visuelles précédentes ; l'animation de la maison
reste suspendue jusqu'à l'échéance réelle de la promenade. La reprise de
l'activité applique cette règle avant la première image affichée.

La première pose de sommeil du louveteau fournie sur la planche originale
contient deux queues. La première frame runtime reprend donc intégralement la
troisième pose de sommeil du même âge, bulle comprise, après normalisation.
Les autres frames et les PNG sources restent inchangés. Le manifeste décrit
cette provenance ; le validateur compare tous les pixels des frames 1 et 3.

Un bouton temporaire **Dev · Grandir** permet de passer au début du stade suivant
pour le seul profil actif : petit → ado → adulte → vieux. Il est désactivé pour
un animal vieux ou un cycle terminé. `DevGrowth.ENABLED=false` masque le bouton
et désactive la fonction. Le changement ne touche pas les besoins, le sommeil,
les minuteries de promenade ou les autres profils ; l'âge et son entrée
d'historique sont sauvegardés, puis les actions précédentes sont annulées et
les packs ainsi que le portrait sont actualisés immédiatement.

`tools/test_promenade_visibility.py` couvre les retours anticipés, les callbacks
de rendu et la fin de promenade pour les trois espèces aux quatre âges.
`tools/test_dev_growth.py` couvre les frontières d'âge, les profils mixtes, la
sauvegarde et l'inactivité du bouton au dernier stade. Ces tests font partie de
la validation CI avant publication.

### Corrections de découpe v0.8.7

Les pixels semi-transparents des léopards conservent désormais leurs couleurs
d'origine. L'ancienne propagation des RGB depuis l'intérieur des silhouettes
éclaircissait les traits noirs et formait des halos gris, surtout chez le vieux.
Seul l'alpha presque nul est nettoyé ; les noirs, les blancs du pelage et les
couleurs des objets ne sont pas remplacés.

Après la préparation habituelle, `tools/prepare_v087_cutout_assets.py` enlève les
poches de fond confirmées par inspection dans les images des léopards. Les
annotations `source-assets/cutout-v087/leopard-*.json` définissent, pour chaque
âge, action et frame, un point de départ dans le fond, une petite zone de
recherche et des points de pelage ou de symbole à préserver. Les boîtes servent
uniquement à borner une sélection connectée : elles ne sont jamais effacées en
entier. Les polygones de protection couvrent aussi les joues, les museaux et les
contours des pattes. Aucun filtre global ne supprime les pixels blancs ou gris
des animaux.

Les cadres imprimés, chiffres et restes de damier des loups sont retirés par le
générateur de leurs packs, en amont de la normalisation. Les huit planches sources
restent intactes. Les queues et le pelage clair du vieux sont protégés contre les
fausses détections de fond.

L'anticrénelage du contour extérieur du loup est séparé du damier opaque dans un
anneau d'un pixel source. Les pixels intérieurs, les pointes de queue claires et
les bulles protégées conservent leurs RGBA. Les tests contrôlent cette limite sur
des cas synthétiques et sur les poses originales.

`tools/validate_cutout_assets.py` contrôle les couleurs semi-transparentes, les
points devenus transparents et ceux qui doivent rester visibles. Le manifeste
`cutout-audit-manifest.json` relie les annotations aux fichiers et pixels RGBA
générés. Il conserve un delta compressé des seuls pixels retirés. Le validateur
peut ainsi reconstruire exactement l'image normalisée avant détourage et vérifier
son empreinte RGBA ; les tolérances historiques de masse restent inchangées.
Les dimensions, marges et pixels conservés sont contrôlés sur l'image finale.
Cette distinction évite de faire grossir un dessin parce qu'un fond opaque
reliait auparavant le personnage à une bulle ou remplissait une boucle de queue.

La chaîne CI impose ces vérifications après les validateurs des deux espèces et
avant la compilation Android. Un [comparatif avant/après](qa/sprite-cutouts-v087.png)
montre six cas représentatifs sur des fonds contrastés.

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
