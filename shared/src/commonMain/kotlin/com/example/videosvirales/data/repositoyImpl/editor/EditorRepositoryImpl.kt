package com.example.videosvirales.data.repositoyImpl.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
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
    ): EditorResult<ExportedClip> =
        safeCall(EditorError.EXPORT_FAILED) { editorServices.exportClip(video, clip, outputDirectory) }

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
