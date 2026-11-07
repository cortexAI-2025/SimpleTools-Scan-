package com.simpletools.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Optional one-time purchase ("PRO") of a SimpleTools app.
 *
 * - [state] is what the UI shows. [PremiumState.Purchased] is the only value that unlocks PRO features.
 * - [connect] is idempotent: it loads the remembered entitlement at once, then asks the store.
 * - Nothing is granted by a simulation: the state only changes from a store answer or from an
 *   entitlement the store has already confirmed on this device.
 *
 * The template ships [NoOpBillingRepository]. See docs/MONETIZATION.md.
 */
interface BillingRepository {
    val state: StateFlow<BillingState>

    fun connect()

    /** Starts the purchase; returns once the store has answered (success, pending, cancelled or failed). */
    suspend fun purchasePremium(activity: Activity): PurchaseResult

    /** Asks the store which purchases this account owns, and applies the answer. */
    suspend fun restorePurchases(): RestoreResult
}

data class BillingState(
    val premium: PremiumState = PremiumState.Unavailable,
    /** Price exactly as Google Play formats it (e.g. "3,99 €"); null until the store has answered. */
    val priceText: String? = null,
    val check: StoreCheck = StoreCheck.NotChecked,
) {
    val isPremium: Boolean get() = premium == PremiumState.Purchased
}

sealed interface PremiumState {
    /** Billing is not part of this product, or the store is not available on this device. */
    data object Unavailable : PremiumState
    data object NotPurchased : PremiumState
    data object Pending : PremiumState
    data object Purchased : PremiumState
}

val PremiumState.isPremium: Boolean get() = this == PremiumState.Purchased

/** How the current [BillingState.premium] was established. */
sealed interface StoreCheck {
    data object NotChecked : StoreCheck
    data object Checking : StoreCheck

    /** Google Play answered during this session. */
    data object Verified : StoreCheck

    /** Google Play could not be reached: the purchase verified earlier on this device is used. */
    data object RememberedOffline : StoreCheck

    /** Google Play could not be reached and no earlier purchase is known on this device. */
    data object Unreachable : StoreCheck
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Pending : PurchaseResult
    data object Cancelled : PurchaseResult
    data object Unavailable : PurchaseResult
    data class Failed(val reason: String) : PurchaseResult
}

sealed interface RestoreResult {
    /** The account owns the purchase; PRO is active. */
    data object Restored : RestoreResult
    data object NothingToRestore : RestoreResult
    data object Unavailable : RestoreResult
    data class Failed(val reason: String) : RestoreResult
}

/**
 * Persists, on this device only, that the store confirmed the purchase. It is what keeps PRO
 * available offline. It is cleared as soon as a store answer says the purchase is no longer owned
 * (refund, revocation).
 */
interface LicenseStore {
    suspend fun rememberedOwnership(): Boolean
    suspend fun remember(owned: Boolean)
}

object NoOpBillingRepository : BillingRepository {
    private val unavailable = MutableStateFlow(BillingState(premium = PremiumState.Unavailable))
    override val state: StateFlow<BillingState> = unavailable.asStateFlow()
    override fun connect() = Unit
    override suspend fun purchasePremium(activity: Activity): PurchaseResult = PurchaseResult.Unavailable
    override suspend fun restorePurchases(): RestoreResult = RestoreResult.Unavailable
}
