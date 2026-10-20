package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.Locale

class DesktopVideoExporter {
    private val tts = DesktopTtsSynthesizer()

    suspend fun export(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
        narrationText: String,
    ): ExportedClip = withContext(Dispatchers.IO) {
        var narrationFile: File? = null
        var baseFile: File? = null
        try {
            FfmpegTools.requireAvailable()
            val customAudio = clip.audio
            narrationFile = if (customAudio == null && narrationText.isNotBlank()) {
                tts.synthesize(narrationText)
            } else {
                null
            }
            val outputDir = File(outputDirectory).apply { mkdirs() }
            val outputFile = File(
                outputDir,
                "clip_${clip.index.toString().padStart(2, '0')}_${System.currentTimeMillis()}.mp4",
            )

            val tracks = when {
                customAudio != null -> listOf(
                    ExternalAudio(
                        path = customAudio.uri,
                        offsetMs = 0,
                        durationMs = clip.durationMs,
                    )
                )
                narrationFile != null -> listOf(
                    ExternalAudio(
                        path = narrationFile.absolutePath,
                        offsetMs = 0,
                        durationMs = clip.durationMs,
                    )
                )
                else -> emptyList()
            }

            if (tracks.isEmpty()) {
                exportBase(video, clip, outputFile)
            } else {
                baseFile = Files.createTempFile("videosvirales_base_", ".mp4").toFile()
                exportBase(video, clip, baseFile)
                mixAdditionalAudio(baseFile, tracks, outputFile)
            }

            ExportedClip(
                index = clip.index,
                path = outputFile.absolutePath,
                displayName = outputFile.name,
                uri = outputFile.toURI().toString(),
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.EXPORT_FAILED, exception)
        } finally {
            narrationFile?.delete()
            baseFile?.delete()
        }
    }

    suspend fun mergeVideos(
        videos: List<VideoFile>,
        narrations: List<String>,
        sceneAudios: List<AudioFile?>,
        fullAudio: AudioFile?,
        aspect: VideoAspect,
        outputDirectory: String,
    ): ExportedClip = withContext(Dispatchers.IO) {
        if (videos.size < 2) throw EditorException(EditorError.EXPORT_FAILED)
        val narrationFiles = mutableListOf<File>()
        var baseFile: File? = null
        try {
            FfmpegTools.requireAvailable()
            val outputDir = File(outputDirectory).apply { mkdirs() }
            val outputFile = File(outputDir, "merged_${System.currentTimeMillis()}.mp4")

            val tracks = mutableListOf<ExternalAudio>()
            var offsetMs = 0L
            videos.forEachIndexed { index, video ->
                val sceneAudio = sceneAudios.getOrNull(index)
                if (sceneAudio != null) {
                    tracks += ExternalAudio(
                        path = sceneAudio.uri,
                        offsetMs = offsetMs,
                        durationMs = video.durationMs,
                    )
                } else {
                    val text = narrations.getOrElse(index) { "" }
                    if (text.isNotBlank()) {
                        val file = tts.synthesize(text)
                        narrationFiles += file
                        tracks += ExternalAudio(
                            path = file.absolutePath,
                            offsetMs = offsetMs,
                            durationMs = video.durationMs,
                        )
                    }
                }
                offsetMs += video.durationMs
            }
            fullAudio?.let { audio ->
                tracks += ExternalAudio(
                    path = audio.uri,
                    offsetMs = 0,
                    durationMs = videos.sumOf { it.durationMs }.coerceAtLeast(1),
                )
            }

            baseFile = Files.createTempFile("videosvirales_merge_", ".mp4").toFile()
            FfmpegTools.run(
                arguments = buildList {
                    add("-hide_banner")
                    add("-nostdin")
                    add("-y")
                    videos.forEach { video -> addAll(listOf("-i", video.localPath())) }
                    add("-filter_complex")
                    add(buildMergeFilter(videos, aspect))
                    add("-map")
                    add("[v]")
                    add("-map")
                    add("[a]")
                    add("-c:v")
                    add("libx264")
                    add("-preset")
                    add("veryfast")
                    add("-crf")
                    add("22")
                    add("-pix_fmt")
                    add("yuv420p")
                    add("-c:a")
                    add("aac")
                    add("-b:a")
                    add("192k")
                    add("-movflags")
                    add("+faststart")
                    add(baseFile.absolutePath)
                }
            )
            if (tracks.isEmpty()) {
                baseFile.copyTo(outputFile, overwrite = true)
            } else {
                mixAdditionalAudio(baseFile, tracks, outputFile)
            }

            ExportedClip(
                index = 1,
                path = outputFile.absolutePath,
                displayName = outputFile.name,
                uri = outputFile.toURI().toString(),
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.EXPORT_FAILED, exception)
        } finally {
            narrationFiles.forEach { file -> file.delete() }
            baseFile?.delete()
        }
    }

