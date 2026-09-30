# Revue visuelle à terminer — ne pas déclarer les dessins validés

La planche utilisateur 1000006717.png est une grille de **7 colonnes × 7 lignes**.
L'ancienne annotation cub_annotated.png en 8×8 est erronée et ne doit pas servir au découpage.
Numérotation correcte, ligne par ligne : humeurs 1–11 +13, idle 13, tired 15,
sleep 42, happy 48, walk_front 23–26, walk_back 27–30, walk_side 31–34.
Les références originales ado/adulte/vieux ne sont pas présentes dans cette reprise.
La ressemblance entre la planche CUB fournie et certains fichiers TEEN doit être résolue
à partir des planches originales : ne pas permuter les deux packs par supposition.

Certaines oreilles sont déjà tronquées dans les images happy et side existantes.
Une marge de transparence ne reconstitue pas ces pixels. Les sprites gardent leur âge
actuellement attribué en attendant validation graphique. Aucun échange entre packs.
L'ancien atlas de portraits CUB de 80px est tronqué : 14955 octets présents pour
30542 octets annoncés par RIFF. Échec confirmé par Pillow et ImageMagick.
Il est archivé hors des ressources Android et désactivé pour éviter qu'une extraction
non vérifiée remplace le corps du léopard. Les réactions utilisent provisoirement
les poses happy/tired du même pack.

v0.5.4 corrige la mécanique de rendu, pas ces dessins manquants ou ambigus.
