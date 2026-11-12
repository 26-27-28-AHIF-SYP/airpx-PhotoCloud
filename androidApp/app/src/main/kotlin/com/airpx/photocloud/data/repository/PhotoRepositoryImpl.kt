package com.airpx.photocloud.data.repository

import com.airpx.photocloud.data.mediastore.MediaStoreScanner
import com.airpx.photocloud.domain.model.Album
import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.LibrarySortOrder
import com.airpx.photocloud.domain.model.PhotoItem
import com.airpx.photocloud.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PhotoRepositoryImpl(
    private val scanner: MediaStoreScanner
) : PhotoRepository {

    override fun getPhotos(
        sortOrder: LibrarySortOrder,
        filter: LibraryMediaFilter
    ): Flow<List<PhotoItem>> = flow {
        emit(scanner.scanPhotos(sortOrder, filter))
    }.flowOn(Dispatchers.IO)

    override fun getAlbums(): Flow<List<Album>> = flow {
        emit(scanner.scanAlbums())
    }.flowOn(Dispatchers.IO)

    override fun getAlbumPhotos(
        albumId: String,
        sortOrder: LibrarySortOrder
    ): Flow<List<PhotoItem>> = flow {
        emit(scanner.scanPhotos(sortOrder, LibraryMediaFilter.ALL))
    }.flowOn(Dispatchers.IO)

    override fun getFavorites(
        sortOrder: LibrarySortOrder
    ): Flow<List<PhotoItem>> = flow {
        val all = scanner.scanPhotos(sortOrder, LibraryMediaFilter.ALL)
        emit(all.filter { it.isFavorite })
    }.flowOn(Dispatchers.IO)
}
