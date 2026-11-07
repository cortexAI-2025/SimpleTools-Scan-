package com.simpletools.core.billing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementTest {

    @Test
    fun `an owned product unlocks PRO and is remembered`() {
        val d = Entitlement.decide(StoreOutcome.Answered(ownsProduct = true, purchasePending = false), rememberedOwnership = false)
        assertEquals(PremiumState.Purchased, d.premium)
        assertEquals(StoreCheck.Verified, d.check)
        assertTrue(d.rememberOwnership)
    }

    @Test
    fun `owned wins over pending`() {
        val d = Entitlement.decide(StoreOutcome.Answered(ownsProduct = true, purchasePending = true), rememberedOwnership = false)
        assertEquals(PremiumState.Purchased, d.premium)
    }

    @Test
    fun `a pending purchase is not PRO yet and is not remembered`() {
        val d = Entitlement.decide(StoreOutcome.Answered(ownsProduct = false, purchasePending = true), rememberedOwnership = false)
        assertEquals(PremiumState.Pending, d.premium)
        assertFalse(d.premium.isPremium)
        assertFalse(d.rememberOwnership)
    }

    @Test
    fun `a refund clears the remembered purchase`() {
        val d = Entitlement.decide(StoreOutcome.Answered(ownsProduct = false, purchasePending = false), rememberedOwnership = true)
        assertEquals(PremiumState.NotPurchased, d.premium)
        assertEquals(StoreCheck.Verified, d.check)
        assertFalse(d.rememberOwnership)
    }

    @Test
    fun `offline with a remembered purchase keeps PRO`() {
        val d = Entitlement.decide(StoreOutcome.Unreachable, rememberedOwnership = true)
        assertEquals(PremiumState.Purchased, d.premium)
        assertEquals(StoreCheck.RememberedOffline, d.check)
        assertTrue(d.rememberOwnership)
    }

    @Test
    fun `offline without any memory never grants PRO and says the check failed`() {
        val d = Entitlement.decide(StoreOutcome.Unreachable, rememberedOwnership = false)
        assertEquals(PremiumState.NotPurchased, d.premium)
        assertEquals(StoreCheck.Unreachable, d.check)
        assertFalse(d.rememberOwnership)
    }

    @Test
    fun `no billing repository ever reports PRO`() = runTest {
        val state = NoOpBillingRepository.state.value
        assertEquals(PremiumState.Unavailable, state.premium)
        assertFalse(state.isPremium)
    }

    @Test
    fun `only a completed purchase counts as PRO`() {
        assertTrue(PremiumState.Purchased.isPremium)
        assertFalse(PremiumState.Pending.isPremium)
        assertFalse(PremiumState.NotPurchased.isPremium)
        assertFalse(PremiumState.Unavailable.isPremium)
    }
}
