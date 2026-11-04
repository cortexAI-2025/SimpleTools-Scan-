# Release signing and Google Play

No signing key, password or service account is ever committed. `.gitignore` excludes keystores and
`scripts/check-no-secrets.sh` fails CI if one is tracked.

## Signing inputs

`build-logic` (`ReleaseSigning.kt`) reads four values from **environment variables** or, locally,
from your **user** Gradle properties (`~/.gradle/gradle.properties`, outside the repository):

| Name | Meaning |
|------|---------|
| `SIMPLETOOLS_KEYSTORE_FILE` | Absolute path to the upload keystore (`.jks`) |
| `SIMPLETOOLS_KEYSTORE_PASSWORD` | Keystore password |
| `SIMPLETOOLS_KEY_ALIAS` | Key alias |
| `SIMPLETOOLS_KEY_PASSWORD` | Key password |

If any value is missing the release build is simply unsigned.

## Create an upload key (once per product)

```bash
keytool -genkeypair -v -keystore simpletools-pdf-upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Store the keystore and passwords in a password manager. Enroll the app in
**Play App Signing**: Google keeps the app signing key, you only hold the upload key (which can be
reset if lost).

## GitHub secrets (needed later, for `.github/workflows/release.yml`)

Create a `release` environment (Settings → Environments) and add:

| Secret | Value |
|--------|-------|
| `SIMPLETOOLS_KEYSTORE_BASE64` | `base64 -w0 simpletools-pdf-upload.jks` |
| `SIMPLETOOLS_KEYSTORE_PASSWORD` | keystore password |
| `SIMPLETOOLS_KEY_ALIAS` | key alias |
| `SIMPLETOOLS_KEY_PASSWORD` | key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | *(future)* Play Console service account, for automated uploads |

Then enable the workflow with the repository **variable** `SIMPLETOOLS_RELEASE_ENABLED=true`
(Settings → Secrets and variables → Actions → Variables). Without it the release job is skipped:
this keeps template tags and products not yet on Play free of failing runs.

Pushing a tag `vX.Y.Z` (or running the workflow manually) then runs tests, lint and the permission
audit, builds `app-release.aab`, verifies its signature and uploads it with the R8 mapping file.
> Status: the unsigned `bundleRelease` path is exercised by CI on every PR; the signed workflow has not
> run yet because no upload key exists — it will be exercised with the first product release.

Upload to Play is still manual (internal testing track first) until the Phase 4 publishing workflow.

## Release checklist

1. Bump `app.versionCode` (always increasing) and `app.versionName` in `simpletools.properties`.
2. Set `privacy.policyUrl` and make sure the Play Data safety form matches `PrivacyDeclaration`.
3. Check `permissions.declared` only lists what the features need.
4. Tag and push: `git tag v1.0.0 && git push origin v1.0.0`.
5. Upload the AAB to the internal testing track, test, then promote.
