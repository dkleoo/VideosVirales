package com.example.videosvirales.platform.picker

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.picker_video_title
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberVideoUriPicker(onResult: (String?) -> Unit): () -> Unit {
    val title = stringResource(Res.string.picker_video_title)
    return {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> VIDEO_EXTENSIONS.any { name.endsWith(it, true) } }
        dialog.isVisible = true
        val selectedFile = dialog.file?.let { File(dialog.directory, it).absolutePath }
        onResult(selectedFile)
    }
}

private val VIDEO_EXTENSIONS = listOf(".mp4", ".mov", ".mkv", ".avi", ".webm", ".m4v")
