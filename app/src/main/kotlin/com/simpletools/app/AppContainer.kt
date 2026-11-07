package com.simpletools.app

import android.content.Context
import com.simpletools.core.ads.AdsProvider
import com.simpletools.core.ads.NoOpAdsProvider
import com.simpletools.core.analytics.AnalyticsTracker
import com.simpletools.core.analytics.NoOpAnalyticsTracker
import com.simpletools.core.billing.BillingRepository
import com.simpletools.core.billing.NoOpBillingRepository
import com.simpletools.core.billing.play.DataStoreLicenseStore
import com.simpletools.core.billing.play.PlayBillingRepository
import com.simpletools.core.common.AppConfig
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.core.settings.DataStoreSettingsRepository
import com.simpletools.core.settings.SettingsRepository
import com.simpletools.feature.scanner.ScannerDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application-wide dependencies. This is the single place where a product swaps the
 * optional integrations (analytics, ads, billing) for real implementations.
 */
class AppContainer(context: Context) {

    val appConfig: AppConfig = buildConfigAppConfig()

    val privacyDeclaration: PrivacyDeclaration = PrivacyDeclaration.from(appConfig)

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository.create(context.applicationContext.filesDir)

    // Optional integrations: no-op by default, so no third-party SDK is shipped.
    // See docs/MONETIZATION.md to enable them in a product.
    val analytics: AnalyticsTracker = NoOpAnalyticsTracker
    val ads: AdsProvider = NoOpAdsProvider

    /** SimpleTools Scan PRO: Google Play Billing when the product sells PRO, otherwise nothing. */
    val billing: BillingRepository = if (appConfig.monetization.premium) {
        PlayBillingRepository(
            context = context.applicationContext,
            productId = PRO_PRODUCT_ID,
            licenseStore = DataStoreLicenseStore.create(context.applicationContext),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    } else {
        NoOpBillingRepository
    }

    /** The document being scanned, its private files and the image/PDF engines. */
    val scanner: ScannerDependencies = ScannerDependencies.create(context) { billing.state.value.isPremium }

    init {
        // Loads the licence at launch: a purchase verified earlier is available offline at once.
        billing.connect()
    }

    companion object {
        /** Non-consumable product created in the Play Console (see docs/PLAY_CONSOLE.md). */
        const val PRO_PRODUCT_ID = "simpletools_scan_pro_lifetime"
    }
}

/** Maps the fields generated from simpletools.properties to the runtime [AppConfig]. */
internal fun buildConfigAppConfig(): AppConfig = AppConfig(
    applicationId = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE.toLong(),
    accent = BuildConfig.BRAND_ACCENT,
    supportedLanguages = AppConfig.parseList(BuildConfig.SUPPORTED_LANGUAGES),
    features = AppConfig.Features(onboarding = BuildConfig.FEATURE_ONBOARDING),
    monetization = AppConfig.Monetization(
        ads = BuildConfig.MONETIZATION_ADS,
        premium = BuildConfig.MONETIZATION_PREMIUM,
    ),
    privacy = AppConfig.Privacy(
        localOnly = BuildConfig.PRIVACY_LOCAL_ONLY,
        analytics = BuildConfig.PRIVACY_ANALYTICS,
        policyUrl = BuildConfig.PRIVACY_POLICY_URL.ifBlank { null },
    ),
    declaredPermissions = AppConfig.parseList(BuildConfig.DECLARED_PERMISSIONS),
)
