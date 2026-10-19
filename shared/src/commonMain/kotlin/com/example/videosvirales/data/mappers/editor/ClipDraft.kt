package com.example.videosvirales.data.mappers.editor

data class ClipDraft(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val score: Float,
    val subtitle: String,
    val animation: ClipAnimation,
) {
    val durationMs: Long
        get() = endMs - startMs
}
