package com.simpletools.core.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Optional premium/ad-removal purchase abstraction.
 *
 * The template ships only [NoOpBillingRepository]: no Google Play Billing library
 * is included. A product with `monetization.premium=true` adds a dedicated module
 * (e.g. `:core:billing-play`) implementing [BillingRepository] and wires it in the
 * app container. See docs/MONETIZATION.md.
 */
interface BillingRepository {
    val premiumState: Flow<PremiumState>

    suspend fun purchasePremium(): PurchaseResult

    suspend fun restorePurchases()
}

sealed interface PremiumState {
    /** Billing is not part of this product, or the store is not reachable. */
    data object Unavailable : PremiumState
    data object NotPurchased : PremiumState
    data object Pending : PremiumState
    data object Purchased : PremiumState
}

val PremiumState.isPremium: Boolean get() = this == PremiumState.Purchased

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Pending : PurchaseResult
    data object Cancelled : PurchaseResult
    data object Unavailable : PurchaseResult
    data class Failed(val reason: String) : PurchaseResult
}

object NoOpBillingRepository : BillingRepository {
    override val premiumState: Flow<PremiumState> = flowOf(PremiumState.Unavailable)
    override suspend fun purchasePremium(): PurchaseResult = PurchaseResult.Unavailable
    override suspend fun restorePurchases() = Unit
}