    private fun exportBase(
        video: VideoFile,
        clip: ClipDraft,
        outputFile: File,
    ) {
        val hasSubtitle = clip.subtitle.isNotBlank()
        if (hasSubtitle) FfmpegTools.requireFilters("subtitles")

        val temporaryDirectory = Files.createTempDirectory("videosvirales_clip_").toFile()
        try {
            if (hasSubtitle) {
                AssSubtitleWriter.write(
                    file = File(temporaryDirectory, SUBTITLE_FILE),
                    text = clip.subtitle,
                    durationMs = clip.durationMs,
                    width = clip.aspect.width,
                    height = clip.aspect.height,
                )
            }
            FfmpegTools.run(
                arguments = buildList {
                    add("-hide_banner")
                    add("-nostdin")
                    add("-y")
                    add("-ss")
                    add(formatSeconds(clip.startMs))
                    add("-i")
                    add(video.localPath())
                    add("-t")
                    add(formatSeconds(clip.durationMs))
                    add("-vf")
                    add(buildFilters(clip, hasSubtitle))
                    add("-c:v")
                    add("libx264")
                    add("-preset")
                    add("veryfast")
                    add("-crf")
                    add("22")
                    add("-pix_fmt")
                    add("yuv420p")
                    add("-c:a")
                    add("aac")
                    add("-b:a")
                    add("192k")
                    add("-movflags")
                    add("+faststart")
                    add(outputFile.absolutePath)
                },
                workingDirectory = temporaryDirectory,
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun mixAdditionalAudio(
        baseFile: File,
        tracks: List<ExternalAudio>,
        outputFile: File,
    ) {
        val hasBaseAudio = FfmpegTools.hasAudio(baseFile.absolutePath)
        val filterParts = mutableListOf<String>()
        val labels = mutableListOf<String>()
        if (hasBaseAudio) {
            filterParts += "[0:a]volume=1.0[a0]"
            labels += "[a0]"
        }
        tracks.forEachIndexed { index, track ->
            val inputIndex = index + 1
            filterParts += "[$inputIndex:a]" +
                "atrim=0:${formatSeconds(track.durationMs)}," +
                "adelay=${track.offsetMs}|${track.offsetMs}," +
                "volume=1.0[ext$index]"
            labels += "[ext$index]"
        }
        val outputLabel = if (labels.size == 1) {
            labels.first()
        } else {
            filterParts += "${labels.joinToString("")}amix=inputs=${labels.size}:" +
                "duration=${if (hasBaseAudio) "first" else "longest"}:" +
                "dropout_transition=2[aout]"
            "[aout]"
        }

        val arguments = mutableListOf<String>()
        arguments += listOf("-hide_banner", "-nostdin", "-y")
        arguments += listOf("-i", baseFile.absolutePath)
        tracks.forEach { track -> arguments += listOf("-i", track.path) }
        arguments += listOf(
            "-filter_complex",
            filterParts.joinToString(";"),
            "-map",
            "0:v:0",
            "-map",
            outputLabel,
            "-c:v",
            "copy",
            "-c:a",
            "aac",
            "-b:a",
            "192k",
            "-movflags",
            "+faststart",
            outputFile.absolutePath,
        )
        FfmpegTools.run(arguments)
    }

    private fun buildMergeFilter(
        videos: List<VideoFile>,
        aspect: VideoAspect,
    ): String {
        val parts = mutableListOf<String>()
        videos.forEachIndexed { index, video ->
            parts += "[$index:v]scale=${aspect.width}:${aspect.height}:" +
                "force_original_aspect_ratio=increase," +
                "crop=${aspect.width}:${aspect.height},setsar=1,fps=30[v$index]"
            if (FfmpegTools.hasAudio(video.localPath())) {
                parts += "[$index:a]aresample=48000," +
                    "aformat=sample_fmts=fltp:channel_layouts=stereo[a$index]"
            } else {
                parts += "anullsrc=r=48000:cl=stereo," +
                    "atrim=0:${formatSeconds(video.durationMs)},asetpts=N/SR/TB[a$index]"
            }
        }
        val videoInputs = videos.indices.joinToString("") { "[v$it]" }
        val audioInputs = videos.indices.joinToString("") { "[a$it]" }
        parts += "${videoInputs}concat=n=${videos.size}:v=1:a=0[v]"
        parts += "${audioInputs}concat=n=${videos.size}:v=0:a=1[a]"
        return parts.joinToString(";")
    }

    private fun buildFilters(
        clip: ClipDraft,
        hasSubtitle: Boolean,
    ): String = buildList {
        add("scale=${clip.aspect.width}:${clip.aspect.height}:force_original_aspect_ratio=increase")
        add("crop=${clip.aspect.width}:${clip.aspect.height}")
        add("setsar=1")
        if (hasSubtitle) {
            add("subtitles=$SUBTITLE_FILE")
        }
    }.joinToString(",")

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

    private data class ExternalAudio(
        val path: String,
        val offsetMs: Long,
        val durationMs: Long,
    )

    private companion object {
        const val SUBTITLE_FILE = "subtitles.ass"
    }
}
