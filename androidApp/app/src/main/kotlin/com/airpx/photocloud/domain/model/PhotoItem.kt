package com.airpx.photocloud.domain.model

import java.time.Instant

enum class MediaType {
    PHOTO,
    VIDEO
}

data class Location(
    val latitude: Double,
    val longitude: Double
)

data class PhotoItem(
    val id: String, // MediaStore ID or local identifier
    val contentUri: String,
    val creationTimestamp: Instant?,
    val addedTimestamp: Instant?,
    val modificationTimestamp: Instant?,
    val location: Location?,
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    val isFavorite: Boolean
)
