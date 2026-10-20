package com.example.videosvirales.data.remote.editor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoAspect
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
    private val tts = AndroidTtsSynthesizer(context)

    suspend fun export(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
        narrationText: String,
    ): ExportedClip = withContext(Dispatchers.Main) {
        var narrationFile: File? = null
        try {
            narrationFile = if (clip.audio == null) narration(narrationText) else null
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

            val videoItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), buildVideoEffects(clip)))
                .build()
            val sequences = mutableListOf(
                EditedMediaItemSequence.withAudioAndVideoFrom(listOf(videoItem))
            )
            val customAudio = clip.audio
            if (customAudio != null) {
                sequences += audioSequence(customAudio, clip.durationMs, 0L)
            } else {
                narrationFile?.let { file ->
                    sequences += EditedMediaItemSequence.withAudioFrom(
                        listOf(narrationItem(file, clip.durationMs))
                    )
                }
            }

            transform(Composition.Builder(sequences).build(), tempFile)
            val published = publish(tempFile, fileName, outputDirectory)
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
        } finally {
            narrationFile?.delete()
        }
    }

    suspend fun mergeVideos(
        videos: List<VideoFile>,
        narrations: List<String>,
        sceneAudios: List<AudioFile?>,
        fullAudio: AudioFile?,
        aspect: VideoAspect,
        outputDirectory: String,
    ): ExportedClip = withContext(Dispatchers.Main) {
        if (videos.isEmpty()) throw EditorException(EditorError.EXPORT_FAILED)
        val narrationFiles = mutableListOf<File>()
        try {
            val items = videos.map { video ->
                val effects = Effects(
                    emptyList(),
                    listOf(
                        Presentation.createForWidthAndHeight(
                            aspect.width,
                            aspect.height,
                            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
                        )
                    ),
                )
                EditedMediaItem.Builder(MediaItem.fromUri(video.uri))
                    .setEffects(effects)
                    .build()
            }
            val sequences = mutableListOf(
                EditedMediaItemSequence.withAudioAndVideoFrom(items)
            )

            var offsetUs = 0L
            videos.forEachIndexed { index, video ->
                val sceneAudio = sceneAudios.getOrNull(index)
                if (sceneAudio != null) {
                    sequences += audioSequence(sceneAudio, video.durationMs, offsetUs)
                } else {
                    val text = narrations.getOrElse(index) { "" }
                    if (text.isNotBlank()) {
                        val file = tts.synthesize(text)
                        narrationFiles += file
                        sequences += EditedMediaItemSequence
                            .Builder(setOf(C.TRACK_TYPE_AUDIO))
                            .addGap(offsetUs)
                            .addItem(narrationItem(file, video.durationMs))
                            .build()
                    }
                }
                offsetUs += video.durationMs * 1_000
            }
            if (fullAudio != null) {
                val totalDurationMs = videos.sumOf { it.durationMs }.coerceAtLeast(1)
                sequences += audioSequence(fullAudio, totalDurationMs, 0L)
            }

            val fileName = "merged_${System.currentTimeMillis()}.mp4"
            val tempFile = File(context.cacheDir, fileName)
            transform(Composition.Builder(sequences).build(), tempFile)
            val published = publish(tempFile, fileName, outputDirectory)
            ExportedClip(
                index = 1,
                path = published.path,
                displayName = fileName,
                uri = published.uri,
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.EXPORT_FAILED, exception)
        } finally {
            narrationFiles.forEach { file -> file.delete() }
        }
    }

    private fun audioSequence(
        audio: AudioFile,
        durationMs: Long,
        offsetUs: Long,
    ): EditedMediaItemSequence = EditedMediaItemSequence
        .Builder(setOf(C.TRACK_TYPE_AUDIO))
        .addGap(offsetUs.coerceAtLeast(0))
        .addItem(
            EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(Uri.parse(audio.uri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setEndPositionMs(durationMs.coerceAtLeast(1))
                            .build()
                    )
                    .build()
            ).build()
        )
        .build()

    private suspend fun narration(text: String): File? =
        if (text.isBlank()) null else tts.synthesize(text)

    private fun narrationItem(file: File, durationMs: Long): EditedMediaItem =
        EditedMediaItem.Builder(
            MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs(durationMs.coerceAtLeast(1))
                        .build()
                )
                .build()
        ).build()

    private suspend fun transform(composition: Composition, outputFile: File) {
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
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(listener)
                .build()
            continuation.invokeOnCancellation { transformer.cancel() }
            transformer.start(composition, outputFile.absolutePath)
        }
    }

    private fun buildVideoEffects(clip: ClipDraft): List<Effect> = buildList {
        add(
            Presentation.createForWidthAndHeight(
                clip.aspect.width,
                clip.aspect.height,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
            )
        )
        if (clip.subtitle.isNotBlank()) {
            add(OverlayEffect(listOf(SubtitleOverlay(clip.subtitle))))
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
}
