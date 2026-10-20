package com.example.videosvirales.platform.picker

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.picker_videos_title
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberVideoUrisPicker(onResult: (List<String>) -> Unit): () -> Unit {
    val title = stringResource(Res.string.picker_videos_title)
    return {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
            isMultipleMode = true
            setFilenameFilter { _, name -> VIDEO_EXTENSIONS.any { name.endsWith(it, true) } }
        }
        dialog.isVisible = true
        val selected = dialog.files
            ?.map { file -> file.absolutePath }
            .orEmpty()
        onResult(selected)
    }
}

private val VIDEO_EXTENSIONS = listOf(".mp4", ".mov", ".mkv", ".avi", ".webm", ".m4v")
