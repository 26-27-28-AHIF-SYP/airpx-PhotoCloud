package com.airpx.photocloud.domain.repository

import com.airpx.photocloud.domain.model.Album
import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.LibrarySortOrder
import com.airpx.photocloud.domain.model.PhotoItem
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getPhotos(sortOrder: LibrarySortOrder, filter: LibraryMediaFilter): Flow<List<PhotoItem>>
    fun getAlbums(): Flow<List<Album>>
    fun getAlbumPhotos(albumId: String, sortOrder: LibrarySortOrder): Flow<List<PhotoItem>>
    fun getFavorites(sortOrder: LibrarySortOrder): Flow<List<PhotoItem>>
}
