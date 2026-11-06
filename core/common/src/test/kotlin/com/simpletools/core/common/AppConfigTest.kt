package com.simpletools.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {

    @Test
    fun `parseList trims values and ignores empty entries`() {
        assertEquals(listOf("en", "fr"), AppConfig.parseList(" en, ,fr ,"))
        assertEquals(emptyList<String>(), AppConfig.parseList(""))
    }
}
