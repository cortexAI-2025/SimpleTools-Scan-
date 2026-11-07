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

## 6. Contours, filtres (gratuit)

1. Photographier une feuille posée sur une table sombre, puis sur une table claire.
   - Attendu : les 4 coins sont placés sur la feuille dès l'écran d'ajustement. Si aucune page n'est trouvée,
     le message « Aucune page détectée » apparaît et les coins par défaut restent.
2. Taper **Détecter les contours** après avoir déplacé les coins.
3. Dans l'aperçu : **Couleur**, **Gris**, **N&B** → chaque aperçu correspond à la page enregistrée.

## 7. PRO : reconnaissance, PDF recherchable, contraste (achat requis)

Les étapes 7.2 à 7.5 demandent une licence PRO active. Sans achat réel (produit non encore configuré), vérifier
seulement que les écrans affichent l'offre et que rien n'est débloqué.

1. **Mode avion activé**, sans achat : taper l'icône *Texte reconnu (PRO)* → l'écran PRO s'ouvre ;
   le bouton *Débloquer PRO* est désactivé et le prix indique « non disponible ».
2. Après achat de test (testeur de licence, piste de test interne) : *Texte reconnu* → **Reconnaître le texte**.
   - Attendu : la progression « Page n sur N » avance, puis le texte apparaît, **avec le mode avion**.
3. Rechercher un mot présent sur une page (ex. « facture », sans accent) → la page et la ligne apparaissent.
4. **Copier le texte** → coller dans une note : le texte est identique.
5. **Exporter en TXT** puis **PDF recherchable** → ouvrir le PDF dans *Mes fichiers* : le texte se sélectionne
   et se recherche (Ctrl+F / recherche du lecteur), la page reste identique à la photo.
6. Réglage du **Contraste** (curseur) → l'aperçu change ; après enregistrement, la page a le même contraste.
7. Redémarrer l'app en mode avion → l'état « Licence active, vérifiée précédemment » reste affiché.
8. **Restaurer mes achats** après réinstallation avec le même compte Google → PRO revient.
9. Vérifier que *Partager* et *Enregistrer* (sans PRO) fonctionnent toujours sans achat.

## 8. À noter en plus

- Temps de reconnaissance par page, consommation mémoire perçue, chauffe.
- **Confidentialité** : pendant l'OCR, aucune connexion ne doit apparaître (vérifier avec un proxy ou
  *Utilisation des données* si possible) ; à consigner dans la fiche Data safety.
