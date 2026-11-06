package com.simpletools.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.core.privacy.PrivacySummary
import com.simpletools.core.ui.rememberAppLabel

@Composable
fun OnboardingScreen(
    privacy: PrivacyDeclaration,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(Spacing.md), contentAlignment = Alignment.Center) {
                SimpleToolsPrimaryButton(
                    text = stringResource(R.string.feature_onboarding_start),
                    onClick = onFinished,
                    modifier = Modifier.widthIn(max = MaxContentWidth),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.feature_onboarding_title, rememberAppLabel()),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = Spacing.md).semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.feature_onboarding_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )
                PrivacySummary(
                    declaration = privacy,
                    showPermissions = false,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
        }
    }
}
