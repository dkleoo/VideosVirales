package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

class DesktopVideoAnalyzer {
    suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal> = withContext(Dispatchers.IO) {
        FfmpegTools.requireAvailable()
        val path = video.path()
        val sceneSignals = detectScenes(path, onProgress)
        val audioSignals = detectAudioEnergy(path, onProgress)
        (sceneSignals + audioSignals)
            .sortedBy { it.timestampMs }
            .also { onProgress(1f) }
    }

    private fun detectScenes(
        path: String,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal> {
        val output = try {
            FfmpegTools.run(
                listOf(
                    "-hide_banner",
                    "-nostdin",
                    "-i",
                    path,
                    "-an",
                    "-vf",
                    "select='gt(scene,0.3)',showinfo",
                    "-f",
                    "null",
                    "-",
                )
            )
        } catch (exception: EditorException) {
            if (exception.error == EditorError.FFMPEG_MISSING) throw exception
            return emptyList()
        }

        onProgress(0.55f)
        val pattern = Regex("pts_time:([0-9]+(?:\\.[0-9]+)?)")
        return pattern.findAll(output)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .filter { it >= 0 }
            .map { seconds ->
                VideoSignal(
                    timestampMs = (seconds * 1_000).toLong(),
                    score = SCENE_SCORE,
                )
            }
            .toList()
    }

    private fun detectAudioEnergy(
        path: String,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal> {
        val bytes = try {
            FfmpegTools.runForBytes(
                listOf(
                    "-hide_banner",
                    "-nostdin",
                    "-i",
                    path,
                    "-vn",
                    "-ac",
                    "1",
                    "-ar",
                    SAMPLE_RATE.toString(),
                    "-f",
                    "s16le",
                    "-",
                )
            )
        } catch (exception: EditorException) {
            if (exception.error == EditorError.FFMPEG_MISSING) throw exception
            return emptyList()
        }

        onProgress(0.8f)
        if (bytes.size < 2) return emptyList()

        val sampleCount = bytes.size / 2
        val windowSamples = max(1, SAMPLE_RATE * WINDOW_MS / 1_000)
        val windows = mutableListOf<Pair<Long, Float>>()
        var index = 0
        while (index + 1 < bytes.size) {
            val end = minOf(index + windowSamples * 2, bytes.size)
            var sum = 0.0
            var count = 0
            var byteIndex = index
            while (byteIndex + 1 < end) {
                val low = bytes[byteIndex].toInt() and 0xFF
                val high = bytes[byteIndex + 1].toInt()
                val sample = (high shl 8) or low
                sum += sample.toDouble() * sample
                count++
                byteIndex += 2
            }
            if (count > 0) {
                val rms = sqrt(sum / count).toFloat()
                val timeMs = (index / 2L) * 1_000 / SAMPLE_RATE
                windows += timeMs to rms
            }
            index = end
        }

        val maxRms = windows.maxOfOrNull { it.second } ?: 0f
        if (maxRms <= 0f) return emptyList()

        return windows
            .map { (timeMs, rms) ->
                VideoSignal(
                    timestampMs = timeMs,
                    score = (rms / maxRms).coerceIn(0f, 1f),
                )
            }
            .filter { it.score > 0.15f }
    }

    private fun VideoFile.path(): String =
        if (uri.startsWith("file:")) filePath() else uri

    private fun VideoFile.filePath(): String = try {
        java.io.File(java.net.URI(uri)).absolutePath
    } catch (exception: Exception) {
        uri.removePrefix("file://")
    }

    private companion object {
        const val SAMPLE_RATE = 8_000
        const val WINDOW_MS = 500
        const val SCENE_SCORE = 0.7f
    }
}
