package com.simpletools.core.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.simpletools.core.designsystem.icon.SimpleToolsIcons

/** Standard top bar. Pass [onNavigateBack] on secondary screens to show an accessible back button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleToolsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(SimpleToolsIcons.ArrowBack),
                        contentDescription = stringResource(R.string.core_ui_navigate_back),
                    )
                }
            }
        },
        actions = actions,
    )
}
