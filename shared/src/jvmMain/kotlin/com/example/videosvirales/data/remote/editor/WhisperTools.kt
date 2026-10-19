package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object WhisperTools {
    private const val MODEL_URL =
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin?download=true"
    private const val MIN_MODEL_BYTES = 10_000_000L

    private val executable: String by lazy {
        listOf(
            "/opt/homebrew/bin/whisper-cli",
            "/usr/local/bin/whisper-cli",
            "whisper-cli",
        ).firstOrNull(::isRunnable) ?: "whisper-cli"
    }

    private val modelFile: File by lazy {
        File(
            System.getProperty("user.home"),
            ".videosvirales/models/ggml-tiny.bin",
        )
    }

    fun transcribe(audioFile: File): String {
        if (!isRunnable(executable)) {
            throw EditorException(EditorError.TRANSCRIPTION_FAILED)
        }
        val model = ensureModel()
        val outputBase = File(audioFile.parentFile, "transcript_${System.nanoTime()}")
        try {
            val process = ProcessBuilder(
                executable,
                "-m",
                model.absolutePath,
                "-f",
                audioFile.absolutePath,
                "-otxt",
                "-of",
                outputBase.absolutePath,
                "-np",
                "-nt",
                "-l",
                "auto",
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() != 0) {
                throw EditorException(EditorError.TRANSCRIPTION_FAILED)
            }
            val transcriptFile = File("${outputBase.absolutePath}.txt")
            if (!transcriptFile.isFile) {
                throw EditorException(EditorError.TRANSCRIPTION_FAILED)
            }
            return cleanTranscript(transcriptFile.readText())
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TRANSCRIPTION_FAILED, exception)
        } finally {
            File("${outputBase.absolutePath}.txt").delete()
        }
    }

    private fun cleanTranscript(text: String): String = text
        .lineSequence()
        .map { it.trim() }
        .filterNot { line ->
            line.isBlank() ||
                line == "[BLANK_AUDIO]" ||
                line == "[MUSIC]" ||
                line == "(silence)"
        }
        .joinToString(" ")
        .trim()

    private fun ensureModel(): File {
        if (modelFile.isFile && modelFile.length() > MIN_MODEL_BYTES) return modelFile
        modelFile.parentFile?.mkdirs()

        val temporaryFile = File(modelFile.parentFile, "${modelFile.name}.part")
        try {
            val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 180_000
                requestMethod = "GET"
            }
            if (connection.responseCode !in 200..299) {
                throw EditorException(EditorError.TRANSCRIPTION_FAILED)
            }
            connection.inputStream.use { input ->
                temporaryFile.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
            if (temporaryFile.length() <= MIN_MODEL_BYTES) {
                throw EditorException(EditorError.TRANSCRIPTION_FAILED)
            }
            Files.move(
                temporaryFile.toPath(),
                modelFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
            return modelFile
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TRANSCRIPTION_FAILED, exception)
        } finally {
            temporaryFile.delete()
        }
    }

    private fun isRunnable(path: String): Boolean = try {
        val process = ProcessBuilder(path, "--version")
            .redirectErrorStream(true)
            .start()
        process.inputStream.use { it.readBytes() }
        process.waitFor() == 0
    } catch (exception: Exception) {
        false
    }
}
