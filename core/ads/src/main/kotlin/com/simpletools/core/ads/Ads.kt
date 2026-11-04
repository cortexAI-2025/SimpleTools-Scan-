package com.simpletools.core.ads

/**
 * Optional advertising abstraction.
 *
 * No ad SDK is part of the template. A product with `monetization.ads=true`
 * (which requires `privacy.localOnly=false`) adds a provider module implementing
 * [AdsProvider], wires it in the app container and updates its privacy disclosure.
 * See docs/MONETIZATION.md.
 */
interface AdsProvider {
    /** Initializes the SDK. Must only be called after any required consent was collected. */
    suspend fun initialize()

    val isReady: Boolean
}

object NoOpAdsProvider : AdsProvider {
    override suspend fun initialize() = Unit
    override val isReady: Boolean = false
}

/** SimpleTools advertising rules, shared by every product. */
object AdPolicy {

    /**
     * Ads are shown only when the product enables them, the provider is ready and the
     * user has not bought premium/ad removal.
     */
    fun shouldShowAds(adsEnabled: Boolean, providerReady: Boolean, hasPremium: Boolean): Boolean =
        adsEnabled && providerReady && !hasPremium
}
