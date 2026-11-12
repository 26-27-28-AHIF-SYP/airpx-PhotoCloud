package com.airpx.photocloud.domain.model

data class SyncPhoto(
    val uri: String,
    val filename: String,
    val capturedAt: String?,
    val mimeType: String,
    val size: Long,
    val sha256: String
)

data class SyncSummary(val uploaded: Int, val pending: Int, val failed: Int)
