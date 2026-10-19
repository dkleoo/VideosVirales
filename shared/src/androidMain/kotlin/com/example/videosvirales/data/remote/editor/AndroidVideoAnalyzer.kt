package com.example.videosvirales.data.remote.editor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class AndroidVideoAnalyzer(
    private val context: Context,
) {
    suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal> = withContext(Dispatchers.Default) {
        if (video.durationMs <= 0) return@withContext emptyList()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(video.uri))
            val sampleIntervalMs = max(500L, video.durationMs / 120)
            val sampleCount = (video.durationMs / sampleIntervalMs).toInt() + 1
            val signals = mutableListOf<VideoSignal>()
            var previousSignature: FloatArray? = null

            repeat(sampleCount) { index ->
                val timeMs = (index * sampleIntervalMs).coerceAtMost(video.durationMs)
                val signature = frameSignature(retriever, timeMs)
                if (signature != null) {
                    val previous = previousSignature
                    if (previous != null) {
                        val score = difference(previous, signature)
                        if (score > 0.01f) {
                            signals += VideoSignal(timestampMs = timeMs, score = score)
                        }
                    }
                    previousSignature = signature
                }
                if (index % 4 == 0) {
                    onProgress(index.toFloat() / sampleCount)
                }
            }

            onProgress(1f)
            signals
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.ANALYSIS_FAILED, exception)
        } finally {
            retriever.release()
        }
    }

    private fun frameSignature(
        retriever: MediaMetadataRetriever,
        timeMs: Long,
    ): FloatArray? {
        val bitmap = frameAt(retriever, timeMs) ?: return null
        return try {
            val scaled = if (bitmap.width == SIZE && bitmap.height == SIZE) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
            }
            try {
                val pixels = IntArray(SIZE * SIZE)
                scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
                FloatArray(pixels.size) { index ->
                    val pixel = pixels[index]
                    val red = pixel shr 16 and 0xFF
                    val green = pixel shr 8 and 0xFF
                    val blue = pixel and 0xFF
                    (red + green + blue) / (3f * 255f)
                }
            } finally {
                if (scaled !== bitmap) scaled.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun frameAt(
        retriever: MediaMetadataRetriever,
        timeMs: Long,
    ): Bitmap? {
        val timeUs = timeMs * 1_000
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
                SIZE,
                SIZE,
            )
        } else {
            val original = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: return null
            Bitmap.createScaledBitmap(original, SIZE, SIZE, true).also {
                if (it !== original) original.recycle()
            }
        }
    }

    private fun difference(first: FloatArray, second: FloatArray): Float {
        if (first.size != second.size) return 0f
        var total = 0f
        first.indices.forEach { index ->
            total += abs(first[index] - second[index])
        }
        return (total / first.size).coerceIn(0f, 1f)
    }

    private companion object {
        const val SIZE = 24
    }
}
