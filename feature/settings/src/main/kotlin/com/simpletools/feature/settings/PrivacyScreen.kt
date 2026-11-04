package com.simpletools.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.core.privacy.PrivacySummary
import com.simpletools.core.ui.SettingsItem
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.core.ui.openExternalUrl

@Composable
fun PrivacyScreen(
    declaration: PrivacyDeclaration,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            SimpleToolsTopBar(title = stringResource(R.string.feature_settings_privacy), onNavigateBack = onNavigateBack)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = MaxContentWidth).verticalScroll(rememberScrollState())) {
                PrivacySummary(declaration)
                declaration.policyUrl?.let { url ->
                    SettingsItem(
                        title = stringResource(R.string.feature_settings_privacy_policy),
                        icon = SimpleToolsIcons.Info,
                        trailingIcon = SimpleToolsIcons.OpenInNew,
                        onClick = { context.openExternalUrl(url) },
                    )
                }
            }
        }
    }
}
