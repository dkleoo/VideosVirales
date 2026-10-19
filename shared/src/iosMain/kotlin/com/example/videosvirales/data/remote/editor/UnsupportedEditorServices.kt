package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorException

class UnsupportedEditorServices : EditorServices {
    override suspend fun loadVideo(uri: String): VideoFile =
        throw EditorException(EditorError.UNSUPPORTED)

    override suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal> = throw EditorException(EditorError.UNSUPPORTED)

    override suspend fun exportClip(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
    ): ExportedClip = throw EditorException(EditorError.UNSUPPORTED)

    override suspend fun openClip(uri: String) = throw EditorException(EditorError.UNSUPPORTED)

    override fun defaultOutputDirectory(): String = ""
}
