package com.example.videosvirales.data.remote.editor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AndroidVideoThumbnailer(
    private val context: Context,
) {
    suspend fun load(
        video: VideoFile,
        timestampMs: Long,
        aspect: VideoAspect,
    ): ByteArray = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(video.uri))
            val frame = retriever.getFrameAtTime(
                timestampMs * 1_000,
                MediaMetadataRetriever.OPTION_CLOSEST,
            ) ?: throw EditorException(EditorError.ANALYSIS_FAILED)
            val cropped = cropToAspect(frame, aspect)
            val scaled = Bitmap.createScaledBitmap(
                cropped,
                aspect.thumbnailWidth,
                aspect.thumbnailHeight,
                true,
            )
            try {
                ByteArrayOutputStream().use { output ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)
                    output.toByteArray()
                }
            } finally {
                if (scaled !== cropped) scaled.recycle()
                if (cropped !== frame) cropped.recycle()
                frame.recycle()
            }
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.ANALYSIS_FAILED, exception)
        } finally {
            retriever.release()
        }
    }

    private fun cropToAspect(bitmap: Bitmap, aspect: VideoAspect): Bitmap {
        val targetRatio = aspect.width.toFloat() / aspect.height
        val sourceRatio = bitmap.width.toFloat() / bitmap.height
        return if (sourceRatio > targetRatio) {
            val width = (bitmap.height * targetRatio).toInt().coerceAtLeast(1)
            val left = (bitmap.width - width) / 2
            Bitmap.createBitmap(bitmap, left, 0, width, bitmap.height)
        } else {
            val height = (bitmap.width / targetRatio).toInt().coerceAtLeast(1)
            val top = (bitmap.height - height) / 2
            Bitmap.createBitmap(bitmap, 0, top, bitmap.width, height)
        }
    }
}
