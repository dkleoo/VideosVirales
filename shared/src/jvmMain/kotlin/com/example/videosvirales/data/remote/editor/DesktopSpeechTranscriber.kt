package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.Locale

class DesktopSpeechTranscriber {
    suspend fun transcribe(
        video: VideoFile,
        startMs: Long,
        endMs: Long,
    ): String = withContext(Dispatchers.IO) {
        FfmpegTools.requireAvailable()
        val path = video.localPath()
        if (!FfmpegTools.hasAudio(path)) return@withContext ""

        val temporaryDirectory = Files.createTempDirectory("videosvirales_stt_").toFile()
        try {
            val audioFile = File(temporaryDirectory, "audio.wav")
            FfmpegTools.run(
                listOf(
                    "-hide_banner",
                    "-nostdin",
                    "-y",
                    "-ss",
                    formatSeconds(startMs),
                    "-i",
                    path,
                    "-t",
                    formatSeconds(endMs - startMs),
                    "-vn",
                    "-ac",
                    "1",
                    "-ar",
                    "16000",
                    "-c:a",
                    "pcm_s16le",
                    audioFile.absolutePath,
                ),
                workingDirectory = temporaryDirectory,
            )
            WhisperTools.transcribe(audioFile)
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TRANSCRIPTION_FAILED, exception)
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun VideoFile.localPath(): String =
        if (uri.startsWith("file:")) {
            try {
                File(java.net.URI(uri)).absolutePath
            } catch (exception: Exception) {
                uri.removePrefix("file://")
            }
        } else {
            uri
        }

    private fun formatSeconds(durationMs: Long): String =
        String.format(Locale.US, "%.3f", durationMs.coerceAtLeast(0) / 1_000.0)
}
