package com.airpx.photocloud.domain.repository

import com.airpx.photocloud.domain.model.SyncPhoto

interface SyncRepository {
    suspend fun scanPhotos(): List<SyncPhoto>
    suspend fun missingHashes(hashes: List<String>): Set<String>
    suspend fun upload(photo: SyncPhoto)
}
