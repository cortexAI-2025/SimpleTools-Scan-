package com.simpletools.core.billing.play

import com.simpletools.core.billing.StoreOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPurchasesTest {

    private val pro = "simpletools_scan_pro_lifetime"

    private fun snapshot(vararg products: String, purchased: Boolean = true, pending: Boolean = false, acknowledged: Boolean = true) =
        PurchaseSnapshot(products.toList(), purchased, pending, acknowledged)

    @Test
    fun `an empty answer means nothing is owned`() {
        assertEquals(StoreOutcome.Answered(ownsProduct = false, purchasePending = false), PlayPurchases.summarize(pro, emptyList()))
    }

    @Test
    fun `a purchase of the product is owned`() {
        val outcome = PlayPurchases.summarize(pro, listOf(snapshot(pro)))
        assertEquals(StoreOutcome.Answered(ownsProduct = true, purchasePending = false), outcome)
    }

    @Test
    fun `a purchase of another product does not unlock this app`() {
        val outcome = PlayPurchases.summarize(pro, listOf(snapshot("other_product")))
        assertEquals(StoreOutcome.Answered(ownsProduct = false, purchasePending = false), outcome)
    }

    @Test
    fun `a pending purchase is reported as pending, not owned`() {
        val outcome = PlayPurchases.summarize(pro, listOf(snapshot(pro, purchased = false, pending = true, acknowledged = false)))
        assertEquals(StoreOutcome.Answered(ownsProduct = false, purchasePending = true), outcome)
    }

    @Test
    fun `only bought and unacknowledged purchases need acknowledgement`() {
        val list = listOf(
            snapshot(pro, acknowledged = false),
            snapshot(pro, acknowledged = true),
            snapshot(pro, purchased = false, pending = true, acknowledged = false),
        )
        val needing = PlayPurchases.needingAcknowledgement(list)
        assertEquals(1, needing.size)
        assertTrue(needing.single().purchased && !needing.single().acknowledged)
    }
}
