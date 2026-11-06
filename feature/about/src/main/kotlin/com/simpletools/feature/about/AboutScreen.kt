package com.simpletools.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.ui.SettingsItem
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.core.ui.openExternalUrl
import com.simpletools.core.ui.rememberAppLabel

@Composable
fun AboutScreen(
    versionName: String,
    privacyPolicyUrl: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = { SimpleToolsTopBar(title = stringResource(R.string.feature_about_title), onNavigateBack = onNavigateBack) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = rememberAppLabel(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.feature_about_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.feature_about_tagline),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                    Text(
                        text = stringResource(R.string.feature_about_family),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (privacyPolicyUrl != null) {
                    SettingsItem(
                        title = stringResource(R.string.feature_about_privacy_policy),
                        icon = SimpleToolsIcons.Lock,
                        trailingIcon = SimpleToolsIcons.OpenInNew,
                        onClick = { context.openExternalUrl(privacyPolicyUrl) },
                    )
                }
            }
        }
    }
}
