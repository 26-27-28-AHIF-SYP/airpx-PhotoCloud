package com.airpx.photocloud.domain.usecase

import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.LibrarySortOrder
import com.airpx.photocloud.domain.model.PhotoItem
import com.airpx.photocloud.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow

class GetPhotosUseCase(
    private val repository: PhotoRepository
) {
    operator fun invoke(
        sortOrder: LibrarySortOrder = LibrarySortOrder.CAPTURE_DATE,
        filter: LibraryMediaFilter = LibraryMediaFilter.ALL
    ): Flow<List<PhotoItem>> {
        return repository.getPhotos(sortOrder, filter)
    }
}
