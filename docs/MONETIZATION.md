# Monetization: SimpleTools Scan PRO, analytics and ads

## Model of SimpleTools Scan

**Freemium with a one-time purchase.** The free version is complete for scanning and is free for life:
no daily limit, no watermark, no account, no advertising. PRO is a single non-consumable purchase,
**3,99 € incl. VAT** (price set in the Play Console, shown as returned by Google Play), with no subscription.

| | Free (forever) | PRO (one purchase) |
|---|---|---|
| Camera scan, photo import | ✅ | ✅ |
| Automatic page detection, perspective, rotation, manual corners | ✅ | ✅ |
| Colour, grey, black and white filters | ✅ | ✅ |
| Multi-page documents, reorder, retake, delete | ✅ | ✅ |
| PDF export, save, share, *Open in SimpleTools PDF* | ✅ | ✅ |
| Offline, no account, no ads | ✅ | ✅ |
| Text recognition (OCR), French and English, offline | | ✅ |
| Copy and search the recognised text, export TXT | | ✅ |
| Searchable PDF (invisible text layer aligned on the image) | | ✅ |
| Contrast adjustment per page | | ✅ |

Not yet in PRO (planned, not shipped in this version): batch processing, scanning profiles, PDF
compression settings, folders and categories, advanced search, automatic naming, local backup and
restore, advanced sharpness and orientation detection. They must not be advertised until they exist.

## Architecture of the licence

| Module | Role | Dependencies |
|--------|------|--------------|
| `core:billing` | Contract shared by all SimpleTools apps: `BillingRepository`, `BillingState`, `PremiumState`, `StoreCheck`, `LicenseStore` and the entitlement rules (`Entitlement.decide`) | none (Android library, `Activity` in the purchase call) |
| `core:billing-play` | Google Play Billing implementation: `PlayBillingRepository`, `DataStoreLicenseStore`, purchase summary (`PlayPurchases`) | `billing-ktx` 9.1.0 |
| `feature:premium` | The PRO screen: comparison, licence state, price, buy, restore, privacy explanation | `core:billing` |
| `app/AppContainer` | Chooses `PlayBillingRepository` when `monetization.premium=true`, else `NoOpBillingRepository` | |

Rules implemented in `Entitlement.decide` (pure Kotlin, unit tested):

- Google Play says the product is owned → PRO, and the ownership is remembered on the device.
- Google Play says it is not owned (never bought, refund, revocation) → no PRO, the memory is cleared.
- Purchase pending (e.g. cash payment) → `Pending`, **not** PRO until Google Play confirms.
- Google Play cannot be reached → PRO **only** if a purchase was verified earlier on this device
  (offline use). Otherwise no PRO and the screen says that the check failed. A licence is never granted
  by a simulation.

Other behaviour of `PlayBillingRepository`:

- The price is the `formattedPrice` returned by Play for `simpletools_scan_pro_lifetime`; when Play does
  not know the product, the price shows as unavailable and the purchase button stays disabled.
- Purchases are acknowledged as soon as they are seen (Google refunds unacknowledged purchases after
  three days). Restore asks Play again and applies the answer.
- Billing is started at launch (`AppContainer.init`), so the remembered licence is available at once.
- The Play SDK authenticates the user. The app never sees or stores payment data, card numbers, accounts
  or receipts; it stores one boolean (`product_owned`) in its private DataStore.

Known limit: on a rooted device the local memory can be edited. It only matters offline, and the store
check restores the truth at the next connection.

## Reusing the licence in another SimpleTools app

1. Depend on `:core:billing` and `:core:billing-play`.
2. Choose the product id and create the product in that app's Play Console entry.
3. In its `AppContainer`, create `PlayBillingRepository(context, productId, DataStoreLicenseStore.create(context), scope)`
   when `monetization.premium=true`, and `billing.connect()` once at launch.
4. Declare `com.android.vending.BILLING` in `permissions.declared` (the build fails otherwise).
5. Gate PRO features with `billing.state.value.isPremium` at the point of use; never gate by a flag in the UI alone.

No product id, price or key is stored in the repository. `docs/PLAY_CONSOLE.md` lists what the Play
Console must contain.

## Text recognition (ML Kit, on device)

The OCR uses **ML Kit text recognition** with the Latin model **bundled in the APK**
(`com.google.mlkit:text-recognition`, Latin script, covers French and English). Recognition runs on the
phone and needs no network connection and no download at run time. Words are stored per page in the
app's private storage as JSON (text and boxes). The searchable PDF places each word, as invisible text
(render mode 3, Helvetica WinAnsi), over its position in the image.

Before publishing, check on a real phone (network capture, Android *Data usage* or a proxy) that
Google's ML Kit runtime does not send images or text to a server; the documentation of the Play Data
safety form must match that result.

## Enabling ads

Ads need the network, so they are only allowed with `privacy.localOnly=false`
(`AppFactoryConfig` rejects the contradiction). SimpleTools Scan has no ads.

1. Create `core/ads-<provider>` implementing `AdsProvider`; collect consent (UMP / GDPR) **before**
   `initialize()`.
2. Product: `privacy.localOnly=false`, `monetization.ads=true`, declare `android.permission.INTERNET`
   (and any permission the SDK adds) in `permissions.declared`, wire the provider in `AppContainer`.
3. Show ads only when `AdPolicy.shouldShowAds(adsEnabled, provider.isReady, premiumState.isPremium)`,
   never on onboarding, settings or permission screens, and never over the core task.
4. Update the privacy policy and the Play Data safety form; the in-app Privacy screen switches to
   "Contains ads" automatically.

## Enabling analytics

Same pattern: provider module implementing `AnalyticsTracker`, wrapped in `GatedAnalyticsTracker`
(config flag + user consent), `privacy.analytics=true` + `privacy.localOnly=false`. Events are coarse
(`snake_case` names) and never contain user content. SimpleTools Scan sends no analytics.
