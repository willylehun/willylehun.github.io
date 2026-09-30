# Revue visuelle — sprites

## Sources confirmées

Les quatre planches originales 7×7 ont été retrouvées et comparées aux packs Android :

- `1000006717.png` → **Léopardeau / CUB**
- `1000006719.png` → **Ado / TEEN**
- `1000006718.png` → **Adulte / ADULT**
- `1000006720.png` → **Vieux / OLD**

La précédente annotation 8×8 était incorrecte et n'est plus utilisée.

## Mapping de la planche CUB

Numérotation ligne par ligne sur la grille 7×7 :

- humeurs face joueur : **1 à 11 + 13**
- idle : **13**
- tired : **15**
- walk_front : **23–26**
- walk_back : **27–30**
- walk_side : **31–34**
- sleep : **42**
- happy : **48**

Le nouvel atlas `leopard_cub_face_moods.webp` a été régénéré directement depuis les images 1–11 +13 de la source CUB, en 12 frames de 80×80 (960×80 au total). Son SHA-256 attendu est :

`f658bd06425abe67ab06475c8f84399db01e9230b430623f9bdde52ebe577083`

L'ancien atlas tronqué reste archivé et désactivé.

## Directions

- gauche : SIDE natif
- droite : miroir horizontal du même SIDE
- haut : BACK / dos
- bas : FRONT / face

Aucune direction ne charge un fichier d'un autre âge.


v0.5.6 réactive un atlas d'humeurs face propre à chaque âge à partir des strips HD déjà archivés dans le dépôt. Les réactions heureuses n'utilisent plus le PNG happy historique aux oreilles tronquées. Les marches restent dans leurs packs d'âge et conservent les sprites FRONT/BACK/SIDE dédiés.
