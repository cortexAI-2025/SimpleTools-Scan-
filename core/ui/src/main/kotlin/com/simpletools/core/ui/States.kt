package com.simpletools.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.designsystem.theme.Spacing

/**
 * Centered message with an optional primary action. Used for empty, first-run and
 * "nothing opened yet" screens: the action should be the app's main utility.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    @DrawableRes icon: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize().padding(Spacing.lg), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = MaxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                SimpleToolsPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.core_ui_loading)
    Box(
        modifier = modifier.fillMaxSize().semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.core_ui_error_generic),
    onRetry: (() -> Unit)? = null,
) {
    EmptyState(
        title = message,
        modifier = modifier,
        actionLabel = onRetry?.let { stringResource(R.string.core_ui_retry) },
        onAction = onRetry,
    )
}
