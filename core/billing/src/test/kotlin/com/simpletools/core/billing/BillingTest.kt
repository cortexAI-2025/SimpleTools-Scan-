package com.simpletools.core.billing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingTest {

    @Test
    fun `no-op billing is unavailable and never grants premium`() = runTest {
        val state = NoOpBillingRepository.premiumState.first()

        assertEquals(PremiumState.Unavailable, state)
        assertFalse(state.isPremium)
        assertEquals(PurchaseResult.Unavailable, NoOpBillingRepository.purchasePremium())
    }

    @Test
    fun `only a completed purchase is premium`() {
        assertTrue(PremiumState.Purchased.isPremium)
        assertFalse(PremiumState.Pending.isPremium)
        assertFalse(PremiumState.NotPurchased.isPremium)
    }
}
