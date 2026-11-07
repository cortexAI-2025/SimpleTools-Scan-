package com.simpletools.feature.premium

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.core.billing.BillingState
import com.simpletools.core.billing.PremiumState
import com.simpletools.core.billing.StoreCheck
import com.simpletools.core.designsystem.theme.SimpleToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The PRO offer must never unlock or suggest a purchase it cannot make: the buy button needs a price
 * from the store, and the licence status says how the state was established.
 */
@RunWith(AndroidJUnit4::class)
class PremiumScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var purchases = 0
    private var restores = 0
    private var retries = 0

    private fun show(state: BillingState) = composeRule.setContent {
        SimpleToolsTheme {
            PremiumScreen(
                state = state,
                snackbarHostState = SnackbarHostState(),
                onNavigateBack = {},
                onPurchase = { purchases++ },
                onRestore = { restores++ },
                onRetryCheck = { retries++ },
            )
        }
    }

    @Test
    fun `without a store price the purchase is not offered`() {
        show(BillingState(premium = PremiumState.NotPurchased, priceText = null, check = StoreCheck.Verified))

        composeRule.onNodeWithText("Price not available right now.").assertExists()
        composeRule.onNodeWithTag(PURCHASE_BUTTON_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(RESTORE_BUTTON_TAG).assertIsEnabled()
    }

    @Test
    fun `with the store price the purchase is offered and shows the price as Play formats it`() {
        show(BillingState(premium = PremiumState.NotPurchased, priceText = "3,99 €", check = StoreCheck.Verified))

        composeRule.onNodeWithTag(PRICE_TAG).assertTextEquals("3,99 €")
        composeRule.onNodeWithTag(PURCHASE_BUTTON_TAG).assertIsEnabled().performScrollTo().performClick()
        assertEquals(1, purchases)
    }

    @Test
    fun `an owned licence disables the purchase and shows it as active`() {
        show(BillingState(premium = PremiumState.Purchased, priceText = "3,99 €", check = StoreCheck.Verified))

        composeRule.onNodeWithText("PRO is unlocked on this device.").assertExists()
        composeRule.onNodeWithTag(PURCHASE_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun `an offline licence says it was verified earlier`() {
        show(BillingState(premium = PremiumState.Purchased, priceText = "3,99 €", check = StoreCheck.RememberedOffline))

        composeRule.onNodeWithText(
            "PRO is unlocked. The purchase was verified earlier, so it works offline.",
        ).assertExists()
    }

    @Test
    fun `an unreachable store never reads as a licence and offers a retry`() {
        show(BillingState(premium = PremiumState.NotPurchased, priceText = "3,99 €", check = StoreCheck.Unreachable))

        composeRule.onNodeWithText(
            "The licence cannot be checked right now. Connect to the internet, then check again.",
        ).assertExists()
        composeRule.onNodeWithText("Check again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `restore is always available when no check is running`() {
        show(BillingState(premium = PremiumState.NotPurchased, priceText = "3,99 €", check = StoreCheck.Verified))

        composeRule.onNodeWithTag(RESTORE_BUTTON_TAG).performScrollTo().performClick()
        assertEquals(1, restores)
    }
}
