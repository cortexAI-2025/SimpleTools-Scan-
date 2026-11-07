# Google Play Console : configuration de SimpleTools Scan PRO

Ce document liste ce qui doit exister dans la Play Console **avant** qu'un achat réel soit possible.
Tant que ces étapes ne sont pas faites, l'application affiche « Prix non disponible » et le bouton
« Débloquer PRO » reste désactivé. Aucune licence n'est simulée (voir `docs/MONETIZATION.md`).

## 1. Produit intégré (achat unique, non consommable)

| Champ | Valeur |
|-------|--------|
| Menu | *Monétisation > Produits > Produits intégrés* → **Créer un produit** |
| ID du produit | `simpletools_scan_pro_lifetime` (identique à `AppContainer.PRO_PRODUCT_ID` : ne pas le changer seul) |
| Nom | SimpleTools Scan PRO |
| Description (FR) | Débloque la reconnaissance du texte (OCR) hors ligne, le PDF recherchable, la recherche et l'export TXT, et le réglage du contraste. Achat unique, sans abonnement. |
| Description (EN) | Unlocks offline text recognition (OCR), searchable PDF, text search and TXT export, and contrast adjustment. One-time purchase, no subscription. |
| Prix | **3,99 € TTC** pour la France (prix de base en EUR). Les autres pays sont calculés par Play ; l'application affiche le prix renvoyé par Play. |
| Statut | **Actif** (un produit inactif n'est pas vendu) |

Le type de produit doit être **produit intégré ponctuel** (non consommable) : c'est ce que l'application
interroge (`ProductType.INAPP`). Ne pas le créer comme abonnement.

## 2. Tester les achats (sans payer)

1. *Paramètres de test > Testeurs de licence* : ajouter le compte Google de test (et non le compte personnel
   du développeur si possible).
2. Les achats de test ne sont **pas facturés** pour les testeurs de licence.
3. Google Play **ne livre les achats qu'à une application installée par Play** : publier d'abord l'AAB
   signé sur la piste **Test interne**, puis installer l'application depuis le lien de test sur le
   Galaxy A14. Un APK de debug installé depuis GitHub ne peut pas acheter.
4. Vérifier, sur le téléphone : achat → PRO débloqué ; redémarrage avec le mode avion → PRO toujours
   actif (licence mémorisée) ; *Restaurer mes achats* après réinstallation.

## 3. Formulaire « Sécurité des données » (Data safety)

Déclaration à remplir à partir du comportement réel de la version (voir `docs/PERMISSIONS.md`) :

- **Données collectées : aucune.** Les photos et documents restent sur l'appareil.
- **Données partagées : aucune.**
- Chiffrement en transit : sans objet (aucune connexion réseau pour l'application).
- Suppression : les documents sont supprimés par l'utilisateur dans l'application ou en désinstallant.
- Achats : traités par Google Play ; l'application ne reçoit que l'état de la licence.
- **À vérifier avant publication** : l'OCR utilise ML Kit avec le modèle inclus dans l'application. La
  permission `INTERNET` est retirée du manifeste (voir `app/src/main/AndroidManifest.xml`) ; il faut
  confirmer sur le téléphone, en mode avion, que la reconnaissance fonctionne, puis documenter le résultat.

## 4. Politique de confidentialité

- Renseigner `privacy.policyUrl` dans `simpletools.properties` (obligatoire pour la publication). L'URL doit
  décrire le traitement réel : traitement local, aucun envoi, achat géré par Google Play.

## 5. Notes de version

Le texte FR/EN est dans `docs/RELEASE_NOTES.md`. Le copier dans *Fiche du Store > Nouveautés*, pour chaque
langue, lors de la mise en production.

## 6. Ce qui bloque la vente réelle

| Élément | État attendu | Conséquence si absent |
|---------|--------------|------------------------|
| Produit `simpletools_scan_pro_lifetime` actif | requis | prix indisponible, achat désactivé |
| Application publiée sur une piste (interne au minimum) | requis | Play ne livre pas les achats |
| AAB signé (clé d'upload, voir `docs/RELEASE.md`) | requis | pas de publication |
| URL de politique de confidentialité | requis | refus de publication |
| Formulaire Data safety rempli | requis | refus de publication |
| Test de l'achat sur le Galaxy A14 | requis avant production | non vérifié |

Ces éléments dépendent du compte Play Console du propriétaire : ils ne peuvent pas être créés depuis le dépôt.
