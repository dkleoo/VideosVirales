package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal

interface EditorServices {
    suspend fun loadVideo(uri: String): VideoFile

    suspend fun analyze(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): List<VideoSignal>

    suspend fun exportClip(
        video: VideoFile,
        clip: ClipDraft,
        outputDirectory: String,
    ): ExportedClip

    suspend fun openClip(uri: String)

    fun defaultOutputDirectory(): String
}
