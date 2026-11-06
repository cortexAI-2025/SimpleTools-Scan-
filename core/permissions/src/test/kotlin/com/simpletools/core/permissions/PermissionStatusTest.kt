package com.simpletools.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun `granted wins over everything`() {
        assertEquals(PermissionStatus.Granted, PermissionStatus.resolve(granted = true, shouldShowRationale = true, hasRequested = true))
    }

    @Test
    fun `first launch is not requested`() {
        assertEquals(PermissionStatus.NotRequested, PermissionStatus.resolve(granted = false, shouldShowRationale = false, hasRequested = false))
    }

    @Test
    fun `refused once asks for a rationale`() {
        assertEquals(PermissionStatus.Denied, PermissionStatus.resolve(granted = false, shouldShowRationale = true, hasRequested = true))
    }

    @Test
    fun `refused without rationale after a request is permanent`() {
        assertEquals(PermissionStatus.PermanentlyDenied, PermissionStatus.resolve(granted = false, shouldShowRationale = false, hasRequested = true))
    }
}
