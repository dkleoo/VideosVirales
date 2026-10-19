package com.example.videosvirales.data.remote.editor

import android.graphics.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation

@androidx.annotation.OptIn(UnstableApi::class)
class KenBurnsTransformation(
    private val fromScale: Float,
    private val toScale: Float,
    private val durationUs: Long,
) : MatrixTransformation {
    private val matrix = Matrix()

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val progress = if (durationUs > 0) {
            (presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
        } else {
            0f
        }
        val scale = fromScale + (toScale - fromScale) * progress
        matrix.reset()
        matrix.postScale(scale, scale)
        return matrix
    }
}
