package com.simpletools.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.designsystem.theme.TouchTarget

/** The main action of a screen: large, full width, impossible to miss. */
@Composable
fun SimpleToolsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget.primaryAction),
    ) {
        ButtonContent(text, icon)
    }
}

/** A secondary action that still deserves a visible container. */
@Composable
fun SimpleToolsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget.primaryAction),
    ) {
        ButtonContent(text, icon)
    }
}

/** A low emphasis action (e.g. "Not now"). */
@Composable
fun SimpleToolsTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = TouchTarget.minimum),
    ) {
        Text(text)
    }
}

@Composable
private fun ButtonContent(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        // Decorative: the button text already describes the action.
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.sm))
    }
    Text(text)
}
