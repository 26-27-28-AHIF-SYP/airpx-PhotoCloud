package com.airpx.photocloud

import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.LibrarySortOrder
import com.airpx.photocloud.domain.model.MediaType
import com.airpx.photocloud.domain.model.PhotoItem
import com.airpx.photocloud.domain.repository.PhotoRepository
import com.airpx.photocloud.domain.usecase.GetPhotosUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FakePhotoRepository : PhotoRepository {
    private val fakePhotos = listOf(
        PhotoItem(
            id = "1",
            contentUri = "content://media/1",
            creationTimestamp = Instant.ofEpochSecond(1000),
            addedTimestamp = Instant.ofEpochSecond(1000),
            modificationTimestamp = Instant.ofEpochSecond(1000),
            location = null,
            width = 1080,
            height = 1920,
            mediaType = MediaType.PHOTO,
            isFavorite = false
        ),
        PhotoItem(
            id = "2",
            contentUri = "content://media/2",
            creationTimestamp = Instant.ofEpochSecond(2000),
            addedTimestamp = Instant.ofEpochSecond(2000),
            modificationTimestamp = Instant.ofEpochSecond(2000),
            location = null,
            width = 1920,
            height = 1080,
            mediaType = MediaType.VIDEO,
            isFavorite = true
        )
    )

    override fun getPhotos(
        sortOrder: LibrarySortOrder,
        filter: LibraryMediaFilter
    ): Flow<List<PhotoItem>> {
        val filtered = when (filter) {
            LibraryMediaFilter.ALL -> fakePhotos
            LibraryMediaFilter.PHOTOS -> fakePhotos.filter { it.mediaType == MediaType.PHOTO }
            LibraryMediaFilter.VIDEOS -> fakePhotos.filter { it.mediaType == MediaType.VIDEO }
        }
        return flowOf(filtered)
    }

    override fun getAlbums() = flowOf(emptyList<com.airpx.photocloud.domain.model.Album>())
    override fun getAlbumPhotos(albumId: String, sortOrder: LibrarySortOrder) = flowOf(fakePhotos)
    override fun getFavorites(sortOrder: LibrarySortOrder) = flowOf(fakePhotos.filter { it.isFavorite })
}

class GetPhotosUseCaseTest {

    private val repository = FakePhotoRepository()
    private val useCase = GetPhotosUseCase(repository)

    @Test
    fun getPhotos_all_returnsAllItems() = runTest {
        val result = useCase(LibrarySortOrder.CAPTURE_DATE, LibraryMediaFilter.ALL).first()
        assertEquals(2, result.size)
    }

    @Test
    fun getPhotos_photosOnly_returnsOnlyPhotos() = runTest {
        val result = useCase(LibrarySortOrder.CAPTURE_DATE, LibraryMediaFilter.PHOTOS).first()
        assertEquals(1, result.size)
        assertEquals(MediaType.PHOTO, result.first().mediaType)
    }

    @Test
    fun getPhotos_videosOnly_returnsOnlyVideos() = runTest {
        val result = useCase(LibrarySortOrder.CAPTURE_DATE, LibraryMediaFilter.VIDEOS).first()
        assertEquals(1, result.size)
        assertEquals(MediaType.VIDEO, result.first().mediaType)
    }
}
