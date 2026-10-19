package com.example.videosvirales.presentation.util

import kotlin.math.roundToInt

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

fun formatScore(score: Float): Int = (score.coerceIn(0f, 1f) * 100).roundToInt()
