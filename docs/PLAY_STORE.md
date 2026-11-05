# SimpleTools Scan 1.0.0 — AAB signé et mise sur le Play Store

L'AAB de release est déjà construit et minifié à chaque PR (artefact `simpletools-scan-release-aab-unsigned`),
mais **il n'est pas signé** : le Play Console le refuse. La signature se fait avec *votre* clé d'envoi
(upload key), créée une fois sur votre ordinateur et jamais versionnée.

## 1. Créer la clé d'envoi (une fois, sur votre ordinateur)

```bash
keytool -genkeypair -v -keystore simpletools-scan-upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Gardez le fichier `.jks` et les deux mots de passe dans un gestionnaire de mots de passe.
Avec *Play App Signing* (activé par défaut), Google garde la clé de signature de l'application ;
la clé d'envoi peut être réinitialisée par le support Google si elle est perdue.

## 2. Donner la clé à GitHub Actions (dépôt SimpleTools-Scan-)

1. *Settings → Environments → New environment* : `release`.
2. Dans cet environnement, *Add environment secret* :

   | Secret | Valeur |
   |---|---|
   | `SIMPLETOOLS_KEYSTORE_BASE64` | sortie de `base64 -w0 simpletools-scan-upload.jks` (macOS : `base64 -i simpletools-scan-upload.jks`) |
   | `SIMPLETOOLS_KEYSTORE_PASSWORD` | mot de passe du keystore |
   | `SIMPLETOOLS_KEY_ALIAS` | `upload` |
   | `SIMPLETOOLS_KEY_PASSWORD` | mot de passe de la clé |

3. *Settings → Secrets and variables → Actions → Variables* : `SIMPLETOOLS_RELEASE_ENABLED` = `true`.

## 3. Construire l'AAB signé

Après la fusion de la PR dans `main` :

```bash
git checkout main && git pull
git tag v1.0.0 && git push origin v1.0.0
```

(ou *Actions → Release → Run workflow* sur `main`). Le workflow relance tests, lint et audit des
permissions, construit `app-release.aab`, **vérifie sa signature** (`jarsigner -verify`) et publie
l'artefact `simpletools-release-aab` (AAB + `mapping.txt` R8).

## 4. Play Console

1. *Créer une application* : nom **SimpleTools Scan**, application gratuite, package `com.simpletools.scan`.
2. *Tester et publier → Tests internes* : envoyer `app-release.aab`, ajouter `mapping.txt`
   (*Explorateur d'app bundle → Fichiers de désobscurcissement*).
3. *Contenu de l'application* :
   - **Règles de confidentialité** : URL obligatoire (l'app utilise la caméra). Une fois l'URL publiée, la
     renseigner aussi dans `privacy.policyUrl` (`simpletools.properties`) pour l'afficher dans « À propos ».
   - **Sécurité des données** : *Aucune donnée collectée ni partagée*. Photos et PDF restent sur l'appareil,
     aucune permission Internet ; le partage est une action explicite de l'utilisateur vers l'app de son choix.
   - **Annonces** : non. **Public cible** : 13 ans et plus (outil généraliste). **Classification** : questionnaire IARC
     (aucun contenu sensible).
   - **Autorisations** : `CAMERA` — photographier les pages à numériser, uniquement quand l'utilisateur le demande.
4. *Fiche Play Store* (texte proposé ci-dessous) + icône 512×512, image de présentation 1024×500 et au moins
   2 captures d'écran de téléphone (le Galaxy A14 convient).
5. Tester en interne, puis promouvoir en production.

## Texte de fiche proposé

**Titre** : SimpleTools Scan

**Description courte (FR)** : Numérisez vos documents en PDF, hors ligne, sans compte ni publicité.

**Short description (EN)** : Scan documents to PDF — offline, no account, no ads.

**Description complète (FR)**
> Photographiez vos pages, ajustez leurs quatre coins pour les redresser, réunissez-les et exportez un PDF.
>
> • Correction de perspective avec quatre coins réglables et aperçu avant validation
> • Rotation à 90°, réorganisation, suppression ou reprise d'une page
> • PDF multipage au nom de votre choix : enregistrez-le, partagez-le ou ouvrez-le dans SimpleTools PDF
> • Fonctionne hors ligne : aucune connexion, aucun compte, aucune publicité
> • Vos photos restent sur votre téléphone ; seule la caméra est demandée, au moment de photographier

**Full description (EN)**
> Photograph your pages, drag their four corners to straighten them, put them together and export one PDF.
>
> • Perspective correction with four adjustable corners and a preview before saving
> • 90° rotation, reorder, delete or retake any page
> • Multi-page PDF with the name you choose: save it, share it or open it in SimpleTools PDF
> • Works offline: no connection, no account, no ads
> • Your photos stay on your phone; only the camera is requested, when you take a photo
