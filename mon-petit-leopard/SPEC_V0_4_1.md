# Mon Petit Léopard — Spécification corrective v0.4.1

## Option 1 — Correctif technique appliqué

### 1. Léopard visible en permanence
- Le personnage est chargé depuis une ressource explicite selon l’âge : `leopard_cub`, `leopard_teen`, `leopard_adult`, `leopard_old`.
- Le conteneur du personnage passe de 150 dp à 230 dp.
- Les PNG source sont automatiquement recadrés sur leurs pixels visibles pendant le build : le grand espace transparent autour du personnage est supprimé.
- Le personnage recadré est redimensionné dans un canevas transparent 640 × 640 avant compilation.
- `FIT_CENTER` est conservé afin de ne jamais déformer les proportions.
- Le mouvement autonome agit sur l’ImageView complète, sans changer de ressource au milieu d’un déplacement.

### 2. Correction du flou
- Les images de jeu sont placées dans `drawable-nodpi` afin d’empêcher Android d’appliquer son propre facteur de densité.
- Les sprites sont recadrés puis préparés avec interpolation Lanczos.
- Les décors sont préparés en 1280 × 960 avec recadrage centré et léger renforcement de netteté.
- Le code n’étire plus volontairement un bitmap selon deux axes différents.
- Les décors utilisent `CENTER_CROP`; le léopard utilise `FIT_CENTER`.

> Important : cette préparation améliore fortement l’affichage des sources existantes, mais un fichier source réellement basse définition ne peut pas retrouver des détails qui n’existent pas. La cible définitive reste le remplacement progressif des sources par des exports natifs HD.

### 3. Validation obligatoire
Avant publication d’une version :
- vérifier sur téléphone portrait que le léopard est visible dans les quatre pièces ;
- vérifier les quatre âges ;
- vérifier qu’aucun personnage n’est coupé ;
- vérifier l’absence d’étirement ;
- vérifier le changement Salon/Cuisine/Bain/Jardin ;
- vérifier caresse tactile ;
- vérifier apparition d’une bêtise et nettoyage par frottement.

## Option 2 — Spécification complète v0.4.1

### Écran
- Portrait uniquement.
- En-tête compact.
- Deux lignes de trois jauges.
- Compétences.
- Grande scène principale.
- Barre inférieure : Pièces / Objets / Actions.
- Pas de boutons de pièces permanents.
- Pas d’objets d’inventaire flottants.

### Personnage
Le léopard est toujours au premier plan de la scène et se déplace de manière autonome dans la pièce choisie.

États prioritaires :
1. bêtise / inquiétude ;
2. épuisement ;
3. faim ;
4. soif ;
5. saleté ;
6. manque de câlins ;
7. tristesse ;
8. content / très heureux ;
9. calme.

Toucher directement le léopard = caresser.
- + Câlins ;
- + Bonheur ;
- si aucune émotion prioritaire : réaction « très content » ;
- une caresse ne masque jamais une émotion prioritaire.

### Pièces
Menu Pièces :
- Salon
- Cuisine
- Salle de bain
- Jardin

Le changement de pièce repositionne le léopard et relance ses déplacements.

### Objets
Menu Objets contextuel, sans affichage permanent des objets dans le décor.

Cuisine :
- Boissons : eau, biberon, lait.
- Repas : croquettes junior, croquettes, pâtée, poulet, poisson, viande.
- Friandises/snacks : biscuit, friandises, pomme, banane, pastèque, carotte, baies.
- Aucun menu Ustensiles.

Salle de bain, uniquement :
- Toilettage
- Savon
- Peigne
- Serviette

Salon :
- Jouets : balle léopard, balle de tennis, pelote, souris, peluche, corde, poisson jouet, tunnel.
- Dressage : clicker, sifflet.
- Repos : cocon.

Jardin :
- Promenade
- Anneau d’obstacle
- Canne à plume
- Griffoir
- Concours
- Repos au soleil
- Fetch / Rapporter la balle supprimé.

### Actions
Menu Actions :
- Punir
- Nettoyer

Punir avec une bêtise :
- améliore l’Obéissance ;
- léger malus Bonheur/Câlins ;
- ne supprime pas la saleté.

Punir sans bêtise :
- fort malus Bonheur/Câlins ;
- légère perte d’Obéissance.

Nettoyer :
- uniquement si une bêtise existe ;
- affiche une consigne de frottement ;
- le joueur frotte directement l’objet de la bêtise ;
- l’objet devient progressivement transparent et plus petit ;
- disparition après une distance cumulée de frottement suffisante ;
- + Propreté, + compétence Propreté, +1 étoile.

### Bêtises visibles
Cuisine :
- gamelle renversée ;
- croquettes répandues ;
- bouteille renversée ;
- friandises fouillées.

Salle de bain :
- pipi ;
- crotte ;
- papier toilette ;
- serviettes renversées.

Salon :
- canapé griffé ;
- coussin déchiré ;
- chaussure mâchouillée ;
- pelote déroulée ;
- livre abîmé ;
- jouet éventré.

Jardin :
- plante déterrée ;
- pot cassé ;
- arrosoir renversé ;
- terre partout.

### Qualité visuelle cible
Pour les futurs exports natifs :
- décors : minimum 1280 × 960, cible 1440 × 1080 ou supérieure ;
- personnage : minimum 640 × 640 par frame avec transparence réelle ;
- icône Android : master 1024 × 1024 + adaptive icon ;
- objets de menu : minimum 256 × 256 transparents ;
- pas de JPEG pour les sprites avec transparence ;
- pas de ré-échantillonnage multiple d’un même asset.

### Critère de sortie de bêta visuelle
Une version n’est validée que si :
- le léopard est visible et net dans les 4 pièces ;
- les 4 âges sont visibles ;
- les décors restent propres sur un téléphone 1080p ;
- aucun objet n’est affiché hors menu sauf une bêtise ;
- les gestes caresse et nettoyage fonctionnent au toucher ;
- l’orientation reste portrait après rotation physique du téléphone.
