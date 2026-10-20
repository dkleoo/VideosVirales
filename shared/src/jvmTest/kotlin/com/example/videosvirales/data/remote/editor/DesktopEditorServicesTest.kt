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
    fun loadAnalyzeAndExport() = runBlocking<Unit> {
        val available = runCatching { FfmpegTools.requireAvailable() }.isSuccess
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
                narrationText = "",
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

    @Test
    fun mergeWithNarration() = runBlocking<Unit> {
        if (System.getenv("RUN_NARRATION_TEST") != "true") return@runBlocking
        val available = runCatching { FfmpegTools.requireAvailable() }.isSuccess
        if (!available) return@runBlocking

        val workingDirectory = Files.createTempDirectory("videosvirales_merge_").toFile()
        try {
            val videos = (1..2).map { index ->
                val file = File(workingDirectory, "input_$index.mp4")
                FfmpegTools.run(
                    listOf(
                        "-hide_banner",
                        "-y",
                        "-f",
                        "lavfi",
                        "-i",
                        "testsrc2=size=640x360:rate=30:duration=2",
                        "-f",
                        "lavfi",
                        "-i",
                        "sine=frequency=440:duration=2",
                        "-c:v",
                        "libx264",
                        "-pix_fmt",
                        "yuv420p",
                        "-c:a",
                        "aac",
                        "-shortest",
                        file.absolutePath,
                    )
                )
                file
            }
            val services = DesktopEditorServices()
            val loaded = videos.map { services.loadVideo(it.absolutePath) }
            val merged = services.mergeVideos(
                videos = loaded,
                narrations = listOf("This is a narrator test.", "Second scene narrator."),
                sceneAudios = listOf(null, null),
                fullAudio = null,
                aspect = VideoAspect.PORTRAIT_9_16,
                outputDirectory = File(workingDirectory, "output").absolutePath,
            )
            val mergedFile = File(merged.path)
            assertTrue(mergedFile.isFile)
            assertTrue(mergedFile.length() > 0)
            assertTrue(FfmpegTools.hasAudio(merged.path))
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    @Test
    fun piperSynthesis() = runBlocking<Unit> {
        if (System.getenv("RUN_NARRATION_TEST") != "true") return@runBlocking
        val file = PiperTtsSynthesizer().synthesize(
            "Hola, esta es una prueba de voz natural para el narrador."
        )
        try {
            assertTrue(file.isFile)
            assertTrue(file.length() > 0)
        } finally {
            file.delete()
        }
    }

    @Test
    fun exportWithNarration() = runBlocking<Unit> {
        if (System.getenv("RUN_NARRATION_TEST") != "true") return@runBlocking
        val available = runCatching { FfmpegTools.requireAvailable() }.isSuccess
        if (!available) return@runBlocking

        val workingDirectory = Files.createTempDirectory("videosvirales_narration_").toFile()
        try {
            val input = File(workingDirectory, "input.mp4")
            FfmpegTools.run(
                listOf(
                    "-hide_banner",
                    "-y",
                    "-f",
                    "lavfi",
                    "-i",
                    "testsrc2=size=640x360:rate=30:duration=2",
                    "-f",
                    "lavfi",
                    "-i",
                    "sine=frequency=440:duration=2",
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
            val clip = com.example.videosvirales.data.mappers.editor.ClipDraft(
                index = 1,
                startMs = 0,
                endMs = 2_000,
                score = 0f,
                subtitle = "",
                narration = "",
                aspect = VideoAspect.PORTRAIT_9_16,
            )
            val exported = services.exportClip(
                video = video,
                clip = clip,
                outputDirectory = File(workingDirectory, "output").absolutePath,
                narrationText = "This is a narrator test.",
            )
            val exportedFile = File(exported.path)
            assertTrue(exportedFile.isFile)
            assertTrue(exportedFile.length() > 0)
            assertTrue(FfmpegTools.hasAudio(exported.path))
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    @Test
    fun exportWithCustomAudio() = runBlocking<Unit> {
        val available = runCatching { FfmpegTools.requireAvailable() }.isSuccess
        if (!available) return@runBlocking

        val workingDirectory = Files.createTempDirectory("videosvirales_custom_audio_").toFile()
        try {
            val input = File(workingDirectory, "input.mp4")
            FfmpegTools.run(
                listOf(
                    "-hide_banner",
                    "-y",
                    "-f",
                    "lavfi",
                    "-i",
                    "testsrc2=size=640x360:rate=30:duration=2",
                    "-f",
                    "lavfi",
                    "-i",
                    "sine=frequency=440:duration=2",
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
            val customAudio = File(workingDirectory, "custom.wav")
            FfmpegTools.run(
                listOf(
                    "-hide_banner",
                    "-y",
                    "-f",
                    "lavfi",
                    "-i",
                    "sine=frequency=880:duration=2",
                    "-c:a",
                    "pcm_s16le",
                    customAudio.absolutePath,
                )
            )

            val services = DesktopEditorServices()
            val video = services.loadVideo(input.absolutePath)
            val audio = services.loadAudio(customAudio.absolutePath)
            val clip = com.example.videosvirales.data.mappers.editor.ClipDraft(
                index = 1,
                startMs = 0,
                endMs = 2_000,
                score = 0f,
                subtitle = "",
                narration = "This narration should be ignored.",
                audio = audio,
                aspect = VideoAspect.PORTRAIT_9_16,
            )
            val exported = services.exportClip(
                video = video,
                clip = clip,
                outputDirectory = File(workingDirectory, "output").absolutePath,
                narrationText = "This narration should be ignored.",
            )
            val exportedFile = File(exported.path)
            assertTrue(exportedFile.isFile)
            assertTrue(exportedFile.length() > 0)
            assertTrue(FfmpegTools.hasAudio(exported.path))
        } finally {
            workingDirectory.deleteRecursively()
        }
    }
}
