package com.airpx.photocloud

import com.airpx.photocloud.domain.model.SyncPhoto
import com.airpx.photocloud.domain.repository.SyncRepository
import com.airpx.photocloud.domain.usecase.SynchronizePhotosUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SynchronizePhotosUseCaseTest {
    @Test fun uploadsOnlyHashesReportedMissingAndReportsProgress() = runTest {
        val photos = listOf(photo("a"), photo("b"), photo("c"))
        val uploaded = mutableListOf<String>()
        val repository = object : SyncRepository {
            override suspend fun scanPhotos() = photos
            override suspend fun missingHashes(hashes: List<String>) = setOf("a", "c")
            override suspend fun upload(photo: SyncPhoto) { uploaded += photo.sha256 }
        }
        val progress = mutableListOf<Int>()
        val useCase = SynchronizePhotosUseCase(repository)
        val summary = useCase { progress += it.uploaded }
        assertEquals(listOf("a", "c"), uploaded)
        assertEquals(2, summary.uploaded)
        assertEquals(0, summary.pending)
        assertEquals(listOf(0, 1, 2), progress)
    }

    @Test fun reportsFailedUploadsSeparatelyFromPendingCount() = runTest {
        val repository = object : SyncRepository {
            override suspend fun scanPhotos() = listOf(photo("a"))
            override suspend fun missingHashes(hashes: List<String>) = setOf("a")
            override suspend fun upload(photo: SyncPhoto) { error("network unavailable") }
        }

        var reportedFailure: Exception? = null
        val summary = SynchronizePhotosUseCase(repository)(onUploadFailure = { _, error -> reportedFailure = error })

        assertEquals(0, summary.uploaded)
        assertEquals(0, summary.pending)
        assertEquals(1, summary.failed)
        assertEquals("network unavailable", reportedFailure?.message)
    }

    @Test fun reportsScannedPhotosAsPendingThenUploaded() = runTest {
        val photos = (1..4).map { photo("hash-$it") }
        val repository = object : SyncRepository {
            override suspend fun scanPhotos() = photos
            override suspend fun missingHashes(hashes: List<String>) = hashes.toSet()
            override suspend fun upload(photo: SyncPhoto) = Unit
        }
        val statuses = mutableListOf<com.airpx.photocloud.domain.model.SyncSummary>()

        val summary = SynchronizePhotosUseCase(repository)(onProgress = { statuses += it })

        assertEquals(4, statuses.first().found)
        assertEquals(4, statuses.first().pending)
        assertEquals(4, summary.found)
        assertEquals(4, summary.uploaded)
        assertEquals(0, summary.pending)
        assertEquals(4, statuses.last().uploaded)
    }

    private fun photo(hash: String) = SyncPhoto("content://$hash", "$hash.jpg", null, "image/jpeg", 1, hash)
}
