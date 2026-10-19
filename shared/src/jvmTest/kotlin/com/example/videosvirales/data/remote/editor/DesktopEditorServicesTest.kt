package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.EditorConfig
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.domain.useCase.editor.BuildClipsUseCase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopEditorServicesTest {

    @Test
    fun loadAnalyzeAndExport() = runBlocking<Unit> {        val available = runCatching { FfmpegTools.requireAvailable() }.isSuccess
        if (!available) return@runBlocking

        val workingDirectory = Files.createTempDirectory("videosvirales_test_").toFile()
        try {
            val input = File(workingDirectory, "sample.mp4")
            FfmpegTools.run(
                listOf(
                    "-hide_banner",
                    "-y",
                    "-f",
                    "lavfi",
                    "-i",
                    "testsrc2=size=640x360:rate=30:duration=6",
                    "-f",
                    "lavfi",
                    "-i",
                    "sine=frequency=440:duration=6",
                    "-c:v",
                    "libx264",
                    "-pix_fmt",
                    "yuv420p",
                    "-c:a",
                    "aac",
                    "-shortest",
                    input.absolutePath,
                )
            )

            val services = DesktopEditorServices()
            val video = services.loadVideo(input.absolutePath)
            assertTrue(video.durationMs > 0)

            val signals = services.analyze(video) { }
            assertTrue(signals.isNotEmpty())

            val thumbnail = services.loadThumbnail(
                video = video,
                timestampMs = 1_500,
                aspect = VideoAspect.PORTRAIT_9_16,
            )
            assertTrue(thumbnail.isNotEmpty())

            val clips = BuildClipsUseCase()(
                video = video,
                signals = signals,
                config = EditorConfig(
                    clipCount = 2,
                    clipDurationMs = 3_000,
                    aspect = VideoAspect.PORTRAIT_9_16,
                ),
            )
            assertEquals(2, clips.size)

            val exported = services.exportClip(
                video = video,
                clip = clips.first().copy(subtitle = "Test subtitle"),
                outputDirectory = File(workingDirectory, "output").absolutePath,
            )
            val exportedFile = File(exported.path)
            assertTrue(exportedFile.isFile)
            assertTrue(exportedFile.length() > 0)
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    @Test
    fun transcribeSpeech() = runBlocking<Unit> {
        if (System.getenv("RUN_SPEECH_TEST") != "true") return@runBlocking
        val whisperAvailable = runCatching {
            ProcessBuilder("whisper-cli", "--version")
                .redirectErrorStream(true)
                .start()
                .also { process -> process.inputStream.use { it.readBytes() } }
                .waitFor() == 0
        }.getOrDefault(false)
        if (!whisperAvailable) return@runBlocking

        val workingDirectory = Files.createTempDirectory("videosvirales_speech_").toFile()
        try {
            val speechFile = File(workingDirectory, "speech.aiff")
            val sayProcess = ProcessBuilder(
                "say",
                "-o",
                speechFile.absolutePath,
                "Hola, este es un video de prueba para subtitulos automaticos",
            ).redirectErrorStream(true).start()
            sayProcess.inputStream.use { it.readBytes() }
            if (sayProcess.waitFor() != 0) return@runBlocking

            val videoFile = File(workingDirectory, "speech.mp4")
            FfmpegTools.run(
                listOf(
                    "-hide_banner",
                    "-y",
                    "-f",
                    "lavfi",
                    "-i",
                    "testsrc2=size=640x360:rate=30:duration=6",
                    "-i",
                    speechFile.absolutePath,
                    "-c:v",
                    "libx264",
                    "-pix_fmt",
                    "yuv420p",
                    "-c:a",
                    "aac",
                    "-shortest",
                    videoFile.absolutePath,
                )
            )

            val services = DesktopEditorServices()
            val video = services.loadVideo(videoFile.absolutePath)
            val transcript = services.transcribe(video, 0, 6_000)
            assertTrue(transcript.isNotBlank())
        } finally {
            workingDirectory.deleteRecursively()
        }
    }
}
