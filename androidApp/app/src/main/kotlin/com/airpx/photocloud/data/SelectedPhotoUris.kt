package com.airpx.photocloud.data

import android.content.Context
import android.net.Uri

/** Persisted system-picker grants let WorkManager read the same picker photos shown in the library. */
object SelectedPhotoUris {
    private const val PREFS = "selected_photo_uris"
    private const val KEY_URIS = "uris"

    fun add(context: Context, uris: List<Uri>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_URIS, emptySet()).orEmpty().toMutableSet()
        saved += uris.map(Uri::toString)
        prefs.edit().putStringSet(KEY_URIS, saved).apply()
    }

    fun get(context: Context): List<Uri> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(KEY_URIS, emptySet()).orEmpty().map(Uri::parse)
}
