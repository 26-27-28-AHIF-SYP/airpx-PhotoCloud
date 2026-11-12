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

    private fun photo(hash: String) = SyncPhoto("content://$hash", "$hash.jpg", null, "image/jpeg", 1, hash)
}
