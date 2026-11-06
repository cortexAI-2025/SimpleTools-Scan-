package com.simpletools.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Spacing scale (4dp grid). Use these instead of ad-hoc values to keep screens consistent. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Horizontal padding of screen content. */
    val screenHorizontal = 16.dp
}

/** Touch target sizes. SimpleTools uses larger primary actions than the 48dp minimum. */
object TouchTarget {
    val minimum = 48.dp
    val primaryAction = 56.dp
}

/** Maximum width of reading content on tablets/foldables. */
val MaxContentWidth = 640.dp
