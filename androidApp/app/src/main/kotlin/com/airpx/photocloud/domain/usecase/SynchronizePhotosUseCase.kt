package com.airpx.photocloud.domain.usecase

import com.airpx.photocloud.domain.model.SyncSummary
import com.airpx.photocloud.domain.repository.SyncRepository

class SynchronizePhotosUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke(onProgress: suspend (SyncSummary) -> Unit = {}): SyncSummary {
        val photos = repository.scanPhotos()
        var uploaded = 0
        var failed = 0
        val missing = repository.missingHashes(photos.map { it.sha256 })
        val pending = photos.count { it.sha256 in missing }
        onProgress(SyncSummary(uploaded, pending, failed))
        photos.filter { it.sha256 in missing }.forEach { photo ->
            try { repository.upload(photo); uploaded++ } catch (_: Exception) { failed++ }
            onProgress(SyncSummary(uploaded, pending - uploaded - failed, failed))
        }
        return SyncSummary(uploaded, pending - uploaded - failed, failed)
    }
}
