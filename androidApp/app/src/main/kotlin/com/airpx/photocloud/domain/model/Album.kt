package com.airpx.photocloud.domain.model

data class Album(
    val id: String,
    val title: String,
    val assetCount: Int,
    val coverPhoto: PhotoItem?
)
