package com.simpletools.core.permissions

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPermissionTest {

    @Test
    fun `notifications are only requested from Android 13`() {
        assertTrue(AppPermission.NOTIFICATIONS.runtimePermissions(Build.VERSION_CODES.S_V2).isEmpty())
        assertEquals(
            listOf("android.permission.POST_NOTIFICATIONS"),
            AppPermission.NOTIFICATIONS.runtimePermissions(Build.VERSION_CODES.TIRAMISU),
        )
    }

    @Test
    fun `activity recognition is only requested from Android 10`() {
        assertTrue(AppPermission.ACTIVITY_RECOGNITION.runtimePermissions(Build.VERSION_CODES.P).isEmpty())
        assertEquals(1, AppPermission.ACTIVITY_RECOGNITION.runtimePermissions(Build.VERSION_CODES.Q).size)
    }

    @Test
    fun `camera and microphone are runtime permissions on every supported version`() {
        assertEquals(listOf("android.permission.CAMERA"), AppPermission.CAMERA.runtimePermissions(26))
        assertEquals(listOf("android.permission.RECORD_AUDIO"), AppPermission.MICROPHONE.runtimePermissions(26))
    }

    @Test
    fun `maps manifest names back to capabilities`() {
        assertEquals(AppPermission.CAMERA, AppPermission.fromManifestName("android.permission.CAMERA"))
        assertNull(AppPermission.fromManifestName("android.permission.INTERNET"))
    }
}
