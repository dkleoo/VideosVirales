package com.example.videosvirales.data.repositoyImpl.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.remote.editor.EditorServices
import com.example.videosvirales.domain.repository.editor.EditorRepository

class EditorRepositoryImpl(
    private val editorServices: EditorServices,
) : EditorRepository {
    override suspend fun loadVideo(uri: String): EditorResult<VideoFile> =
        safeCall(EditorError.NO_VIDEO) { editorServices.loadVideo(uri) }

    override suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): EditorResult<List<VideoSignal>> =
        safeCall(EditorError.ANALYSIS_FAILED) { editorServices.analyze(video, onProgress) }

    override suspend fun exportClip(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
        narrationText: String,
    ): EditorResult<ExportedClip> =
        safeCall(EditorError.EXPORT_FAILED) {
            editorServices.exportClip(video, clip, outputDirectory, narrationText)
        }

    override suspend fun mergeVideos(
        videos: List<VideoFile>,
        narrations: List<String>,
        sceneAudios: List<AudioFile?>,
        fullAudio: AudioFile?,
        aspect: VideoAspect,
        outputDirectory: String,
    ): EditorResult<ExportedClip> =
        safeCall(EditorError.EXPORT_FAILED) {
            editorServices.mergeVideos(
                videos,
                narrations,
                sceneAudios,
                fullAudio,
                aspect,
                outputDirectory,
            )
        }

    override suspend fun loadAudio(uri: String): EditorResult<AudioFile> =
        safeCall(EditorError.NO_VIDEO) { editorServices.loadAudio(uri) }

    override suspend fun loadThumbnail(
        video: VideoFile,
        timestampMs: Long,
        aspect: VideoAspect,
    ): EditorResult<ByteArray> =
        safeCall(EditorError.ANALYSIS_FAILED) {
            editorServices.loadThumbnail(video, timestampMs, aspect)
        }

    override suspend fun transcribe(
        video: VideoFile,
        startMs: Long,
        endMs: Long,
    ): EditorResult<String> =
        safeCall(EditorError.TRANSCRIPTION_FAILED) {
            editorServices.transcribe(video, startMs, endMs)
        }

    override suspend fun openClip(clip: ExportedClip): EditorResult<Unit> =
        safeCall(EditorError.OPEN_FAILED) { editorServices.openClip(clip.uri) }

    override fun defaultOutputDirectory(): String = editorServices.defaultOutputDirectory()

    private suspend inline fun <T> safeCall(
        fallbackError: EditorError,
        block: suspend () -> T,
    ): EditorResult<T> = try {
        EditorResult.Success(block())
    } catch (exception: EditorException) {
        EditorResult.Error(exception.error)
    } catch (exception: Exception) {
        EditorResult.Error(fallbackError)
    }
}
