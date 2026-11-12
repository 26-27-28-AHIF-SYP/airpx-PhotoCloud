package com.airpx.photocloud.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MediaHashCache(context: Context) : SQLiteOpenHelper(context, "media_hashes.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE media_hash (media_id TEXT PRIMARY KEY, date_modified INTEGER NOT NULL, sha256 TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun get(mediaId: String, dateModified: Long): String? = readableDatabase.rawQuery(
        "SELECT sha256 FROM media_hash WHERE media_id = ? AND date_modified = ?",
        arrayOf(mediaId, dateModified.toString())
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    fun put(mediaId: String, dateModified: Long, hash: String) {
        writableDatabase.insertWithOnConflict("media_hash", null, ContentValues().apply {
            put("media_id", mediaId)
            put("date_modified", dateModified)
            put("sha256", hash)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
