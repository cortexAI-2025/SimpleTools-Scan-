# Analytics, ads and billing (optional)

The template ships **interfaces and no-op implementations only**:

| Module | Interface | Default |
|--------|-----------|---------|
| `core:analytics` | `AnalyticsTracker`, `GatedAnalyticsTracker` | `NoOpAnalyticsTracker` |
| `core:ads` | `AdsProvider`, `AdPolicy` | `NoOpAdsProvider` |
| `core:billing` | `BillingRepository`, `PremiumState`, `PurchaseResult` | `NoOpBillingRepository` |

They are pure Kotlin modules with no third-party dependency, so a free utility ships no ads/billing
SDK at all. The implementations are selected in one place: `app/.../AppContainer.kt`.

## Enabling billing (premium / ad removal)

1. Create `core/billing-play` (`simpletools.android.library`) depending on
   `com.android.billingclient:billing-ktx` (add it to the version catalog) and implementing
   `BillingRepository` (connect, query the one-time product, launch the flow with the current
   Activity, acknowledge purchases, map to `PremiumState`).
2. In the product: `monetization.premium=true`, `implementation(projects.core.billingPlay)` in the app,
   and `val billing: BillingRepository = PlayBillingRepository(context)` in `AppContainer`.
3. The Billing library adds `com.android.vending.BILLING`: list it in `permissions.declared`.
4. Add a `feature:premium` screen (purchase + restore) when the first product needs it.

## Enabling ads

Ads need the network, so they are only allowed with `privacy.localOnly=false`
(`AppFactoryConfig` rejects the contradiction).

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
(`snake_case` names) and never contain user content.
