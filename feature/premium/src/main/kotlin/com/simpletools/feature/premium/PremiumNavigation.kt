package com.simpletools.feature.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.simpletools.core.billing.BillingRepository
import com.simpletools.core.billing.PurchaseResult
import com.simpletools.core.billing.RestoreResult
import com.simpletools.feature.premium.R
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** SimpleTools Scan PRO: comparison, price, purchase, restore, licence state, privacy. */
@Serializable
data object PremiumRoute

fun NavController.navigateToPremium() = navigate(PremiumRoute) { launchSingleTop = true }

fun NavGraphBuilder.premiumScreen(
    billing: BillingRepository,
    onNavigateBack: () -> Unit,
) {
    composable<PremiumRoute> {
        val state by billing.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        // Idempotent: loads the remembered licence at once, then asks Google Play.
        LaunchedEffect(billing) { billing.connect() }

        PremiumScreen(
            state = state,
            snackbarHostState = snackbar,
            onNavigateBack = onNavigateBack,
            onPurchase = {
                val activity = context.findActivity()
                if (activity != null) {
                    scope.launch {
                        val message = purchaseMessage(context, billing.purchasePremium(activity))
                        if (message != null) snackbar.showSnackbar(message)
                    }
                }
            },
            onRestore = {
                scope.launch { snackbar.showSnackbar(restoreMessage(context, billing.restorePurchases())) }
            },
            onRetryCheck = billing::connect,
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** What the user is told after a purchase attempt; a cancelled purchase needs no message. */
internal fun purchaseMessage(context: Context, result: PurchaseResult): String? = when (result) {
    PurchaseResult.Success -> context.getString(R.string.pro_purchase_success)
    PurchaseResult.Pending -> context.getString(R.string.pro_purchase_pending)
    PurchaseResult.Cancelled -> null
    PurchaseResult.Unavailable -> context.getString(R.string.pro_store_unavailable)
    is PurchaseResult.Failed -> context.getString(R.string.pro_purchase_failed)
}

internal fun restoreMessage(context: Context, result: RestoreResult): String = when (result) {
    RestoreResult.Restored -> context.getString(R.string.pro_restore_success)
    RestoreResult.NothingToRestore -> context.getString(R.string.pro_restore_nothing)
    RestoreResult.Unavailable -> context.getString(R.string.pro_store_unavailable)
    is RestoreResult.Failed -> context.getString(R.string.pro_restore_failed)
}
