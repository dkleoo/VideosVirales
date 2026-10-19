package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

class DesktopEditorServices : EditorServices {
    private val analyzer = DesktopVideoAnalyzer()
    private val exporter = DesktopVideoExporter()

    override suspend fun loadVideo(uri: String): VideoFile = withContext(Dispatchers.IO) {
        try {
            FfmpegTools.requireAvailable()
            val file = File(localPath(uri))
            if (!file.isFile) throw EditorException(EditorError.NO_VIDEO)
            val durationMs = FfmpegTools.probeDurationMs(file.absolutePath)
            if (durationMs <= 0) throw EditorException(EditorError.NO_VIDEO)
            VideoFile(
                uri = file.absolutePath,
                displayName = file.name,
                durationMs = durationMs,
                sizeBytes = file.length(),
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.NO_VIDEO, exception)
        }
    }

    override suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal> = analyzer.analyze(video, onProgress)

    override suspend fun exportClip(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
    ): ExportedClip = exporter.export(video, clip, outputDirectory)

    override suspend fun openClip(uri: String) = withContext(Dispatchers.IO) {
        try {
            val file = if (uri.startsWith("file:")) {
                File(java.net.URI(uri))
            } else {
                File(uri)
            }
            if (!file.isFile) throw EditorException(EditorError.OPEN_FAILED)
            if (!Desktop.isDesktopSupported() ||
                !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
            ) {
                throw EditorException(EditorError.OPEN_FAILED)
            }
            Desktop.getDesktop().open(file)
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.OPEN_FAILED, exception)
        }
    }

    override fun defaultOutputDirectory(): String {
        val directory = File(System.getProperty("user.home"), "Movies/VideosVirales")
        directory.mkdirs()
        return directory.absolutePath
    }

    private fun localPath(uri: String): String =
        if (uri.startsWith("file:")) {
            try {
                File(java.net.URI(uri)).absolutePath
            } catch (exception: Exception) {
                uri.removePrefix("file://")
            }
        } else {
            uri
        }
}
