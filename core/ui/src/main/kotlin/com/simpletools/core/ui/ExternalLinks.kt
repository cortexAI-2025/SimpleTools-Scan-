package com.simpletools.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens [url] in the user's browser. The app itself needs no INTERNET permission for this.
 * Returns false when no app can handle the link.
 */
fun Context.openExternalUrl(url: String): Boolean = startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

/** Starts [intent], returning false instead of crashing when nothing can handle it. */
fun Context.startActivitySafely(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}
