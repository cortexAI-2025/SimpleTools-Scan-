package com.simpletools.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    @Test
    fun `ads require the feature flag and a ready provider`() {
        assertFalse(AdPolicy.shouldShowAds(adsEnabled = false, providerReady = true, hasPremium = false))
        assertFalse(AdPolicy.shouldShowAds(adsEnabled = true, providerReady = false, hasPremium = false))
        assertTrue(AdPolicy.shouldShowAds(adsEnabled = true, providerReady = true, hasPremium = false))
    }

    @Test
    fun `premium always removes ads`() {
        assertFalse(AdPolicy.shouldShowAds(adsEnabled = true, providerReady = true, hasPremium = true))
    }

    @Test
    fun `no-op provider is never ready`() {
        assertFalse(NoOpAdsProvider.isReady)
    }
}
