# Tests du rendu Android et des retours de promenade

Ce projet Gradle indépendant exécute les classes Java réelles de l'application avec
leurs ressources Android compilées. Il utilise Robolectric 4.14.1, Android API 34
et `GraphicsMode.NATIVE` : `BitmapFactory`, `Canvas` et le dessin des `ImageView`
s'appuient sur le moteur graphique natif Android, plutôt que sur les anciens
substituts graphiques sans rendu de Robolectric.

Référence : [documentation officielle de `GraphicsMode.Mode`](https://robolectric.org/javadoc/4.10/org/robolectric/annotation/GraphicsMode.Mode.html).

Le cycle de vie des activités est piloté par Robolectric. Il s'agit d'une
vérification locale des classes et du rendu Android, pas d'un test sur un appareil
physique. Les profils créés sont isolés dans l'environnement du test.

## Exécution

Prérequis : JDK 17 complet (avec `javac` et `jlink`), Gradle 8.9, Android SDK 35
et Build Tools 35.0.0. `JAVA_HOME` doit pointer vers ce JDK si le Java système
n'inclut que l'environnement d'exécution. Le premier
lancement télécharge Robolectric et son moteur natif depuis les dépôts Maven.

Depuis la racine du dépôt, préparer les ressources dans le même ordre que le
workflow Android, puis lancer les tests. La préparation doit être complètement
terminée avant le lancement ; ne pas régénérer les ressources pendant le test.

```sh
python mon-petit-leopard/tools/prepare_v061_sprite_assets.py
python mon-petit-leopard/tools/prepare_v070_game_assets.py
python mon-petit-leopard/tools/prepare_v075_care_assets.py
python mon-petit-leopard/tools/prepare_v076_promenade_assets.py
python mon-petit-leopard/tools/prepare_v078_garden_assets.py
python mon-petit-leopard/tools/prepare_v086_wolf_assets.py
python mon-petit-leopard/tools/prepare_v087_cutout_assets.py
python mon-petit-leopard/tools/prepare_v089_tiger_assets.py
python mon-petit-leopard/tools/prepare_v0810_lion_assets.py
python mon-petit-leopard/tools/prepare_v0811_fox_assets.py
python mon-petit-leopard/tools/prepare_v0811_bear_assets.py
gradle -p mon-petit-leopard/qa/android-render-tests --no-daemon :app:testDebugUnitTest
```

Les scripts de préparation ont besoin de Pillow et NumPy, comme le workflow
Android principal. Le SDK peut être indiqué avec `ANDROID_HOME` ou le fichier
local `local.properties` de ce projet de test.

Le harness lit `../../app/src/main` par défaut. Pour comparer un autre checkout
préparé, passer `-PqaSourceRoot=/chemin/absolu/mon-petit-leopard` et lancer
`:app:clean` avant de changer cette source. Le harness actuel utilise les six
espèces, dont les deux apparences sexuées du lion, et nécessite les sources
v0.8.11 ou suivantes. Pour comparer une version
antérieure, utiliser le harness du commit correspondant dans une copie isolée :
celui de v0.8.8 ne référence pas encore le tigre et celui de v0.8.9 ne référence
pas encore le lion ; celui de v0.8.10 ne référence pas encore le renard et l’ours.
Pour reproduire la régression
v0.8.7 avec le harness v0.8.8, conserver seulement ses suites
`PromenadeLifecycleGraphicsTest.java` et `NativeGraphicsSmokeTest.java`.

Rapports et captures sont produits sous `app/build/` et exclus du dépôt :

- `reports/tests/testDebugUnitTest/index.html` : rapport lisible ;
- `test-results/testDebugUnitTest/` : résultats JUnit XML ;
- `native-captures/` : captures des quatre âges des animaux, des deux sexes du lion,
  des portraits de promenade, de la maison vide et du retour du renard et de l’ours,
  des six profils mixtes et des petits écrans.

## Couverture

| Suite | Vérification réelle |
| --- | --- |
| `NativeGraphicsSmokeTest` | Décodage d'un sprite avec alpha, dessin d'un `ImageView` sur un fond opaque, conservation des coins transparents. |
| `PromenadeLifecycleGraphicsTest` | Les six espèces aux quatre âges, avec deux parcours par âge pour les lions : départ, pause/arrêt, activité promenade, portrait de la bonne espèce, du bon sexe et du bon âge, clic sur Retour au jardin, reprise anticipée, mise en page, animation, recréation de l'activité et échéance. Pour chaque lion, lionne, renard et ours, visite des quatre pièces via le menu pendant la promenade. |
| `GrowthButtonGraphicsTest` | Clic sur le bouton Dev pour les trois passages d'âge, nouvelle image, annulation du soin et de l'appel en cours, maintien du sommeil, besoins et compétences conservés, espèce et âge persistés après recréation, bouton désactivé au stade vieux. Les trois clics sont aussi testés pendant une promenade, pour les six espèces et les deux sexes du lion. Le rendu du lion, de la lionne, du renard et de l’ours est comparé aux images de leur propre pack après la croissance et au retour. |
| `CompactGrowthLayoutTest` | Bouton Dev et trois commandes du bas entièrement visibles et utilisables à 360 × 640, 320 × 568 et 320 × 480 dp. |
| `TigerIntegrationGraphicsTest` | Aux quatre âges : poses, six images de marche et de course dans chaque direction, manger/sauter/dormir, douze humeurs, quatre soins et disponibilité du biberon. Utilisation réelle des objets : repas, quatre jouets rapportés et corde dans le salon et le jardin, griffoir et repos. Adoption initiale complète à 320 × 480 dp puis réouverture ; descendance tigre avec conservation de l'espèce, des parents et des profils léopard/loup existants. |
| `LionIntegrationGraphicsTest` | Huit variantes sexe × âge soumises au même scénario d’objets et de rendu que le tigre. Adoptions mâle et femelle par les contrôles réels à 320 × 480 dp, deux portraits au choix du sexe et bon portrait au nom/à la réouverture. Neuf visites parmi six profils mixtes, croissance limitée au profil actif et comparaison intégrale des cinq autres sauvegardes. Reproduction entre lions de sexes opposés, refus des autres espèces, sexe du bébé conservé et propre pack rechargé après fermeture. |
| `ForestIntegrationGraphicsTest` | Les quatre âges du renard et de l’ours utilisent le scénario commun d’objets et de rendu. Les six cartes initiales sont atteintes par de vrais gestes de défilement à 320 × 480 dp, avec vérification de chaque portrait. Adoption tactile d’une renarde et d’un ours, choix du sexe, nom et double réouverture. Dix visites parmi six espèces enregistrées, croissance du renard et de l’ours limitée au profil actif. Naissance dans chaque nouvelle espèce, refus des croisements interespèces, parents et cinq sauvegardes voisines conservés. |

Pour la maison vide, le test dessine la scène complète avec son état réel, puis
avec l'animal explicitement masqué. Pendant la promenade, **aucun pixel ne doit
différer** entre les deux rendus. Avant le départ et après l'échéance, l'animal
doit au contraire modifier plus de 100 pixels du décor. Cela vérifie le résultat
composé, en plus de la visibilité du `View`.

Le fond transparent des images affichées est également composé sur deux couleurs
contrastées par le `Canvas` Android natif, avant le départ et après le retour.

Le scénario commun `AnimalActionGraphicsScenario`, utilisé par le tigre,
les huit variantes du lion, le renard et l’ours, compare les pixels du bitmap effectivement affiché
à la frame attendue de son propre pack d’espèce, de sexe et d’âge. Chaque image est ensuite
composée avec le `Canvas` natif pour vérifier qu'elle peint un animal visible et
conserve sa marge transparente. Les jeux utilisent les véritables contrôleurs
de l'application et leurs transitions ; Robolectric pilote les arrivées et la
fin du lancer sans attendre les délais en temps réel. L’adoption du tigre utilise les clics sur sa carte, le sexe et le bouton de
confirmation, ainsi que le champ du nom. Les deux adoptions du lion envoient
réellement `ACTION_DOWN` puis `ACTION_UP` au centre visible des contrôles. Avant
chaque contact, le test révèle le contrôle si nécessaire et vérifie ses limites
complètes à 320 × 480 dp. Les deux portraits au choix du sexe sont également
comparés aux bitmaps du bon pack et doivent être entièrement accessibles.

Les nouvelles adoptions du renard et de l’ours utilisent une suite complète
`ACTION_DOWN` / `ACTION_MOVE` / `ACTION_UP` pour atteindre les cartes et boutons
hors écran, puis `ACTION_DOWN` / `ACTION_UP` pour les activer. Le test ne déplace
pas le `ScrollView` par son API et n’appelle pas les gestionnaires d’adoption.
Chaque contrôle doit être entièrement visible dans la fenêtre réelle avant le
contact ; le défilement ne doit créer aucun profil ni lancer d’activité. Les
prévisualisations du choix de sexe, du nom et du profil sont comparées aux
bitmaps du renard ou de l’ours, qui partagent leur artwork entre sexes.

## Régression reproduite

Le 3 octobre 2026, les huit scénarios de promenade échouaient sur la v0.8.7 au
retour anticipé et après la mise à jour de position : selon l'espèce et l'âge,
4 181 à 7 403 pixels de l'animal apparaissaient dans la maison alors que la
promenade restait active. Les mêmes scénarios passent avec la garde commune de
visibilité v0.8.8. Les bitmaps conservaient leur canal alpha ; le défaut provenait
bien de la remise en visibilité du personnage dans la maison.

Ce harness ne modifie pas `app/build.gradle` et n'ajoute pas de dépendance à l'APK
publié. Le workflow de publication exécute ce harness avant la compilation de
l'APK, en complément des contrôles Python de ressources, d'espèces, de
promenade et de croissance. Les rapports JUnit, le rapport HTML et les captures
natives sont conservés comme artefact CI, même si le gate échoue.

## Résultat v0.8.8

Le harness complet a été exécuté avec succès le 3 octobre 2026 : **16 tests,
0 échec, 0 test ignoré**. Les huit reprises de promenade, les quatre scénarios
de croissance, les trois petits écrans et le décodage/compositing natif passent.
À 320 × 480 dp, la scène mesure 251 dp ; la barre de commandes garde ses 46 dp
et l'espace du bas ses 12 dp. Les quatre boutons restent entièrement visibles.

Deux captures natives ont été conservées hors du dossier de build :

- [`../v088-developer-growth.png`](../v088-developer-growth.png) : louveteau et bouton Dev ;
- [`../v088-promenade-home-empty.png`](../v088-promenade-home-empty.png) : maison vide pendant une promenade, après les trois passages d'âge.

## Vérification candidate v0.8.9

Le 3 octobre 2026, les **28 tests ont passé localement : 0 échec, 0 erreur,
0 test ignoré**. L'exécution a utilisé une copie cohérente des sources et
ressources, contrôlée par SHA-256 avant et après copie, pour rester indépendante
des dernières retouches des contours du tigre effectuées en parallèle. Ce
résultat valide les parcours et chargeurs sur cette copie candidate ; il ne
constitue pas une validation visuelle des dernières retouches.

La suite contient 12 scénarios de promenade, 6 de croissance, 3 petits écrans,
1 contrôle de rendu de base et 6 scénarios d'intégration du tigre. Les captures
natives produites incluent les quatre âges du tigre, la maison vide pendant sa
promenade et le choix initial à 320 × 480 dp. Sur ce dernier écran, les trois
animaux et la carte entière du tigre sont accessibles et le parcours d'adoption
se termine avec l'espèce, le nom et le sexe sauvegardés.

Le gate CI décrit ci-dessus réexécute les 28 tests après la préparation des
ressources finales et avant la compilation de l'APK. Ses rapports et captures
sont l'enregistrement correspondant exactement au commit publié.

## Vérification v0.8.10

La suite comprend **52 tests** : 20 promenades, 10 scénarios de croissance,
3 petits écrans, 1 contrôle de rendu de base, 6 scénarios d’intégration du tigre
et 12 scénarios d’intégration du lion. Les 28 scénarios antérieurs sont
conservés. Les scénarios du lion exigent un sexe explicite dans les chargeurs de
sprites ; le nom de ressource attendu commence par `lion_male_<âge>_` ou
`lion_female_<âge>_`.

Le 3 octobre 2026, une copie candidate stable a permis de vérifier localement
les **52 scénarios**. La première passe a validé 40 scénarios ; les 12 scénarios
d’actions ont ensuite passé après correction d’une attente du test : le profil
fixe « Actions QA » adore la balle de tennis et déteste la peluche. Un rapport
d’objet réussi doit respecter ces goûts, y compris la baisse de joie pour la
peluche. Les sorties attendues sont maintenant explicites, avec le retour à
l’état inactif, la libération de l’objet et la progression de soin de 0,6.
Les 12 scénarios concernés ont été relancés : **0 échec, 0 erreur, 0 test ignoré**,
15,224 secondes d’exécution native. Aucun comportement de production n’a été
modifié pour cette correction du test.

Les 819 fichiers d’entrée ont conservé leurs empreintes SHA-256 pendant chaque
passe. Les seules différences entre les deux passes portent sur les assertions
du scénario partagé. Les 55 captures produites comprennent les quatre choix
initiaux, les deux portraits sexués, l’adoption à 320 × 480 dp, les huit variantes
du lion, les promenades et les six profils mixtes. Les captures du choix initial
et des deux portraits ont aussi été revues visuellement.

Cette validation locale reste celle de la **copie candidate**, antérieure aux
dernières retouches des contours de trois poses de griffoir mâles. Le gate CI
doit donc rejouer les 52 scénarios sur les ressources définitives avant fusion
et publication ; ses rapports et captures correspondent au commit publié.

## Vérification v0.8.11

La suite comprend **77 tests**, en conservant les 52 scénarios v0.8.10 :

- 28 parcours de promenade : les quatre âges des six espèces, avec les deux
  apparences du lion ;
- 14 scénarios de croissance : les trois passages d’âge à la maison et pendant
  une promenade pour chaque apparence ;
- 3 petits écrans et 1 contrôle de rendu natif de base ;
- 6 scénarios d’intégration du tigre et 12 du lion ;
- 13 scénarios d’intégration du renard et de l’ours : 8 parcours d’actions,
  2 adoptions, 1 parcours de profils mixtes et 2 naissances.

Les attentes déterministes du profil « Actions QA » restent inchangées : depuis
une joie de 30, le rapport de la balle de tennis donne 58,8, la laine 42, la
souris 44 et la peluche 22,5. Le soin progresse à 30,6 et le jeu rend l’objet puis
revient à l’état inactif. Ces attentes s’appliquent aux quatre âges des deux
nouveaux animaux, dans le salon comme dans le jardin.

La validation locale doit utiliser une copie immuable des sources et des
ressources préparées. Les résultats JUnit doivent comprendre les **77 tests de
la passe complète**, sans mélanger les XML d’une passe filtrée avec des résultats
anciens. Le gate GitHub réexécute la suite complète après la préparation des
ressources définitives et avant la compilation de l’APK ; les rapports, XML et
captures de ce run constituent la preuve correspondant au commit publié.

Le 3 octobre 2026, la passe locale complète sur une copie candidate a réussi :
**77 tests, 0 échec, 0 erreur, 0 ignoré**, en 54,986 secondes d’exécution native
(1 minute 6 secondes pour Gradle). Les 854 empreintes d’entrée contrôlées avant
et après cette passe sont inchangées. Les 106 captures comprennent les nouveaux
écrans d’adoption et les huit packs du renard et de l’ours ; 21 captures ont été
examinées visuellement : six choix initiaux avant/après défilement, choix du sexe
et confirmation des deux espèces, leurs quatre âges, leurs portraits de vieux
en promenade, le jardin vide puis leur retour et les six profils distincts.
Aucune anomalie de disposition ou de routage visuel n’a été observée dans ces
captures.

Cette passe reste une validation **candidate**, avant les dernières retouches
de pixels. Sa préparation locale par copie superposée avait conservé quatre
anciens fichiers PNG de sommeil en conflit avec les WebP préparés : ils ont été
archivés avant la passe réussie, sans changement du code ni des tests. Vingt-huit
anciens noms de ressources léopard inutilisés subsistent dans cette copie ; la
validation finale repart des dossiers historiques préparés exactement et le
gate CI recrée les ressources depuis les sources. Les résultats de cette copie
ne remplacent donc pas le gate complet sur le commit définitif.
