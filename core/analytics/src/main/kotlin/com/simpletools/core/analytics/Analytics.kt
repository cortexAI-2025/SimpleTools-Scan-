package com.simpletools.core.analytics

/**
 * Optional, provider-agnostic analytics.
 *
 * SimpleTools apps are local-first: the default is [NoOpAnalyticsTracker] and no
 * analytics SDK is shipped. A product that really needs analytics adds a provider
 * module implementing [AnalyticsTracker], sets `privacy.analytics=true` (which
 * requires `privacy.localOnly=false`) and documents it in its privacy disclosure.
 * Product features must never depend on analytics being enabled.
 */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/**
 * A coarse product event. Never put user content (file names, text, locations…) in [params].
 */
data class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
) {
    init {
        require(NAME.matches(name)) { "Event name must be snake_case, 1-40 chars: '$name'" }
    }

    private companion object {
        val NAME = Regex("^[a-z][a-z0-9_]{0,39}$")
    }
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}

/** Forwards events only while [isEnabled] returns true (configuration flag and/or user consent). */
class GatedAnalyticsTracker(
    private val delegate: AnalyticsTracker,
    private val isEnabled: () -> Boolean,
) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        if (isEnabled()) delegate.track(event)
    }
}
