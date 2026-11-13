package com.airpx.photocloud.data

import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import android.util.Log
import com.airpx.photocloud.domain.model.SyncPhoto
import com.airpx.photocloud.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant

class PhotoSyncRepository(private val context: Context, private val api: PhotoCloudApi) : SyncRepository {
    private val hashCache by lazy { MediaHashCache(context) }
    override suspend fun scanPhotos(): List<SyncPhoto> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<SyncPhoto>()
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.MIME_TYPE)
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol).toString())
                val mediaId = cursor.getLong(idCol).toString()
                val modified = cursor.getLong(modifiedCol)
                val hash = hash(mediaId, modified, uri)
                val millis = cursor.getLong(dateCol)
                photos += SyncPhoto(uri.toString(), cursor.getString(nameCol) ?: "photo", if (millis > 0) Instant.ofEpochMilli(millis).toString() else null,
                    cursor.getString(mimeCol) ?: "image/jpeg", cursor.getLong(sizeCol), hash)
            }
        }
        val scannedUris = photos.mapTo(mutableSetOf()) { it.uri }
        SelectedPhotoUris.get(context).filter { it.toString() !in scannedUris }.forEach { uri ->
            val mime = context.contentResolver.getType(uri) ?: return@forEach
            if (!mime.startsWith("image/")) return@forEach
            var filename = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "photo" } ?: "photo"
            var size = 0L
            var modified = 0L
            var capturedAt: String? = null
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
            photos += SyncPhoto(uri.toString(), filename, capturedAt, mime, size, hash(uri.toString(), modified, uri))
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

    private fun hash(cacheId: String, modified: Long, uri: Uri): String = hashCache.get(cacheId, modified)
        ?: context.contentResolver.openInputStream(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }.also { hashCache.put(cacheId, modified, it) }
        } ?: error("Foto kann nicht gelesen werden")
}
