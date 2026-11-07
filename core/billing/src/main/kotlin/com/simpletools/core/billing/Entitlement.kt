package com.simpletools.core.billing

/** What the store said about the product, after a query. */
sealed interface StoreOutcome {
    /** The store answered. [ownsProduct] wins over [purchasePending]. */
    data class Answered(val ownsProduct: Boolean, val purchasePending: Boolean) : StoreOutcome

    /** No answer: no network, the store service is down, or the store is not installed. */
    data object Unreachable : StoreOutcome
}

data class EntitlementDecision(
    val premium: PremiumState,
    val check: StoreCheck,
    /** What to persist for the next offline start. */
    val rememberOwnership: Boolean,
)

/**
 * The single place where a licence is granted or withdrawn:
 * - the store says the product is owned → PRO, and remembered for offline use;
 * - the store says it is not owned (refund, revocation, never bought) → no PRO, memory cleared;
 * - the store cannot be reached → PRO only if it was verified before on this device, otherwise no PRO
 *   and the UI says that the check failed (it is never silently granted).
 */
object Entitlement {

    fun decide(outcome: StoreOutcome, rememberedOwnership: Boolean): EntitlementDecision = when (outcome) {
        is StoreOutcome.Answered -> when {
            outcome.ownsProduct -> EntitlementDecision(PremiumState.Purchased, StoreCheck.Verified, rememberOwnership = true)
            outcome.purchasePending -> EntitlementDecision(PremiumState.Pending, StoreCheck.Verified, rememberOwnership = false)
            else -> EntitlementDecision(PremiumState.NotPurchased, StoreCheck.Verified, rememberOwnership = false)
        }
        StoreOutcome.Unreachable ->
            if (rememberedOwnership) {
                EntitlementDecision(PremiumState.Purchased, StoreCheck.RememberedOffline, rememberOwnership = true)
            } else {
                EntitlementDecision(PremiumState.NotPurchased, StoreCheck.Unreachable, rememberOwnership = false)
            }
    }
}
