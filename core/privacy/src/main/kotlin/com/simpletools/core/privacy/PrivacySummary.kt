package com.simpletools.core.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.core.ui.SettingsItem
import com.simpletools.core.ui.SettingsSectionHeader

/**
 * Plain-language list of the product's privacy commitments.
 *
 * @param showPermissions also list the declared permissions with their purpose.
 */
@Composable
fun PrivacySummary(
    declaration: PrivacyDeclaration,
    modifier: Modifier = Modifier,
    showPermissions: Boolean = true,
) {
    Column(modifier = modifier) {
        PrivacyStatement(stringResource(R.string.core_privacy_no_account))
        PrivacyStatement(
            stringResource(
                if (declaration.localOnly) R.string.core_privacy_local_only else R.string.core_privacy_uses_network,
            ),
        )
        PrivacyStatement(
            stringResource(if (declaration.analytics) R.string.core_privacy_analytics else R.string.core_privacy_no_analytics),
        )
        PrivacyStatement(stringResource(if (declaration.ads) R.string.core_privacy_ads else R.string.core_privacy_no_ads))

        if (!showPermissions) return@Column
        SettingsSectionHeader(stringResource(R.string.core_privacy_permissions_title))
        if (declaration.permissions.isEmpty() && declaration.otherPermissions.isEmpty()) {
            PrivacyStatement(stringResource(R.string.core_privacy_no_permissions))
        }
        declaration.permissions.forEach { permission ->
            SettingsItem(
                title = stringResource(permission.titleRes),
                summary = stringResource(permission.rationaleRes),
                icon = SimpleToolsIcons.Lock,
            )
        }
        declaration.otherPermissions.forEach { name ->
            SettingsItem(title = name, icon = SimpleToolsIcons.Lock)
        }
    }
}

@Composable
private fun PrivacyStatement(text: String) {
    SettingsItem(title = text, icon = SimpleToolsIcons.Check)
}
