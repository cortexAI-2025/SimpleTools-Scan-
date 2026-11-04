# Recette SimpleTools Scan V1 sur Samsung Galaxy A14 (≈ 15 min)

But : vérifier sur un vrai téléphone ce que l’émulateur ne peut pas prouver (vraie caméra,
mise au point, lumière réelle, performances avec de grandes photos).

## 0. Installation

1. Télécharger l’artefact `simpletools-scan-debug-apk` du dernier run CI de la PR, le dézipper.
2. Sur le A14 : *Paramètres > Sécurité > Installer des applis inconnues* → autoriser le navigateur / Mes fichiers.
3. Ouvrir `app-debug.apk` → Installer. L’app s’appelle **SimpleTools Scan** (id `com.simpletools.scan.debug`).
4. Préparer 3 feuilles imprimées (dont une avec du petit texte) et une table de couleur sombre.

## 1. Mode avion + refus de la caméra

1. Activer le **mode avion** (et couper le Wi-Fi s’il reste allumé).
2. Ouvrir l’app → **Numériser une page** → **Autoriser la caméra** → répondre **Refuser**.
   - Attendu : message « L’accès à la caméra a été refusé… », boutons *Autoriser la caméra* et *Importer une photo*.
3. Re-taper **Autoriser la caméra** → **Refuser** encore.
   - Attendu : « L’accès à la caméra est désactivé… » + **Ouvrir les paramètres**.
4. Taper **Ouvrir les paramètres** → *Autorisations > Appareil photo > Autoriser uniquement si l’appli est en cours d’utilisation* → revenir.
   - Attendu : l’aperçu caméra s’affiche sans relancer l’app.

## 2. Trois pages, recadrage, rotation, ordre

1. **Page 1** : poser la feuille de travers sur la table, cadrer, appuyer sur le déclencheur (toucher l’écran pour la mise au point si besoin).
2. Écran *Ajuster les coins* : faire glisser les 4 coins sur les coins de la feuille → **Aperçu**.
   - Attendu : la page apparaît redressée (bords parallèles), texte lisible.
3. **Ajouter au document**. Recommencer pour les pages 2 et 3 (**Ajouter une page**).
4. Pour la page 3, dans l’aperçu, taper **Pivoter** une fois avant **Ajouter au document** (photographiez-la de côté pour que la rotation la remette droite).
5. Sur la liste : **Monter la page 3** deux fois → ordre attendu : 3, 1, 2.
6. Essayer aussi **Reprendre la page** sur une page (nouvelle photo à la même position) et **Supprimer** puis la refaire si besoin.

## 3. Reprise après fermeture

1. **Ajouter une page**, prendre une photo, bouger un coin, puis **ne pas** valider.
2. Fermer l’app depuis les applications récentes (glisser vers le haut).
3. Rouvrir l’app.
   - Attendu : les pages déjà ajoutées sont là, dans le même ordre, et une carte « Page non terminée » propose **Reprendre** : le coin déplacé est conservé.
4. **Reprendre** → terminer ou **Abandonner**.

## 4. Export PDF

1. **Exporter en PDF** → changer le nom (ex. « Test A14 été ») → **Enregistrer sur l’appareil**.
2. Dans le sélecteur Android, taper **retour** sans choisir de dossier.
   - Attendu : « PDF non enregistré. Vos pages sont toujours là. »
3. Recommencer et choisir *Téléchargements* → **Enregistrer**.
   - Attendu : « PDF enregistré : Test A14 été.pdf ».
4. Ouvrir le PDF depuis *Mes fichiers* (ou **Partager** / **Ouvrir dans SimpleTools PDF** si installé).
   - Vérifier : **3 pages**, **ordre 3-1-2**, la page tournée est droite, le petit texte est **lisible** en zoomant.

## 5. À noter dans le retour

- Modèle / version One UI, temps approximatif entre déclencheur et écran de recadrage, temps d’export.
- Netteté du texte, éventuel flou de mise au point, comportement avec le flash (icône éclair).
- Tout écran figé, message d’erreur ou fermeture inattendue (avec l’étape).
