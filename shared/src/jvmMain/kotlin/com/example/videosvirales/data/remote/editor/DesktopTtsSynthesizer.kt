package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DesktopTtsSynthesizer {
    private val piper = PiperTtsSynthesizer()

    suspend fun synthesize(text: String): File = withContext(Dispatchers.IO) {
        try {
            piper.synthesize(text)
        } catch (exception: EditorException) {
            synthesizeSystem(text)
        }
    }

    private suspend fun synthesizeSystem(text: String): File = withContext(Dispatchers.IO) {
        val operatingSystem = System.getProperty("os.name").lowercase(Locale.US)
        val file = when {
            operatingSystem.contains("mac") -> File(
                System.getProperty("java.io.tmpdir"),
                "videosvirales_narration_${System.nanoTime()}.aiff",
            )
            operatingSystem.contains("win") -> File(
                System.getProperty("java.io.tmpdir"),
                "videosvirales_narration_${System.nanoTime()}.wav",
            )
            else -> File(
                System.getProperty("java.io.tmpdir"),
                "videosvirales_narration_${System.nanoTime()}.wav",
            )
        }

        try {
            val command = when {
                operatingSystem.contains("mac") -> listOf(
                    "say",
                    "-o",
                    file.absolutePath,
                    text,
                )
                operatingSystem.contains("win") -> listOf(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    windowsScript(text, file),
                )
                else -> {
                    val executable = if (isRunnable("espeak-ng")) "espeak-ng" else "espeak"
                    listOf(executable, "-w", file.absolutePath, text)
                }
            }
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() != 0 || !file.isFile || file.length() == 0L) {
                throw EditorException(EditorError.TTS_FAILED)
            }
            file
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TTS_FAILED, exception)
        }
    }

    private fun windowsScript(text: String, file: File): String {
        val escapedText = text.replace("'", "''")
        val escapedPath = file.absolutePath.replace("'", "''")
        return "Add-Type -AssemblyName System.Speech; " +
            "\$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
            "\$speaker.SetOutputToWaveFile('$escapedPath'); " +
            "\$speaker.Speak('$escapedText'); " +
            "\$speaker.Dispose()"
    }

    private fun isRunnable(command: String): Boolean = try {
        val process = ProcessBuilder(command, "--version")
            .redirectErrorStream(true)
            .start()
        process.inputStream.use { it.readBytes() }
        process.waitFor() == 0
    } catch (exception: Exception) {
        false
    }
}
