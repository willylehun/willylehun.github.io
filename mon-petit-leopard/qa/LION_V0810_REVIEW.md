# Vérification du lion et de la lionne — v0.8.10

Base publiée : `main` v0.8.9, commit `4f5ef95fab8efb14c7c02e560aaecbff023d8fd0`.
Version Android : `0.8.10`, `versionCode 55`.

## Fonctionnement et identité du profil

Le lion devient la quatrième espèce disponible à l'adoption. Le choix initial
présente les quatre animaux en grille 2 × 2. L'étape suivante montre les deux
portraits du lion, puis le nom et le portrait choisis. Les noms proposés sont
Simba pour le mâle et Nala pour la femelle.

Les deux sexes partagent l'identifiant d'espèce `lion`. Le sexe enregistré
(`male` ou `female`) sélectionne l'un des huit packs : deux sexes × quatre âges.
Les registres de personnages, de soins, de jeux et de jardin reçoivent l'espèce,
le sexe et l'âge. Les portraits des profils et de promenade suivent la même
identité. Une requête de sprites lion sans sexe valide est refusée.

Les règles communes du léopard restent utilisées pour les besoins, goûts,
répétitions, objets, actions, jeux, sommeil, promenades et restrictions par âge.
Les six emplacements et leurs sauvegardes restent partagés entre les espèces.
La reproduction d'un lion et d'une lionne compatibles crée un petit de l'espèce
`lion`, avec un sexe enregistré et conservé lors de la croissance. Les naissances
entre espèces différentes restent refusées.

Les changements d'apparence annulent les activités visuelles et vident les
images précédentes, y compris lorsque le sexe change à âge identique. Les
correctifs d'absence pendant une promenade et le bouton Dev de croissance sont
conservés pour les quatre espèces.

## Origine et préparation des sprites

Les seize PNG sont conservés sans modification dans `source-assets/lion-v0810/`.
Le fichier `sources.json` associe chaque original à son sexe, son âge et son rôle,
avec son nom d'origine et son empreinte SHA-256. Les empreintes sont également
fixées indépendamment dans le validateur.

| Apparence | Planches sources | Packs | Ressources, portraits compris | Frames |
| --- | ---: | ---: | ---: | ---: |
| Lion mâle | 8 | 4 | 113 | 349 |
| Lionne | 8 | 4 | 113 | 349 |
| Total | 16 | 8 | 226 | 698 |

Les cadres font 256 × 256 px avec 16 px de marge transparente. Chaque petit
possède 29 ressources, et chacun des autres âges 28. Le biberon reste réservé
aux petits. Les poses de corde et de griffoir sont les poses uniques fournies ;
le moteur commun assure leur mouvement. La course reprend les séquences de
marche du même sexe et du même âge, jouées à une cadence différente.

Les planches de la lionne petite et adulte font 1448 × 1086 px ; les autres
1536 × 1024 px. Les objets du lion adolescent se trouvent dans la partie haute
de sa seconde planche. L'importeur utilise des coordonnées propres à chaque
source au lieu de supposer une grille commune.

Le damier opaque, ses zones bleutées, les cadres et les chiffres imprimés sont
retirés. Des masques locaux préservent les pattes, les crinières, les yeux, les
bulles et les objets. Le griffoir imprimé est séparé du personnage, car le jeu
possède déjà son griffoir fixe. Les huit jetons de promenade ne gardent que la tête.
Les poses idle gauche mal étiquetées sont réorientées ; les marches gauche et
droite conservent leurs séquences distinctes.

La vieille lionne a une longue mèche dans la planche des mouvements et une
mèche courte dans celle des objets. Les dessins d'origine sont conservés pour
chaque action ; cette différence ne correspond pas à un échange de sexe ou d'âge.

Le manifeste relie chaque sortie à sa source, sa découpe, ses masques et ses
empreintes. `prepare_v0810_lion_assets.py --check` compare les pixels RGBA décodés
sans remplacer les fichiers, en normalisant les RGB invisibles sous alpha nul.
Une différence d'encodage lossless entre versions de Pillow ou de WebP ne doit
pas être confondue avec un changement d'apparence.

