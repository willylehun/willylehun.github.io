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
gradle -p mon-petit-leopard/qa/android-render-tests --no-daemon :app:testDebugUnitTest
```

Les scripts de préparation ont besoin de Pillow et NumPy, comme le workflow
Android principal. Le SDK peut être indiqué avec `ANDROID_HOME` ou le fichier
local `local.properties` de ce projet de test.

Le harness lit `../../app/src/main` par défaut. Pour comparer un autre checkout
préparé, passer `-PqaSourceRoot=/chemin/absolu/mon-petit-leopard` et lancer
`:app:clean` avant de changer cette source. Le harness actuel utilise les trois
espèces et nécessite les sources v0.8.9 ou suivantes. Pour comparer une version
antérieure, utiliser le harness du commit correspondant dans une copie isolée :
celui de v0.8.8 ne référence pas encore le tigre. Pour reproduire la régression
v0.8.7 avec le harness v0.8.8, conserver seulement ses suites
`PromenadeLifecycleGraphicsTest.java` et `NativeGraphicsSmokeTest.java`.

Rapports et captures sont produits sous `app/build/` et exclus du dépôt :

- `reports/tests/testDebugUnitTest/index.html` : rapport lisible ;
- `test-results/testDebugUnitTest/` : résultats JUnit XML ;
- `native-captures/` : captures des quatre âges, de la maison vide pendant la
  promenade et des petits écrans.

## Couverture

| Suite | Vérification réelle |
| --- | --- |
| `NativeGraphicsSmokeTest` | Décodage d'un sprite avec alpha, dessin d'un `ImageView` sur un fond opaque, conservation des coins transparents. |
| `PromenadeLifecycleGraphicsTest` | Les trois espèces aux quatre âges : départ, pause/arrêt, activité promenade, portrait de la bonne espèce et du bon âge, clic sur Retour au jardin, reprise anticipée, mise en page, animation, recréation de l'activité et échéance. |
| `GrowthButtonGraphicsTest` | Clic sur le bouton Dev pour les trois passages d'âge, nouvelle image, annulation du soin et de l'appel en cours, maintien du sommeil, besoins et compétences conservés, espèce et âge persistés après recréation, bouton désactivé au stade vieux. Les trois clics sont aussi testés pendant une promenade, pour les trois espèces. |
| `CompactGrowthLayoutTest` | Bouton Dev et trois commandes du bas entièrement visibles et utilisables à 360 × 640, 320 × 568 et 320 × 480 dp. |
| `TigerIntegrationGraphicsTest` | Aux quatre âges : poses, six images de marche et de course dans chaque direction, manger/sauter/dormir, douze humeurs, quatre soins et disponibilité du biberon. Utilisation réelle des objets : repas, quatre jouets rapportés et corde dans le salon et le jardin, griffoir et repos. Adoption initiale complète à 320 × 480 dp puis réouverture ; descendance tigre avec conservation de l'espèce, des parents et des profils léopard/loup existants. |

Pour la maison vide, le test dessine la scène complète avec son état réel, puis
avec l'animal explicitement masqué. Pendant la promenade, **aucun pixel ne doit
différer** entre les deux rendus. Avant le départ et après l'échéance, l'animal
doit au contraire modifier plus de 100 pixels du décor. Cela vérifie le résultat
composé, en plus de la visibilité du `View`.

Le fond transparent des images affichées est également composé sur deux couleurs
contrastées par le `Canvas` Android natif, avant le départ et après le retour.

Les scénarios d'actions du tigre comparent les pixels du bitmap effectivement
affiché à la frame attendue de son propre pack. Chaque image est ensuite
composée avec le `Canvas` natif pour vérifier qu'elle peint un animal visible et
conserve sa marge transparente. Les jeux utilisent les véritables contrôleurs
de l'application et leurs transitions ; Robolectric pilote les arrivées et la
fin du lancer sans attendre les délais en temps réel. L'adoption utilise les
clics sur la carte du tigre, le sexe et le bouton de confirmation, ainsi que le
champ du nom. Le test fait défiler la sélection si nécessaire et vérifie que la
carte entière reste accessible sur le petit écran.

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
