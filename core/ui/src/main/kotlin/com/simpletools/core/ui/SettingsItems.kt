package com.simpletools.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.core.designsystem.theme.Spacing

/** Title of a group of settings, exposed as a heading to screen readers. */
@Composable
fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.sm)
            .semantics { heading() },
    )
}

/** A navigable or informative row. The whole row is the touch target. */
@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    @DrawableRes trailingIcon: Int? = SimpleToolsIcons.ChevronRight,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(painterResource(it), contentDescription = null) } },
        trailingContent = if (onClick != null && trailingIcon != null) {
            { Icon(painterResource(trailingIcon), contentDescription = null) }
        } else {
            null
        },
        modifier = if (onClick != null) modifier.clickable(role = Role.Button, onClick = onClick) else modifier,
    )
}

/** A row with a switch; the whole row toggles and is announced as a switch. */
@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(painterResource(it), contentDescription = null) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** One option of a single-choice group (e.g. theme). */
@Composable
fun SettingsRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
