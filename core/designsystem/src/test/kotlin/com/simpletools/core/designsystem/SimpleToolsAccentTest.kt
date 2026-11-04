package com.simpletools.core.designsystem

import com.simpletools.core.designsystem.theme.SimpleToolsAccent
import com.simpletools.core.designsystem.theme.colorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SimpleToolsAccentTest {

    @Test
    fun `accent keys match the values accepted by simpletools properties`() {
        assertEquals(
            listOf("blue", "teal", "green", "orange", "red", "purple"),
            SimpleToolsAccent.entries.map { it.key },
        )
    }

    @Test
    fun `unknown accent falls back to blue`() {
        assertEquals(SimpleToolsAccent.Blue, SimpleToolsAccent.fromKey("pink"))
        assertEquals(SimpleToolsAccent.Teal, SimpleToolsAccent.fromKey("teal"))
    }

    @Test
    fun `light and dark schemes use different primary tones`() {
        SimpleToolsAccent.entries.forEach { accent ->
            assertNotEquals(accent.colorScheme(darkTheme = false).primary, accent.colorScheme(darkTheme = true).primary)
        }
    }
}
