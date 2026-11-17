package com.airpx.photocloud.ui.screens.sync

import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualSyncUiStateTest {
    @Test
    fun manualSyncRequiresLoginAndMediaPermissionOnly() {
        assertTrue(canStartManualSync(signedIn = true, hasMediaPermission = true))
        assertFalse(canStartManualSync(signedIn = false, hasMediaPermission = true))
        assertFalse(canStartManualSync(signedIn = true, hasMediaPermission = false))
    }

    @Test
    fun warningOnlyDescribesRunningOrBlockedManualWork() {
        assertEquals("Eine Synchronisierung läuft bereits.", manualSyncWarning(WorkInfo.State.RUNNING))
        assertEquals(
            "Die Synchronisierung wartet auf eine vorausgesetzte Aufgabe.",
            manualSyncWarning(WorkInfo.State.BLOCKED)
        )
        assertNull(manualSyncWarning(WorkInfo.State.ENQUEUED))
        assertNull(manualSyncWarning(WorkInfo.State.SUCCEEDED))
        assertNull(manualSyncWarning(null))
    }
}
