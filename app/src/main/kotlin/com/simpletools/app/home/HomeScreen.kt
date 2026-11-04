package com.simpletools.app.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.simpletools.app.R
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.core.ui.EmptyState
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.core.ui.rememberAppLabel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(onOpenSettings: () -> Unit) {
    composable<HomeRoute> { HomeScreen(onOpenSettings = onOpenSettings) }
}

/**
 * Placeholder for the product's main utility. A derived app replaces this screen with its
 * own feature module (e.g. the PDF reader) and keeps the settings action in the top bar.
 */
@Composable
fun HomeScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SimpleToolsTopBar(
                title = rememberAppLabel(),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(SimpleToolsIcons.Settings),
                            contentDescription = stringResource(R.string.home_open_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        EmptyState(
            title = stringResource(R.string.home_placeholder_title),
            message = stringResource(R.string.home_placeholder_message),
            icon = SimpleToolsIcons.Check,
            modifier = Modifier.padding(padding),
        )
    }
}
