# Vérification du tigre — v0.8.9

Base : `main` v0.8.8, commit `30f339e64a695ca89955947bd7d4b1be6440e897`.
Version Android : `0.8.9`, `versionCode 54`.

## Fonctionnement

Le tigre devient le troisième choix d'adoption, avec son sexe, son nom et sa
sauvegarde indépendante. Les quatre registres de sprites utilisent exclusivement
les ressources `tiger_cub`, `tiger_teen`, `tiger_adult` et `tiger_old`.

Les besoins, goûts, effets d'objets, restrictions par âge, jeux, sommeil, promenade
et reproduction empruntent les règles communes existantes. Les sauvegardes
léopard et loup conservent leur identité et leurs données. Le bouton Dev de la
v0.8.8 actualise aussi l'apparence du tigre à chaque changement de stade.

## Sources et préparation visuelle

Les huit originaux sont conservés dans `source-assets/tiger-v089/`, avec leur nom
d'origine et leur empreinte SHA-256. Les sources de chaque âge sont indépendantes.

| Âge | Planche mouvements/humeurs | Planche objets |
| --- | --- | --- |
| Tigreau | `11_28_01-1(1).png` | `11_28_02-2(1).png` |
| Ado | `11_28_08-1(1).png` | `11_28_09-2(1).png` |
| Adulte | `13_06_44-1(1).png` | `13_06_45-2(1).png` |
| Vieux | `13_06_52-1(1).png` | `13_06_53-2(1).png` |

Ces planches de 1536 × 1024 contiennent un damier opaque. L'importeur prépare
109 ressources de jeu et quatre portraits, soit 349 frames de 256 × 256 avec
16 pixels de marge transparente. Les quatre planches de contrôle ont été revues.

Corrections de préparation : orientation des poses idle gauche, chiffres imprimés
touchant certaines pattes, retrait du griffoir fixe, protection des joues blanches
du vieux tigre et conservation des bulles de soin. Les portraits ne gardent que
la tête. Les poses de corde et de griffoir fournies sont uniques ; le moteur
existant anime ces interactions comme pour le loup.

`prepare_v089_tiger_assets.py --check` a reconstruit les 113 ressources et le
manifeste sans les modifier : identité des octets confirmée. Les 26 sondes alpha
du manifeste vérifient le retrait du fond et la préservation du pelage, des pattes
et des objets aux points audités.

## Validation locale — 3 octobre 2026

Les neuf scripts de la chaîne de contrats passent sur une copie stable des sources
et ressources finales, préparée hors du checkout. Aucun ancien PNG de bootstrap
ne doit rester à côté du WebP runtime du même nom après préparation.

| Vérification | Résultat |
| --- | ---: |
| Effets d'objets et répétitions comparés au léopard | 4 896 cas |
| Routages d'actions selon l'âge et la répétition | 1 632 cas |
| Visibilité pendant la promenade | 804 assertions |
| Ressources de jeu des trois espèces | 327 ressources isolées |
| Reproduction | 75 couples d'âges, six couples interespèces refusés |
| Croissance de six profils mixtes | 18 transitions, 120 clics supplémentaires bornés |
| Validation des packs tigre | Huit originaux, 113 fichiers, 349 frames, 26 sondes |

La compilation Android finale `assembleDebug` a réussi. L'APK vérifié porte
`com.byw.monpetitleopard`, version `0.8.9`, code `54`, et contient les 113 ressources
tigre attendues : 29 pour le petit et 28 pour chacun des trois autres âges,
portraits compris.

La première exécution du harness Android a passé **28 tests, zéro échec et zéro
test ignoré**, sur une copie cohérente précédant les dernières retouches de
contour. Elle couvre l'adoption à 320 × 480, la réouverture, les actions, les
quatre âges, les trois espèces en promenade et la croissance. Cette première
exécution n'est pas présentée comme une revue des contours définitifs.

Le workflow de publication exécute maintenant ces 28 tests sur les ressources
finales du commit, après la préparation et les neuf validations. Les rapports
et captures sont conservés dans l'artefact `mon-petit-leopard-v0.8.9-android-tests`.
Le build APK et la publication n'interviennent qu'après la réussite des contrôles.

Les tests de rendu utilisent Robolectric API 34 et `GraphicsMode.NATIVE` avec
les classes et ressources Android compilées. Ils ne remplacent pas un essai sur
un téléphone physique.
