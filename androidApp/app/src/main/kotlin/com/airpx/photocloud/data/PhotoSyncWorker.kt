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
import android.util.Log

class PhotoSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private var foundCount = 0
    private var uploadedCount = 0
    private var pendingCount = 0
    private var failedCount = 0
    override suspend fun doWork(): Result {
        setProgress(workDataOf())
        val settings = SyncSettings(applicationContext)
        val base = settings.server()
        val token = settings.token() ?: return Result.failure(workDataOf("message" to "Bitte anmelden"))
        if (base.isBlank()) return Result.failure(workDataOf("message" to "Server-URL fehlt"))
        return try {
            val useCase = SynchronizePhotosUseCase(PhotoSyncRepository(applicationContext, PhotoCloudApi(base, token)))
            var previousUploaded = 0
            val summary = useCase(onProgress = { status ->
                foundCount = status.found
                uploadedCount = status.uploaded
                pendingCount = status.pending
                failedCount = status.failed
                Log.d("PhotoSync", "Progress found=${status.found} missing=${status.pending + status.uploaded + status.failed} uploaded=${status.uploaded} failed=${status.failed}")
                if (status.uploaded > previousUploaded) Log.d("PhotoSync", "Uploaded count=${status.uploaded}")
                previousUploaded = status.uploaded
                setProgress(workDataOf("found" to status.found, "uploaded" to status.uploaded, "pending" to status.pending, "failed" to status.failed))
            }, onUploadFailure = { _, error ->
                Log.e("PhotoSync", "Photo upload failed", error)
            })
            Log.d("PhotoSync", "Sync complete found=${summary.found} uploaded=${summary.uploaded} failed=${summary.failed}")
            if (summary.failed > 0) {
                setProgress(workDataOf(
                    "found" to summary.found,
                    "uploaded" to summary.uploaded,
                    "pending" to summary.pending,
                    "failed" to summary.failed,
                    "message" to "${summary.failed} Foto(s) konnten nicht hochgeladen werden. Netzwerk und Server prüfen."
                ))
                Result.retry()
            } else {
                Result.success(workDataOf(
                    "found" to summary.found,
                    "uploaded" to summary.uploaded,
                    "pending" to summary.pending,
                    "failed" to summary.failed
                ))
            }
        } catch (e: Exception) {
            Log.e("PhotoSync", "Sync run failed", e)
            val message = if (e is IOException) {
                "Server nicht erreichbar. URL und Netzwerk prüfen."
            } else {
                e.message ?: "Synchronisierung fehlgeschlagen."
            }
            setProgress(workDataOf("found" to foundCount, "uploaded" to uploadedCount, "pending" to pendingCount, "failed" to maxOf(1, failedCount), "message" to message))
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
