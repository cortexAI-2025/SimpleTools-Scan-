package com.simpletools.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTest {

    private class RecordingTracker : AnalyticsTracker {
        val events = mutableListOf<AnalyticsEvent>()
        override fun track(event: AnalyticsEvent) {
            events += event
        }
    }

    @Test
    fun `gated tracker forwards events only when enabled`() {
        val recorder = RecordingTracker()
        var enabled = false
        val tracker = GatedAnalyticsTracker(recorder) { enabled }

        tracker.track(AnalyticsEvent("screen_opened"))
        enabled = true
        tracker.track(AnalyticsEvent("file_shared"))

        assertEquals(listOf("file_shared"), recorder.events.map { it.name })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `event names must be snake case`() {
        AnalyticsEvent("Screen Opened")
    }
}
