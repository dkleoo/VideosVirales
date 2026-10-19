package com.example.videosvirales.data.remote.editor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.example.videosvirales.data.mappers.editor.ClipAnimation
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
class AndroidVideoExporter(
    private val context: Context,
) {
    suspend fun export(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
    ): ExportedClip = withContext(Dispatchers.Main) {
        try {
            val fileName = "clip_${clip.index.toString().padStart(2, '0')}_${System.currentTimeMillis()}.mp4"
            val tempFile = File(context.cacheDir, fileName)

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(video.uri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                )
                .build()

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), buildVideoEffects(clip)))
                .build()

            suspendCancellableCoroutine<Unit> { continuation ->
                val listener = object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult,
                    ) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                EditorException(EditorError.EXPORT_FAILED, exportException)
                            )
                        }
                    }
                }
                val activeTransformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(listener)
                    .build()
                continuation.invokeOnCancellation { activeTransformer.cancel() }
                activeTransformer.start(editedMediaItem, tempFile.absolutePath)
            }

            val published = publish(
                tempFile = tempFile,
                fileName = fileName,
                fallbackDirectory = outputDirectory,
            )
            ExportedClip(
                index = clip.index,
                path = published.path,
                displayName = fileName,
                uri = published.uri,
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.EXPORT_FAILED, exception)
        }
    }

    private fun buildVideoEffects(clip: ClipDraft): List<Effect> = buildList {
        add(
            Presentation.createForWidthAndHeight(
                WIDTH,
                HEIGHT,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
            )
        )
        val durationUs = clip.durationMs * 1_000
        when (clip.animation) {
            ClipAnimation.ZOOM_IN -> add(
                KenBurnsTransformation(
                    fromScale = 1f,
                    toScale = 1.12f,
                    durationUs = durationUs,
                )
            )
            ClipAnimation.ZOOM_OUT -> add(
                KenBurnsTransformation(
                    fromScale = 1.12f,
                    toScale = 1f,
                    durationUs = durationUs,
                )
            )
            else -> Unit
        }
        if (clip.subtitle.isNotBlank()) {
            add(
                OverlayEffect(
                    listOf(
                        AnimatedSubtitleOverlay(
                            text = clip.subtitle,
                            durationUs = durationUs,
                            animation = clip.animation,
                        )
                    )
                )
            )
        }
    }

    private fun publish(
        tempFile: File,
        fileName: String,
        fallbackDirectory: String,
    ): PublishedClip {
        val published = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { publishToGallery(tempFile, fileName) }
                .getOrElse { publishToDirectory(tempFile, fileName, fallbackDirectory) }
        } else {
            publishToDirectory(tempFile, fileName, fallbackDirectory)
        }
        tempFile.delete()
        return published
    }

    private fun publishToGallery(tempFile: File, fileName: String): PublishedClip {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/VideosVirales",
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw EditorException(EditorError.EXPORT_FAILED)

        return try {
            val output = resolver.openOutputStream(uri)
                ?: throw EditorException(EditorError.EXPORT_FAILED)
            output.use { target ->
                tempFile.inputStream().use { source -> source.copyTo(target) }
            }
            val completedValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completedValues, null, null)
            PublishedClip(
                path = "${Environment.DIRECTORY_MOVIES}/VideosVirales/$fileName",
                uri = uri.toString(),
            )
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }
    }

    private fun publishToDirectory(
        tempFile: File,
        fileName: String,
        directoryPath: String,
    ): PublishedClip {
        val directory = File(directoryPath).apply { mkdirs() }
        val target = File(directory, fileName)
        tempFile.copyTo(target, overwrite = true)
        return PublishedClip(
            path = target.absolutePath,
            uri = target.absolutePath,
        )
    }

    private data class PublishedClip(
        val path: String,
        val uri: String,
    )

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1920
    }
}