## Contrôles de la chaîne de validation

Les dix scripts de contrats couvrent les quatre espèces, les 20 apparences
et les 545 ressources de jeu. Le test historique `test_wolf_behavior.py` vérifie
aussi les deux sexes du lion, les sauvegardes mixtes et la reproduction.
Les six tests ciblés du writer de sprites tigre restent dans la chaîne.

Les 52 scénarios Android utilisent Robolectric API 34 et `GraphicsMode.NATIVE`
avec les classes et ressources Android compilées. Ils couvrent notamment les
gestes réels d'adoption à 320 × 480, les deux aperçus de sexe, les huit parcours
d'actions et de promenade lion, les six profils, la reproduction et la croissance.
Ils ne constituent pas un essai sur un téléphone physique.

Les rapports HTML, les résultats JUnit XML et les captures sont prévus dans
l'artefact CI `mon-petit-leopard-v0.8.10-android-tests`. Le build APK et la
publication sont conditionnés à la réussite des contrôles.

La page `index.html` pointe vers les téléchargements v0.8.10 et présente les
quatre animaux, y compris le lion et la lionne.

## Validation locale — 3 octobre 2026

Les huit apparences ont été revues sur fond sombre ; les packs mâles ont aussi
été revus sur fond clair. Une dernière comparaison ciblée confirme la suppression
des fragments de poteau et des restes de sol, avec conservation des vraies pattes.
Les 97 sondes alpha passent : 43 pour les mâles, 54 pour les femelles.
La reconstruction `--check` des 226 ressources finales réussit.

Les dix suites de contrats et les six tests du writer réussissent sur la copie
finale. Les empreintes vérifient l'absence de mutation des fichiers pendant les
exécutions. L'assertion historique qui cherchait un libellé d'animal directement
dans l'activité a été adaptée à `PetSpecies.label()` et vérifie les quatre cartes.

| Vérification | Résultat |
| --- | ---: |
| Effets d'objets comparés aux règles du léopard | 17 136 cas |
| Routages d'actions selon espèce, sexe, âge et répétition | 5 712 cas |
| Visibilité et retour de promenade | 1 500 assertions |
| Ressources de jeu isolées | 545 ressources dans 20 packs |
| Reproduction | 100 paires d'âges, 12 paires interespèces refusées |
| Croissance de six profils mixtes | 18 transitions, 120 clics supplémentaires bornés |
| Lion et lionne | 16 originaux, 226 fichiers, 698 frames, 97 sondes |

La compilation `assembleDebug` réussit sur la copie finale stable hors git.
L'APK vérifié porte `com.byw.monpetitleopard`, version `0.8.10`, code `55`,
SDK minimum 26 et cible 35. Il contient exactement les 226 ressources lion :
29 pour chaque petit et 28 pour chacun des six autres packs, portraits compris.

Les **52 scénarios Android sont couverts localement** sur une copie immuable
précédant les dernières retouches de contour : 40 scénarios inchangés réussis,
puis les 12 scénarios d'actions réussis après correction de leur fixture.
Aucun code de production n'a changé entre ces deux exécutions. Les assertions
et les captures confirment notamment que les quatre cartes et les deux aperçus
de sexe sont entièrement visibles à 320 × 480, puis que de vrais gestes tactiles
permettent l'adoption et affichent le portrait choisi.

Le nom de test « Actions QA » adore le tennis et déteste la peluche, contrairement
à l'ancienne fixture « Tigre QA ». Le test corrige donc son attente de joie,
sans changer les règles du jeu : depuis 30, les rapports donnent exactement
58,8 pour le tennis, 42 pour la pelote, 44 pour la souris et 22,5 pour la peluche.
Il exige aussi la fin du retour, l'absence d'objet actif et la compétence à 30,6.
Les 12 scénarios corrigés terminent sans échec, erreur ou test ignoré.

Cette passe candidate ne constitue pas une validation Android des toutes dernières
retouches de pixels. Le workflow rejoue obligatoirement les 52 tests sur les
ressources finales du commit avant de construire et publier l'APK.
