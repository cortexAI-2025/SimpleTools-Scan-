package com.simpletools.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionAuditTest {

    private val appId = "com.simpletools.qr.debug"

    @Test
    fun `ignores permissions scoped to the application itself`() {
        val result = PermissionAudit.audit(
            requested = listOf("$appId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"),
            declared = emptyList(),
            applicationId = appId,
        )

        assertTrue(result.isClean)
    }

    @Test
    fun `flags undeclared and unused permissions`() {
        val result = PermissionAudit.audit(
            requested = listOf("android.permission.CAMERA", "android.permission.INTERNET"),
            declared = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO"),
            applicationId = appId,
        )

        assertEquals(listOf("android.permission.INTERNET"), result.undeclared)
        assertEquals(listOf("android.permission.RECORD_AUDIO"), result.unused)
    }
}
