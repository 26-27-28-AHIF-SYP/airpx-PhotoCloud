package com.airpx.photocloud.domain.usecase

import com.airpx.photocloud.domain.model.SyncSummary
import com.airpx.photocloud.domain.model.SyncPhoto
import com.airpx.photocloud.domain.repository.SyncRepository

class SynchronizePhotosUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke(
        onUploadFailure: suspend (SyncPhoto, Exception) -> Unit = { _, error -> throw error },
        onProgress: suspend (SyncSummary) -> Unit = {}
    ): SyncSummary {
        val photos = repository.scanPhotos()
        var uploaded = 0
        var failed = 0
        val missing = repository.missingHashes(photos.map { it.sha256 })
        val toUpload = photos.filter { it.sha256 in missing }.distinctBy { it.sha256 }
        val pending = toUpload.size
        onProgress(SyncSummary(photos.size, uploaded, pending, failed))
        toUpload.forEach { photo ->
            try {
                repository.upload(photo)
                uploaded++
            } catch (error: Exception) {
                failed++
                onUploadFailure(photo, error)
            }
            onProgress(SyncSummary(photos.size, uploaded, pending - uploaded - failed, failed))
        }
        return SyncSummary(photos.size, uploaded, pending - uploaded - failed, failed)
    }
}
