package com.simpletools.core.billing.play

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.QueryPurchasesParams
import com.simpletools.core.billing.BillingRepository
import com.simpletools.core.billing.BillingState
import com.simpletools.core.billing.Entitlement
import com.simpletools.core.billing.LicenseStore
import com.simpletools.core.billing.PremiumState
import com.simpletools.core.billing.PurchaseResult
import com.simpletools.core.billing.RestoreResult
import com.simpletools.core.billing.StoreCheck
import com.simpletools.core.billing.StoreOutcome
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Google Play Billing for one non-consumable in-app product ("lifetime" unlock).
 *
 * - Price: the formatted price returned by Play for [productId]; null when Play does not know the
 *   product (not yet created in the Play Console, or not published), and the UI says so.
 * - Entitlement: granted only from a Play answer, or from the ownership remembered after a verified
 *   answer when Play cannot be reached (see [Entitlement]).
 * - Purchases are acknowledged as soon as they are seen, as Google Play requires.
 * - Pending purchases (e.g. cash payment) are reported as [PremiumState.Pending] and are not PRO yet.
 *
 * Nothing secret is used: the Play SDK authenticates the user; the app never handles payment data.
 */
class PlayBillingRepository(
    context: Context,
    private val productId: String,
    private val licenseStore: LicenseStore,
    private val scope: CoroutineScope,
) : BillingRepository {

    private val _state = MutableStateFlow(BillingState(premium = PremiumState.NotPurchased, check = StoreCheck.NotChecked))
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    private val syncLock = Mutex()
    private val connectLock = Mutex()
    // Written from the UI thread and read from the billing callback thread.
    @Volatile private var productDetails: ProductDetails? = null
    @Volatile private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener(PurchasesUpdatedListener { result, purchases -> onPurchasesUpdated(result, purchases) })
        .build()

    override fun connect() {
        scope.launch { sync() }
    }

    override suspend fun purchasePremium(activity: Activity): PurchaseResult {
        if (!ensureConnected()) return PurchaseResult.Unavailable
        val details = loadProductDetails() ?: return PurchaseResult.Unavailable
        val deferred = CompletableDeferred<PurchaseResult>()
        pendingPurchase = deferred
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            )
            .build()
        val launched = client.launchBillingFlow(activity, params)
        if (launched.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return when (launched.responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> PurchaseResult.Unavailable
                else -> PurchaseResult.Failed(launched.debugMessage.orEmpty())
            }
        }
        return deferred.await()
    }

    override suspend fun restorePurchases(): RestoreResult {
        val outcome = syncLock.withLock { queryAndApply() }
        return when (outcome) {
            StoreOutcome.Unreachable -> RestoreResult.Unavailable
            is StoreOutcome.Answered -> if (outcome.ownsProduct) RestoreResult.Restored else RestoreResult.NothingToRestore
        }
    }

    /** Asks the store and applies its answer to the state and the remembered ownership. */
    private suspend fun sync() {
        syncLock.withLock { queryAndApply() }
    }

    private suspend fun queryAndApply(): StoreOutcome {
        val remembered = licenseStore.rememberedOwnership()
        // Offline start: the purchase verified earlier on this device is shown at once.
        if (remembered) _state.update { it.copy(premium = PremiumState.Purchased, check = StoreCheck.Checking) }
        else _state.update { it.copy(check = StoreCheck.Checking) }

        val outcome = queryStore()
        applyOutcome(outcome, remembered)
        return outcome
    }

    private suspend fun applyOutcome(outcome: StoreOutcome, remembered: Boolean) {
        val decision = Entitlement.decide(outcome, remembered)
        licenseStore.remember(decision.rememberOwnership)
        _state.update { it.copy(premium = decision.premium, check = decision.check) }
    }

    private suspend fun queryStore(): StoreOutcome {
        if (!ensureConnected()) return StoreOutcome.Unreachable
        loadProductDetails()
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return StoreOutcome.Unreachable
        val mine = result.purchasesList.filter { productId in it.products }
        acknowledge(mine)
        return PlayPurchases.summarize(productId, mine.map { it.toSnapshot() })
    }

    private suspend fun ensureConnected(): Boolean = connectLock.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    private suspend fun loadProductDetails(): ProductDetails? {
        productDetails?.let { return it }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null
        val details = result.productDetailsList?.firstOrNull()
        productDetails = details
        _state.update { it.copy(priceText = details?.oneTimePurchaseOfferDetails?.formattedPrice) }
        return details
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val deferred = pendingPurchase
        pendingPurchase = null
        scope.launch {
            val code = result.responseCode
            when (code) {
                BillingClient.BillingResponseCode.OK -> {
                    val mine = purchases.orEmpty().filter { productId in it.products }
                    acknowledge(mine)
                    val snapshots = mine.map { it.toSnapshot() }
                    val outcome = PlayPurchases.summarize(productId, snapshots)
                    syncLock.withLock { applyOutcome(outcome, licenseStore.rememberedOwnership()) }
                    deferred?.complete(
                        when {
                            outcome is StoreOutcome.Answered && outcome.ownsProduct -> PurchaseResult.Success
                            outcome is StoreOutcome.Answered && outcome.purchasePending -> PurchaseResult.Pending
                            else -> PurchaseResult.Failed("no owned product in purchase result")
                        },
                    )
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> deferred?.complete(PurchaseResult.Cancelled)
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    sync()
                    deferred?.complete(PurchaseResult.Success)
                }
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> deferred?.complete(PurchaseResult.Unavailable)
                else -> deferred?.complete(PurchaseResult.Failed(result.debugMessage.orEmpty()))
            }
        }
    }

    private suspend fun acknowledge(purchases: List<Purchase>) {
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach { purchase ->
            // A failure here is retried at the next sync; the purchase stays valid meanwhile.
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
            )
        }
    }

    private fun Purchase.toSnapshot() = PurchaseSnapshot(
        products = products,
        purchased = purchaseState == Purchase.PurchaseState.PURCHASED,
        pending = purchaseState == Purchase.PurchaseState.PENDING,
        acknowledged = isAcknowledged,
    )
}
