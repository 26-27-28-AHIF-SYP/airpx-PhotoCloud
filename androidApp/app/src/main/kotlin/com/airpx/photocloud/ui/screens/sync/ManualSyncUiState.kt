package com.airpx.photocloud.ui.screens.sync

import androidx.work.WorkInfo

internal fun canStartManualSync(signedIn: Boolean, hasMediaPermission: Boolean): Boolean =
    signedIn && hasMediaPermission

internal fun manualSyncWarning(state: WorkInfo.State?): String? = when (state) {
    WorkInfo.State.RUNNING -> "Eine Synchronisierung läuft bereits."
    WorkInfo.State.BLOCKED -> "Die Synchronisierung wartet auf eine vorausgesetzte Aufgabe."
    else -> null
}
