package com.simpletools.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** The product name (`app.name` in simpletools.properties), readable from any module. */
@Composable
fun rememberAppLabel(): String {
    val context = LocalContext.current
    return remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }
}
