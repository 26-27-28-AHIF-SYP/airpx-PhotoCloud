package com.airpx.photocloud.ui.screens.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airpx.photocloud.PhotoPermissionStatus
import com.airpx.photocloud.data.PhotoSyncScheduler
import com.airpx.photocloud.data.SyncSettings
import androidx.work.WorkManager
import kotlinx.coroutines.launch

@Composable
fun AutoBackupScreen(
    permissionStatus: PhotoPermissionStatus,
    onRequestPermission: () -> Unit,
    onSelectMorePhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = remember { SyncSettings(context) }
    val scope = rememberCoroutineScope()
    var isAutoBackupEnabled by remember { mutableStateOf(false) }
    var wifiOnly by remember { mutableStateOf(false) }
    var signedIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        wifiOnly = settings.wifiOnly()
        signedIn = settings.token() != null && settings.server().isNotBlank()
        isAutoBackupEnabled = settings.automatic()
        PhotoSyncScheduler.setAutomatic(context, isAutoBackupEnabled, wifiOnly)
    }
    val workInfos by WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(PhotoSyncScheduler.IMMEDIATE)
        .collectAsState(initial = emptyList())
    val work = workInfos.firstOrNull()
    val progress = work?.progress
    val uploaded = progress?.getInt("uploaded", work.outputData.getInt("uploaded", 0)) ?: 0
    val pending = progress?.getInt("pending", work.outputData.getInt("pending", 0)) ?: 0
    val failed = progress?.getInt("failed", work.outputData.getInt("failed", 0)) ?: 0

    val hasAnyPermission = permissionStatus != PhotoPermissionStatus.DENIED

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Auto-Backup",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isAutoBackupEnabled,
                enabled = signedIn && hasAnyPermission,
                onCheckedChange = {
                    isAutoBackupEnabled = it
                    scope.launch { settings.saveAutomatic(it) }
                    PhotoSyncScheduler.setAutomatic(context, it, wifiOnly)
                }
            )
        }
        Text(
            text = "Deine Medien werden nicht hochgeladen, solange Auto-Backup deaktiviert ist.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nur WLAN", modifier = Modifier.weight(1f))
            Switch(checked = wifiOnly, onCheckedChange = {
                wifiOnly = it
                scope.launch { settings.saveWifiOnly(it) }
                PhotoSyncScheduler.setAutomatic(context, isAutoBackupEnabled, it)
            })
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Berechtigung",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (permissionStatus) {
                PhotoPermissionStatus.FULL -> "Vollständiger Medien-Zugriff erteilt"
                PhotoPermissionStatus.PARTIAL -> "Teilweiser Medien-Zugriff erteilt (nur ausgewählte Fotos)"
                PhotoPermissionStatus.DENIED -> "Zugriff nicht erteilt"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
        when (permissionStatus) {
            PhotoPermissionStatus.DENIED -> {
                Button(onClick = onRequestPermission) {
                    Text("Zugriff erlauben")
                }
            }
            PhotoPermissionStatus.PARTIAL -> {
                Row {
                    OutlinedButton(onClick = onSelectMorePhotos) {
                        Text("Weitere Fotos freigeben")
                    }
                }
            }
            PhotoPermissionStatus.FULL -> { /* No extra action needed */ }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sync Status",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Ausstehend:", modifier = Modifier.weight(1f))
            Text(pending.toString())
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Hochgeladen:", modifier = Modifier.weight(1f))
            Text(uploaded.toString())
        }
        if (failed > 0) Text("Fehlgeschlagen: $failed", color = MaterialTheme.colorScheme.error)
        if (work?.state?.isFinished == true && work.outputData.getString("message") != null) {
            Text(work.outputData.getString("message")!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { PhotoSyncScheduler.syncNow(context, wifiOnly) },
            enabled = signedIn && hasAnyPermission && work?.state?.isFinished != false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Jetzt synchronisieren")
        }
    }
}
