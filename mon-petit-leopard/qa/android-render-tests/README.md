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

Prérequis : Java 17, Gradle 8.9, Android SDK 35 et Build Tools 35.0.0. Le premier
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
gradle -p mon-petit-leopard/qa/android-render-tests --no-daemon :app:testDebugUnitTest
```

Les scripts de préparation ont besoin de Pillow et NumPy, comme le workflow
Android principal. Le SDK peut être indiqué avec `ANDROID_HOME` ou le fichier
local `local.properties` de ce projet de test.

Le harness lit `../../app/src/main` par défaut. Pour comparer un autre checkout
préparé, passer `-PqaSourceRoot=/chemin/absolu/mon-petit-leopard` et lancer
`:app:clean` avant de changer cette source. Les tests du bouton développeur
nécessitent les sources v0.8.8 ou suivantes. Pour une régression sur v0.8.7,
conserver seulement `PromenadeLifecycleGraphicsTest.java` et
`NativeGraphicsSmokeTest.java` dans une copie isolée du harness.

Rapports et captures sont produits sous `app/build/` et exclus du dépôt :

- `reports/tests/testDebugUnitTest/index.html` : rapport lisible ;
- `test-results/testDebugUnitTest/` : résultats JUnit XML ;
- `native-captures/` : captures des quatre âges, de la maison vide pendant la
  promenade et des petits écrans.

## Couverture

| Suite | Vérification réelle |
| --- | --- |
| `NativeGraphicsSmokeTest` | Décodage d'un sprite avec alpha, dessin d'un `ImageView` sur un fond opaque, conservation des coins transparents. |
| `PromenadeLifecycleGraphicsTest` | Les deux espèces aux quatre âges : départ, pause/arrêt, activité promenade, clic sur Retour au jardin, reprise anticipée, mise en page, animation, recréation de l'activité et échéance. |
| `GrowthButtonGraphicsTest` | Clic sur le bouton Dev pour les trois passages d'âge, nouvelle image, annulation du soin et de l'appel en cours, maintien du sommeil, besoins et compétences conservés, persistance après recréation, bouton désactivé au stade vieux. Les trois clics sont aussi testés pendant une promenade, pour les deux espèces. |
| `CompactGrowthLayoutTest` | Bouton Dev et trois commandes du bas entièrement visibles et utilisables à 360 × 640, 320 × 568 et 320 × 480 dp. |

Pour la maison vide, le test dessine la scène complète avec son état réel, puis
avec l'animal explicitement masqué. Pendant la promenade, **aucun pixel ne doit
différer** entre les deux rendus. Avant le départ et après l'échéance, l'animal
doit au contraire modifier plus de 100 pixels du décor. Cela vérifie le résultat
composé, en plus de la visibilité du `View`.

Le fond transparent des images affichées est également composé sur deux couleurs
contrastées par le `Canvas` Android natif, avant le départ et après le retour.

## Régression reproduite

Le 3 octobre 2026, les huit scénarios de promenade échouaient sur la v0.8.7 au
retour anticipé et après la mise à jour de position : selon l'espèce et l'âge,
4 181 à 7 403 pixels de l'animal apparaissaient dans la maison alors que la
promenade restait active. Les mêmes scénarios passent avec la garde commune de
visibilité v0.8.8. Les bitmaps conservaient leur canal alpha ; le défaut provenait
bien de la remise en visibilité du personnage dans la maison.

Ce harness ne modifie pas `app/build.gradle` et n'ajoute pas de dépendance à l'APK
publié. Les régressions rapides de `tools/test_promenade_visibility.py` et
`tools/test_dev_growth.py` restent les contrôles du workflow de publication.

## Résultat v0.8.8

Le harness complet a été exécuté avec succès le 3 octobre 2026 : **16 tests,
0 échec, 0 test ignoré**. Les huit reprises de promenade, les quatre scénarios
de croissance, les trois petits écrans et le décodage/compositing natif passent.
À 320 × 480 dp, la scène mesure 251 dp ; la barre de commandes garde ses 46 dp
et l'espace du bas ses 12 dp. Les quatre boutons restent entièrement visibles.

Deux captures natives ont été conservées hors du dossier de build :

- [`../v088-developer-growth.png`](../v088-developer-growth.png) : louveteau et bouton Dev ;
- [`../v088-promenade-home-empty.png`](../v088-promenade-home-empty.png) : maison vide pendant une promenade, après les trois passages d'âge.
