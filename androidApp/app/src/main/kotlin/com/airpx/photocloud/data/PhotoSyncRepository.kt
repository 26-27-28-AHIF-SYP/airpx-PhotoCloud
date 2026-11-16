package com.airpx.photocloud.data

import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import android.util.Log
import com.airpx.photocloud.data.mediastore.MediaStoreScanner
import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.SyncPhoto
import com.airpx.photocloud.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant

class PhotoSyncRepository(private val context: Context, private val api: PhotoCloudApi) : SyncRepository {
    private val hashCache by lazy { MediaHashCache(context) }
    override suspend fun scanPhotos(): List<SyncPhoto> = withContext(Dispatchers.IO) {
        val photos = MediaStoreScanner(context)
            .scanPhotos(filter = LibraryMediaFilter.PHOTOS)
            .mapNotNull { photo -> syncPhoto(Uri.parse(photo.contentUri)) }
            .toMutableList()
        val scannedUris = photos.mapTo(mutableSetOf()) { it.uri }
        SelectedPhotoUris.get(context).filter { it.toString() !in scannedUris }.forEach { uri ->
            val mime = context.contentResolver.getType(uri) ?: return@forEach
            if (!mime.startsWith("image/")) return@forEach
            syncPhoto(uri)?.let(photos::add)
        }
        Log.d("PhotoSync", "Images found by scan=${photos.size}")
        photos
    }

    override suspend fun missingHashes(hashes: List<String>) = withContext(Dispatchers.IO) {
        hashes.chunked(500).flatMap { api.missing(it) }.toSet()
    }
    override suspend fun upload(photo: SyncPhoto) = withContext(Dispatchers.IO) {
        api.upload(photo.filename, photo.capturedAt, photo.sha256, photo.size, photo.mimeType) { output ->
            context.contentResolver.openInputStream(Uri.parse(photo.uri))?.use { it.copyTo(output) }
                ?: error("Datei nicht verfügbar")
        }
    }

    private fun syncPhoto(uri: Uri): SyncPhoto? {
        var filename = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "photo" } ?: "photo"
        var size = 0L
        var modified = 0L
        var capturedAt: String? = null
        val mime = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: return null
        context.contentResolver.query(uri, arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.DATE_TAKEN
        ), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME).takeIf { it >= 0 && !cursor.isNull(it) }?.let { filename = cursor.getString(it) }
                cursor.getColumnIndex(MediaStore.MediaColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let { size = cursor.getLong(it) }
                cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED).takeIf { it >= 0 && !cursor.isNull(it) }?.let { modified = cursor.getLong(it) }
                cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN).takeIf { it >= 0 && !cursor.isNull(it) }?.let { capturedAt = Instant.ofEpochMilli(cursor.getLong(it)).toString() }
            }
        }
        val cacheId = uri.lastPathSegment ?: uri.toString()
        return SyncPhoto(uri.toString(), filename, capturedAt, mime, size, hash(cacheId, modified, uri))
    }

    private fun hash(cacheId: String, modified: Long, uri: Uri): String = hashCache.get(cacheId, modified)
        ?: context.contentResolver.openInputStream(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }.also { hashCache.put(cacheId, modified, it) }
        } ?: error("Foto kann nicht gelesen werden")
}
