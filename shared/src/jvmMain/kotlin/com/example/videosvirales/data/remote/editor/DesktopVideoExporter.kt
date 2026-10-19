package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.ClipAnimation
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.Locale

class DesktopVideoExporter {
    suspend fun export(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
    ): ExportedClip = withContext(Dispatchers.IO) {
        try {
            FfmpegTools.requireAvailable()
            val hasSubtitle = clip.subtitle.isNotBlank()
            val hasZoom = clip.animation == ClipAnimation.ZOOM_IN ||
                clip.animation == ClipAnimation.ZOOM_OUT
            if (hasSubtitle) FfmpegTools.requireFilters("subtitles")
            if (hasZoom) FfmpegTools.requireFilters("zoompan")

            val outputDir = File(outputDirectory).apply { mkdirs() }
            val outputFile = File(
                outputDir,
                "clip_${clip.index.toString().padStart(2, '0')}_${System.currentTimeMillis()}.mp4",
            )
            val temporaryDirectory = Files.createTempDirectory("videosvirales_clip_").toFile()

            try {
                if (hasSubtitle) {
                    AssSubtitleWriter.write(
                        file = File(temporaryDirectory, SUBTITLE_FILE),
                        text = clip.subtitle,
                        durationMs = clip.durationMs,
                        animation = clip.animation,
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
                        add(buildFilters(clip, hasSubtitle, hasZoom))
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

            ExportedClip(
                index = clip.index,
                path = outputFile.absolutePath,
                displayName = outputFile.name,
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.EXPORT_FAILED, exception)
        }
    }

    private fun buildFilters(
        clip: ClipDraft,
        hasSubtitle: Boolean,
        hasZoom: Boolean,
    ): String = buildList {
        add("scale=1080:1920:force_original_aspect_ratio=increase")
        add("crop=1080:1920")
        add("setsar=1")
        if (hasZoom) {
            add(
                when (clip.animation) {
                    ClipAnimation.ZOOM_IN ->
                        "zoompan=z='min(1.0+0.0006*on,1.12)':d=1:s=1080x1920:fps=30"
                    ClipAnimation.ZOOM_OUT ->
                        "zoompan=z='max(1.12-0.0006*on,1.0)':d=1:s=1080x1920:fps=30"
                    else -> ""
                }
            )
        }
        if (hasSubtitle) {
            add("subtitles=$SUBTITLE_FILE")
        }
    }.filter { it.isNotBlank() }.joinToString(",")

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

    private companion object {
        const val SUBTITLE_FILE = "subtitles.ass"
    }
}
