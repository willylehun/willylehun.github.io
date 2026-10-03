# Vérification du renard et de l’ours brun — v0.8.11

Base publiée : `main` v0.8.10, commit
`b6123457e9c8db8c1f6113a3839bb92e5bc7982f`.
Version Android : `0.8.11`, `versionCode 56`.

## Adoption, profils et règles de jeu

Le choix initial propose six espèces : Léopard, Loup, Tigre, Lion, Renard et Ours.
Les cartes restent organisées sur deux colonnes ; une troisième rangée est ajoutée
dans le conteneur défilant existant. Les portraits de 72 dp sont conservés. Le
parcours de choix du sexe puis du nom emploie les mêmes règles et sauvegardes.
Les confirmations affichent « Adopter ce renard » et « Adopter cet ours ».
Les noms proposés sont Rouky et Balou.

Les identifiants persistants `fox` et `bear` sélectionnent quatre packs chacun :
petit, adolescent, adulte et vieux. Les deux sexes du renard et de l’ours utilisent
les mêmes planches fournies, tout en conservant leur sexe dans le profil.
Le lion et la lionne gardent leurs huit packs distincts et leur routage strict
par sexe. Aucun profil existant n’est migré ou effacé par cette intégration.

Les registres communs sélectionnent les personnages, soins, jeux, griffoirs,
portraits et jetons de promenade selon l’identité du profil actif. Les besoins,
goûts, lassitude, récompenses, durées, restrictions par âge et règles de
reproduction restent ceux du léopard. La croissance et les changements de profil
annulent les activités visuelles en cours et chargent le bon pack. Les naissances
restent limitées aux parents compatibles de la même espèce.

## Origine et préparation des images

Les seize originaux sont conservés sans modification dans
`source-assets/fox-v0811/` et `source-assets/bear-v0811/`. Les fichiers `sources.json`
conservent leurs noms d’envoi, dimensions et SHA-256 ; le validateur épingle
également leurs empreintes indépendamment des manifestes générés.

| Animal | Originaux | Packs | Ressources avec portraits | Frames |
| --- | ---: | ---: | ---: | ---: |
| Renard | 8 | 4 | 113 | 349 |
| Ours brun | 8 | 4 | 113 | 349 |
| Total | 16 | 8 | 226 | 698 |

Chaque pack de petit comprend 29 ressources ; les autres âges en comprennent 28.
Les frames utilisent le canevas de 256 × 256 px et les marges transparentes du
moteur existant. Le biberon reste réservé aux petits. Les poses uniques de corde
et de griffoir sont animées par le moteur commun ; les courses reprennent les
marches de la même espèce et du même âge à leur cadence de jeu.

Les deux sources du petit renard font 1448 × 1086 px ; les quatorze autres images
font 1536 × 1024 px. Les géométries de découpe sont définies pour chaque planche.
Le damier opaque, les chiffres et les restes du griffoir imprimé sont retirés,
avec conservation du pelage blanc, des pattes, du savon et des bulles. Les
orientations idle mal étiquetées sont corrigées ; les marches gauche et droite
conservent leurs dessins distincts. Les jetons de promenade sont recadrés sur
la tête, avec la barbe du vieil ours.

Les importeurs `prepare_v0811_fox_assets.py` et `prepare_v0811_bear_assets.py`
produisent les ressources et leurs manifestes. L’option `--check` reconstruit
les pixels et les compare sans remplacer les sorties. Les comparaisons RGBA
normalisent les RGB invisibles sous alpha nul pour ne pas confondre une
différence d’encodage lossless avec un changement de dessin.

## Validation Android candidate — 3 octobre 2026

La copie stable candidate a exécuté les 77 scénarios Robolectric API 34 avec
`GraphicsMode.NATIVE` : **77 réussis, aucun échec, aucune erreur, aucun ignoré**.
Les 854 empreintes des entrées contrôlées sont restées identiques pendant la
passe réussie. Aucun correctif du Java de production ou des assertions n’a été
nécessaire après cette exécution.

Les 52 scénarios historiques restent présents ; 25 scénarios sont ajoutés :
huit parcours d’actions, huit promenades, quatre croissances, deux adoptions,
deux reproductions et un contrôle des six profils. Les adoptions à 320 × 480
utilisent de vrais événements de défilement et de toucher. La revue visuelle
a examiné 21 des 106 captures produites : choix initial, sexe et nom, quatre âges,
portraits de promenade, jardin vide pendant la sortie, retour et profils.

