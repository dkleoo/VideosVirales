package com.example.videosvirales.domain.repository.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult

interface EditorRepository {
    suspend fun loadVideo(uri: String): EditorResult<VideoFile>

    suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): EditorResult<List<VideoSignal>>

    suspend fun exportClip(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
    ): EditorResult<ExportedClip>

    fun defaultOutputDirectory(): String
}
