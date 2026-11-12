package com.airpx.photocloud.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.airpx.photocloud.domain.usecase.SynchronizePhotosUseCase
import java.io.IOException
import java.util.concurrent.TimeUnit

class PhotoSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        setProgress(workDataOf())
        val settings = SyncSettings(applicationContext)
        val base = settings.server()
        val token = settings.token() ?: return Result.failure(workDataOf("message" to "Bitte anmelden"))
        if (base.isBlank()) return Result.failure(workDataOf("message" to "Server-URL fehlt"))
        return try {
            val useCase = SynchronizePhotosUseCase(PhotoSyncRepository(applicationContext, PhotoCloudApi(base, token)))
            val summary = useCase { status ->
                setProgress(workDataOf("uploaded" to status.uploaded, "pending" to status.pending, "failed" to status.failed))
            }
            if (summary.failed > 0) {
                setProgress(workDataOf(
                    "uploaded" to summary.uploaded,
                    "pending" to summary.pending,
                    "failed" to summary.failed,
                    "message" to "${summary.failed} Foto(s) konnten nicht hochgeladen werden. Netzwerk und Server prüfen."
                ))
                Result.retry()
            } else {
                Result.success(workDataOf(
                    "uploaded" to summary.uploaded,
                    "pending" to summary.pending,
                    "failed" to summary.failed
                ))
            }
        } catch (e: Exception) {
            val message = if (e is IOException) {
                "Server nicht erreichbar. URL und Netzwerk prüfen."
            } else {
                e.message ?: "Synchronisierung fehlgeschlagen."
            }
            setProgress(workDataOf("message" to message))
            Result.retry()
        }
    }
}

object PhotoSyncScheduler {
    const val IMMEDIATE = "photo-sync-immediate"
    const val AUTOMATIC = "photo-sync-automatic"
    fun syncNow(context: Context, wifiOnly: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<PhotoSyncWorker>().setConstraints(constraints(wifiOnly))
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.KEEP, request)
    }
    fun setAutomatic(context: Context, enabled: Boolean, wifiOnly: Boolean = false) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) { manager.cancelUniqueWork(AUTOMATIC); return }
        val request = PeriodicWorkRequestBuilder<PhotoSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints(wifiOnly)).setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        manager.enqueueUniquePeriodicWork(AUTOMATIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
    private fun constraints(wifiOnly: Boolean) = Constraints.Builder()
        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build()
}
