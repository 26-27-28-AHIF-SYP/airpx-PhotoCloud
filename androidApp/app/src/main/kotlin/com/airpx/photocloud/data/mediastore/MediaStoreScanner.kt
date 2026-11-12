package com.airpx.photocloud.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.airpx.photocloud.domain.model.Album
import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.LibrarySortOrder
import com.airpx.photocloud.domain.model.Location
import com.airpx.photocloud.domain.model.MediaType
import com.airpx.photocloud.domain.model.PhotoItem
import java.time.Instant

class MediaStoreScanner(
    private val context: Context
) {
    fun scanPhotos(
        sortOrder: LibrarySortOrder = LibrarySortOrder.CAPTURE_DATE,
        filter: LibraryMediaFilter = LibraryMediaFilter.ALL
    ): List<PhotoItem> {
        val items = mutableListOf<PhotoItem>()

        if (filter == LibraryMediaFilter.ALL || filter == LibraryMediaFilter.PHOTOS) {
            items.addAll(queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.PHOTO, sortOrder))
        }
        if (filter == LibraryMediaFilter.ALL || filter == LibraryMediaFilter.VIDEOS) {
            items.addAll(queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO, sortOrder))
        }

        return when (sortOrder) {
            LibrarySortOrder.CAPTURE_DATE -> items.sortedByDescending { it.creationTimestamp ?: Instant.EPOCH }
            LibrarySortOrder.DATE_ADDED -> items.sortedByDescending { it.addedTimestamp ?: Instant.EPOCH }
        }
    }

    fun scanAlbums(): List<Album> {
        val albumsMap = mutableMapOf<String, MutableList<PhotoItem>>()
        val albumTitles = mutableMapOf<String, String>()

        val photos = scanPhotos(LibrarySortOrder.CAPTURE_DATE, LibraryMediaFilter.ALL)
        for (photo in photos) {
            // Bucket aggregation can be enhanced with cursor BUCKET_ID if needed
            val albumId = "all_photos"
            albumsMap.getOrPut(albumId) { mutableListOf() }.add(photo)
            albumTitles[albumId] = "Alle Fotos"
        }

        return albumsMap.map { (id, items) ->
            Album(
                id = id,
                title = albumTitles[id] ?: "Unbenanntes Album",
                assetCount = items.size,
                coverPhoto = items.firstOrNull()
            )
        }
    }

    private fun queryMedia(
        contentUri: Uri,
        mediaType: MediaType,
        sortOrder: LibrarySortOrder
    ): List<PhotoItem> {
        val items = mutableListOf<PhotoItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )

        val sortColumn = if (sortOrder == LibrarySortOrder.DATE_ADDED) {
            MediaStore.MediaColumns.DATE_ADDED
        } else {
            MediaStore.MediaColumns.DATE_ADDED // Falls back to date_added if date_taken is not uniform
        }

        val cursor = context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            "$sortColumn DESC"
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dateModifiedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val dateAddedSec = c.getLong(dateAddedColumn)
                val dateModifiedSec = c.getLong(dateModifiedColumn)
                val width = c.getInt(widthColumn)
                val height = c.getInt(heightColumn)

                val itemUri = ContentUris.withAppendedId(contentUri, id)

                items.add(
                    PhotoItem(
                        id = id.toString(),
                        contentUri = itemUri.toString(),
                        creationTimestamp = Instant.ofEpochSecond(dateAddedSec),
                        addedTimestamp = Instant.ofEpochSecond(dateAddedSec),
                        modificationTimestamp = Instant.ofEpochSecond(dateModifiedSec),
                        location = null,
                        width = width,
                        height = height,
                        mediaType = mediaType,
                        isFavorite = false
                    )
                )
            }
        }

        return items
    }

    fun scanPhotoItemsFromUris(uris: List<Uri>): List<PhotoItem> {
        return uris.mapNotNull { uri ->
            try {
                var width = 0
                var height = 0
                var dateAddedSec = System.currentTimeMillis() / 1000
                var mediaType = MediaType.PHOTO

                val mimeType = context.contentResolver.getType(uri)
                if (mimeType?.startsWith("video/") == true) {
                    mediaType = MediaType.VIDEO
                }

                val projection = arrayOf(
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT
                )

                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dateIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                        val wIdx = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                        val hIdx = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)

                        if (dateIdx != -1 && !cursor.isNull(dateIdx)) {
                            dateAddedSec = cursor.getLong(dateIdx)
                        }
                        if (wIdx != -1 && !cursor.isNull(wIdx)) {
                            width = cursor.getInt(wIdx)
                        }
                        if (hIdx != -1 && !cursor.isNull(hIdx)) {
                            height = cursor.getInt(hIdx)
                        }
                    }
                }

                PhotoItem(
                    id = uri.toString(),
                    contentUri = uri.toString(),
                    creationTimestamp = Instant.ofEpochSecond(dateAddedSec),
                    addedTimestamp = Instant.ofEpochSecond(dateAddedSec),
                    modificationTimestamp = Instant.ofEpochSecond(dateAddedSec),
                    location = null,
                    width = width,
                    height = height,
                    mediaType = mediaType,
                    isFavorite = false
                )
            } catch (e: Exception) {
                PhotoItem(
                    id = uri.toString(),
                    contentUri = uri.toString(),
                    creationTimestamp = Instant.now(),
                    addedTimestamp = Instant.now(),
                    modificationTimestamp = Instant.now(),
                    location = null,
                    width = 0,
                    height = 0,
                    mediaType = MediaType.PHOTO,
                    isFavorite = false
                )
            }
        }
    }
}