Cette candidate précède les dernières retouches de pixels. Son assemblage local
a aussi conservé 28 anciens noms léopard inutilisés, correctement refusés par les
deux contrôles d’inventaire historique. Quatre doublons PNG/WebP de sommeil
avaient été retirés avant la passe native réussie. Ces erreurs d’assemblage de
la copie locale ne modifient pas le dépôt ni les importeurs ; la validation finale
doit employer l’inventaire préparé exact. Aucune assertion n’est affaiblie.

Le workflow rejoue obligatoirement les 77 scénarios sur les ressources finales
du commit avant de compiler et publier l’APK. Ces scénarios ne constituent pas
un essai sur un téléphone physique.

## Contrats de comportement

Les onze suites de contrats et les six tests du writer passent. Huit suites ont
été exécutées sur la candidate dont le Java est identique au code final. Les deux
contrôles d’inventaire léopard et le validateur renard/ours ont ensuite réussi
sur la copie finale, avec les dossiers historiques préparés copiés exactement.

| Vérification | Couverture réussie sur la candidate |
| --- | ---: |
| Effets des objets comparés au léopard | 26 928 cas |
| Routes d’actions selon profil, âge et répétition | 8 976 cas |
| Ressources de jeu isolées | 763 dans 28 packs |
| Reproduction | 150 paires d’âges, 30 couples interespèces refusés |
| Promenade selon espèce, sexe et âge | 48 cas |
| Visibilité, retour et changements de cache | 2 516 assertions |
| Croissance | 36 transitions, 240 clics après l’âge maximum |

## Validation des ressources finales et de l’APK

Les deux importeurs terminent leur reconstruction `--check` avec un code de
retour nul. Le validateur final confirme les seize originaux, les 226 ressources,
les 698 cadres et les 246 sondes alpha : 78 pour le renard, 168 pour l’ours.
Les quatre âges, directions, marges et provenances sont vérifiés, sans
substitution d’une autre espèce ou d’un autre âge.

La copie finale contient 799 images drawable et les inventaires historiques
exacts, sans les anciens noms supplémentaires de la candidate. Ses 1 022
empreintes restent identiques avant et après les contrôles et la compilation.
`assembleDebug` réussit en 14 secondes.

L’APK local inspecté porte `com.byw.monpetitleopard`, version `0.8.11`, code `56`,
SDK minimum `26` et cible `35`. Sa taille est de **93 757 511 octets**. Les 799
drawables et l’icône launcher sont embarqués, avec exactement 113 fichiers renard
et 113 fichiers ours, répartis 29/28/28/28 par animal. L’intégrité ZIP et la
vérification de signature réussissent.

SHA-256 de cet APK local :
`247406226b40fbe1048df9ddab6adf2078feaac6fab54f3526d93ad77e2ff2b4`.
L’APK publié sera construit et signé par la CI ; son empreinte devra donc être
contrôlée séparément après publication.

## Publication et conservation des téléchargements

La base contient 43 APK historiques et courant, totalisant 782 968 841 octets.
Publier aussi toutes les sources Android et les planches avec les nouveaux APK
ferait dépasser la limite de taille du site GitHub Pages. Le dépôt conserve
toutes ces sources ; seul le contenu livré par Pages est filtré.

Le marqueur vide `.nojekyll` est remplacé par `_config.yml`. La configuration
exclut du site les dossiers internes Android et conserve la page de téléchargement,
`.well-known/assetlinks.json` et tous les APK de `downloads/`. Elle désactive les
transformations optionnelles des documents en pages et le thème par défaut.
Le mode de publication et les réglages d’administration Pages ne changent pas.

Après compilation et préparation des deux noms de l’APK courant, la CI construit
réellement le site avec `actions/jekyll-build-pages@v1`. Le nouveau validateur
`validate_pages_output.py` compare les octets de la page, des associations Android
et de tous les APK publiés à leurs sources, exige un inventaire identique, vérifie
que les deux téléchargements courants sont l’APK compilé et mesure la taille
réelle du site. La publication est conditionnée à un site inférieur à
1 000 000 000 octets et à un APK d’au plus 104 857 600 octets.

Les résultats sont conservés dans les artefacts CI
`mon-petit-leopard-v0.8.11-android-tests` et
`mon-petit-leopard-v0.8.11-pages-verification`. La construction Jekyll n’a pas été
simulée localement : son résultat réel doit être contrôlé sur la PR avant fusion.

Références de publication :
[limites GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits),
[configuration Jekyll sur Pages](https://docs.github.com/en/pages/setting-up-a-github-pages-site-with-jekyll/about-github-pages-and-jekyll),
[action de construction officielle](https://github.com/actions/jekyll-build-pages).
