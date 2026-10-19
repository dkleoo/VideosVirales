package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DesktopVideoThumbnailer {
    suspend fun load(
        video: VideoFile,
        timestampMs: Long,
        aspect: VideoAspect,
    ): ByteArray = withContext(Dispatchers.IO) {
        FfmpegTools.runForBytes(
            listOf(
                "-hide_banner",
                "-nostdin",
                "-ss",
                formatSeconds(timestampMs),
                "-i",
                video.localPath(),
                "-frames:v",
                "1",
                "-vf",
                "scale=${aspect.thumbnailWidth}:${aspect.thumbnailHeight}:" +
                    "force_original_aspect_ratio=increase," +
                    "crop=${aspect.thumbnailWidth}:${aspect.thumbnailHeight}",
                "-f",
                "image2",
                "-vcodec",
                "mjpeg",
                "-",
            )
        )
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
