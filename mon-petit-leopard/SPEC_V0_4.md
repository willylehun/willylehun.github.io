# Mon Petit Léopard — Spécification v0.4.0

## Bloc 1 — Fonctionnement final

### Cycle de vie
- Léopardeau : 1 h réelle.
- Ado : 5 h réelles.
- Adulte : 5 h réelles.
- Vieux : 2 h réelles.
- À 13 h réelles, adoption d’un nouveau léopardeau.
- Les besoins continuent d’évoluer quand l’application est fermée.

### Besoins
Six jauges visibles :
- Faim
- Eau
- Propreté
- Câlins
- Bonheur
- Sommeil

Les besoins influencent directement l’humeur et le comportement. Une émotion forte (faim, soif, fatigue, saleté, tristesse, bêtise en cours) est prioritaire sur l’humeur « content ».

### Caresser
- Le joueur caresse le léopard en touchant directement son sprite à l’écran.
- Une caresse augmente Câlins et Bonheur.
- Si aucune émotion prioritaire n’est active, le léopard devient visiblement content pendant quelques secondes.
- Si une émotion prioritaire est active, la caresse est appréciée mais ne masque pas l’émotion principale.

### Pièces
Les pièces ne sont plus affichées comme boutons permanents. Un bouton **Pièces** ouvre un menu :
- Salon
- Cuisine
- Salle de bain
- Jardin

Le changement de pièce :
- remplace le décor ;
- conserve le léopard dans la scène ;
- relance son déplacement autonome dans la nouvelle pièce ;
- adapte le menu Objets à la pièce.

### Objets
Aucun objet d’inventaire ne flotte en permanence dans le décor. Le bouton **Objets** ouvre un menu contextuel.

Cuisine :
- Eau
- Biberon
- Lait
- Croquettes junior
- Croquettes
- Pâtée
- Poulet
- Poisson
- Viande
- Biscuit
- Friandises
- Pomme
- Banane
- Pastèque
- Carotte
- Baies

Aucune rubrique « ustensiles ».

Salle de bain : uniquement
- Toilettage
- Savon
- Peigne
- Serviette

Salon :
- Balle léopard
- Balle de tennis
- Pelote de laine
- Souris jouet
- Peluche
- Corde
- Poisson jouet
- Tunnel
- Clicker
- Sifflet
- Cocon

Jardin :
- Promenade
- Anneau d’obstacle
- Canne à plume
- Griffoir
- Concours
- Repos au soleil

L’action Fetch / Rapporter la balle est supprimée du jardin.

### Menu Actions
Le bouton **Actions** ouvre :
- Punir
- Nettoyer

Punir :
- si une bêtise est réellement en cours : Obéissance progresse, léger malus Bonheur/Câlins ;
- sans bêtise : punition injuste, fort malus Bonheur/Câlins.

Nettoyer :
- disponible seulement lorsqu’une bêtise est visible ;
- active un mode nettoyage ;
- le joueur doit frotter avec le doigt directement sur la bêtise ;
- la bêtise disparaît progressivement après une distance de frottement suffisante ;
- récompense : Propreté et compétence Propreté augmentent.

### Bêtises
Une bêtise apparaît visuellement dans la scène, selon la pièce :
- cuisine : gamelle renversée, croquettes, bouteille renversée, friandises ;
- salle de bain : pipi, crotte, papier toilette, serviettes ;
- salon : canapé griffé, coussin déchiré, chaussure, pelote, livre, jouet abîmé ;
- jardin : plante déterrée, pot cassé, arrosoir, terre.

Les probabilités dépendent de l’âge, des besoins, de l’Obéissance, de la Délicatesse et de la Propreté.

## Bloc 2 — Interface portrait

Application verrouillée en **portrait**.

Ordre vertical :
1. En-tête : nom, génération, âge, temps restant, étoiles.
2. Jauges : 2 lignes de 3 jauges.
3. Compétences : Propreté / Obéissance / Délicatesse.
4. Grande scène HD occupant la majorité de l’écran.
   - décor de la pièce ;
   - léopard net ;
   - humeur ;
   - bêtise visible ;
   - consigne « frotter » pendant le nettoyage.
5. Barre inférieure :
   - Pièces
   - Objets
   - Actions

Aucun scroll requis pour le gameplay principal.

## Bloc 3 — Implémentation

- MainActivity : UI portrait, besoins, cycle de vie, déplacement autonome, caresse tactile, menu Pièces, menu Actions, geste de frottement.
- ObjectSystem : catalogue d’objets par pièce et âge, effets, menu Objets, génération des bêtises.
- Manifest : orientation portrait.
- Assets : affichage avec FIT_CENTER / CENTER_CROP, sans étirement volontaire.
- Les images de pièces et le léopard sont rendus dans un conteneur dédié afin d’éviter les déformations.
