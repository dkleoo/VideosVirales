package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import java.io.File

object FfmpegTools {
    private val ffmpegPath: String by lazy { resolveFfmpeg() }
    private val ffprobePath: String by lazy { resolveFfprobe() }
    private val filters: Set<String> by lazy { loadFilters() }

    fun requireAvailable() {
        if (!isRunnable(ffmpegPath) || !isRunnable(ffprobePath)) {
            throw EditorException(EditorError.FFMPEG_MISSING)
        }
    }

    fun requireFilters(vararg requiredFilters: String) {
        requireAvailable()
        val missing = requiredFilters.filterNot { it in filters }
        if (missing.isNotEmpty()) {
            throw EditorException(EditorError.FFMPEG_MISSING)
        }
    }

    fun run(
        arguments: List<String>,
        workingDirectory: File? = null,
    ): String {
        requireAvailable()
        val command = listOf(ffmpegPath) + arguments
        return try {
            val builder = ProcessBuilder(command).redirectErrorStream(true)
            if (workingDirectory != null) builder.directory(workingDirectory)
            val process = builder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode != 0) throw ProcessFailure(output)
            output
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: ProcessFailure) {
            throw EditorException(EditorError.EXPORT_FAILED, exception)
        } catch (exception: Exception) {
            throw EditorException(EditorError.FFMPEG_MISSING, exception)
        }
    }

    fun runForBytes(arguments: List<String>): ByteArray {
        requireAvailable()
        val command = listOf(ffmpegPath) + arguments
        return try {
            val process = ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            val bytes = process.inputStream.use { it.readBytes() }
            val exitCode = process.waitFor()
            if (exitCode != 0) throw EditorException(EditorError.ANALYSIS_FAILED)
            bytes
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.FFMPEG_MISSING, exception)
        }
    }

    fun probeDurationMs(path: String): Long {
        val command = listOf(
            ffprobePath,
            "-v",
            "error",
            "-show_entries",
            "format=duration",
            "-of",
            "default=noprint_wrappers=1:nokey=1",
            path,
        )
        return try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() != 0) throw EditorException(EditorError.NO_VIDEO)
            (output.trim().toDoubleOrNull()?.times(1_000))?.toLong() ?: 0L
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.FFMPEG_MISSING, exception)
        }
    }

    private fun resolveFfmpeg(): String {
        val candidates = listOf(
            "/opt/homebrew/opt/ffmpeg-full/bin/ffmpeg",
            "/usr/local/opt/ffmpeg-full/bin/ffmpeg",
            "/opt/homebrew/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "ffmpeg",
        )
        return candidates.firstOrNull(::isRunnable) ?: candidates.first()
    }

    private fun resolveFfprobe(): String {
        val ffmpegFile = File(ffmpegPath)
        val sibling = ffmpegFile.parentFile?.resolve("ffprobe")
        if (sibling != null && isRunnable(sibling.absolutePath)) {
            return sibling.absolutePath
        }
        val candidates = listOf(
            "/opt/homebrew/opt/ffmpeg-full/bin/ffprobe",
            "/usr/local/opt/ffmpeg-full/bin/ffprobe",
            "/opt/homebrew/bin/ffprobe",
            "/usr/local/bin/ffprobe",
            "ffprobe",
        )
        return candidates.firstOrNull(::isRunnable) ?: candidates.first()
    }

    private fun isRunnable(path: String): Boolean = try {
        val process = ProcessBuilder(path, "-version")
            .redirectErrorStream(true)
            .start()
        process.inputStream.use { it.readBytes() }
        process.waitFor() == 0
    } catch (exception: Exception) {
        false
    }

    private fun loadFilters(): Set<String> {
        if (!isRunnable(ffmpegPath)) return emptySet()
        return try {
            val process = ProcessBuilder(ffmpegPath, "-hide_banner", "-filters")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() != 0) return emptySet()
            output.lineSequence()
                .mapNotNull { line -> line.trim().split(Regex("\\s+")).getOrNull(1) }
                .toSet()
        } catch (exception: Exception) {
            emptySet()
        }
    }

    private class ProcessFailure(output: String) : Exception(output.take(2_000))
}
