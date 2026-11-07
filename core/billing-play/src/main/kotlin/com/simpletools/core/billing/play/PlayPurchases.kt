package com.simpletools.core.billing.play

import com.simpletools.core.billing.StoreOutcome

/** The part of a Play purchase that matters for an entitlement. */
data class PurchaseSnapshot(
    val products: List<String>,
    val purchased: Boolean,
    val pending: Boolean,
    val acknowledged: Boolean,
)

/** Reduces the purchases returned by Google Play for one product to a [StoreOutcome]. */
object PlayPurchases {

    fun summarize(productId: String, purchases: List<PurchaseSnapshot>): StoreOutcome {
        val mine = purchases.filter { productId in it.products }
        return StoreOutcome.Answered(
            ownsProduct = mine.any { it.purchased },
            purchasePending = mine.any { it.pending && !it.purchased },
        )
    }

    /** Purchases that were bought but not acknowledged yet: Google refunds them after three days. */
    fun needingAcknowledgement(purchases: List<PurchaseSnapshot>): List<PurchaseSnapshot> =
        purchases.filter { it.purchased && !it.acknowledged }
}
