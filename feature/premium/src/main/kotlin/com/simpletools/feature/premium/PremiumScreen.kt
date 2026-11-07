package com.simpletools.feature.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simpletools.core.billing.BillingState
import com.simpletools.core.billing.PremiumState
import com.simpletools.core.billing.StoreCheck
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.component.SimpleToolsSecondaryButton
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.ui.SimpleToolsTopBar

const val PURCHASE_BUTTON_TAG = "pro_purchase_button"
const val RESTORE_BUTTON_TAG = "pro_restore_button"

/**
 * SimpleTools Scan PRO. Shows only what the app really offers; the price is the one Google Play returns;
 * the licence state says how it was established, and a missing check is never shown as a purchase.
 */
@Composable
fun PremiumScreen(
    state: BillingState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetryCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val busy = state.check == StoreCheck.Checking
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { SimpleToolsTopBar(title = stringResource(R.string.pro_title), onNavigateBack = onNavigateBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .widthIn(max = MaxContentWidth)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(R.string.pro_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.pro_tagline), style = MaterialTheme.typography.bodyLarge)

            LicenseStatus(state = state, onRetryCheck = onRetryCheck)

            ComparisonTable()

            PriceBlock(state = state)

            SimpleToolsPrimaryButton(
                text = stringResource(R.string.pro_buy),
                onClick = onPurchase,
                enabled = !state.isPremium && state.priceText != null && !busy,
                modifier = Modifier.fillMaxWidth().testTag(PURCHASE_BUTTON_TAG),
            )
            SimpleToolsSecondaryButton(
                text = stringResource(R.string.pro_restore),
                onClick = onRestore,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag(RESTORE_BUTTON_TAG),
            )

            PrivacyBlock()
        }
    }
}

@Composable
private fun LicenseStatus(state: BillingState, onRetryCheck: () -> Unit) {
    val (text, active) = when {
        state.premium == PremiumState.Purchased && state.check == StoreCheck.RememberedOffline ->
            stringResource(R.string.pro_status_offline) to true
        state.premium == PremiumState.Purchased -> stringResource(R.string.pro_status_active) to true
        state.premium == PremiumState.Pending -> stringResource(R.string.pro_status_pending) to false
        state.check == StoreCheck.Checking || state.check == StoreCheck.NotChecked ->
            stringResource(R.string.pro_status_checking) to false
        state.check == StoreCheck.Unreachable -> stringResource(R.string.pro_status_unreachable) to false
        state.premium == PremiumState.Unavailable -> stringResource(R.string.pro_status_unavailable) to false
        else -> stringResource(R.string.pro_status_not_purchased) to false
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth().testTag(LICENSE_STATUS_TAG),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (active) {
                    Icon(painterResource(SimpleToolsIcons.Check), contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            if (state.check == StoreCheck.Unreachable || state.premium == PremiumState.NotPurchased && state.check != StoreCheck.Checking) {
                TextButton(onClick = onRetryCheck) { Text(stringResource(R.string.pro_retry)) }
            }
        }
    }
}

@Composable
private fun ComparisonTable() {
    Text(stringResource(R.string.pro_compare_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.pro_column_free), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
        Text(stringResource(R.string.pro_column_pro), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
    }
    FEATURES.forEach { feature ->
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(feature.label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Mark(included = feature.free)
            Mark(included = feature.pro)
        }
    }
}

@Composable
private fun Mark(included: Boolean) {
    val includedLabel = stringResource(R.string.pro_included)
    val notIncludedLabel = stringResource(R.string.pro_not_included)
    Row(Modifier.width(72.dp), horizontalArrangement = Arrangement.Center) {
        if (included) {
            Icon(painterResource(SimpleToolsIcons.Check), contentDescription = includedLabel)
        } else {
            Text(
                stringResource(R.string.pro_mark_none),
                modifier = Modifier.semantics { contentDescription = notIncludedLabel },
            )
        }
    }
}

@Composable
private fun PriceBlock(state: BillingState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(R.string.pro_price_label), style = MaterialTheme.typography.labelLarge)
        Text(
            state.priceText ?: stringResource(R.string.pro_price_unknown),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(PRICE_TAG),
        )
        Text(stringResource(R.string.pro_price_note), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrivacyBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(R.string.pro_privacy_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.pro_privacy_body), style = MaterialTheme.typography.bodyMedium)
    }
}

private data class Feature(val label: Int, val free: Boolean, val pro: Boolean)

private val FEATURES = listOf(
    Feature(R.string.pro_row_scan, free = true, pro = true),
    Feature(R.string.pro_row_edges, free = true, pro = true),
    Feature(R.string.pro_row_filters, free = true, pro = true),
    Feature(R.string.pro_row_document, free = true, pro = true),
    Feature(R.string.pro_row_offline, free = true, pro = true),
    Feature(R.string.pro_row_ocr, free = false, pro = true),
    Feature(R.string.pro_row_copy_search, free = false, pro = true),
    Feature(R.string.pro_row_searchable_pdf, free = false, pro = true),
    Feature(R.string.pro_row_contrast, free = false, pro = true),
)

const val LICENSE_STATUS_TAG = "pro_license_status"
const val PRICE_TAG = "pro_price"
