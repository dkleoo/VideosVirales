package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoAspect
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
        narrationText: String,
    ): ExportedClip

    suspend fun mergeVideos(
        videos: List<VideoFile>,
        narrations: List<String>,
        sceneAudios: List<AudioFile?>,
        fullAudio: AudioFile?,
        aspect: VideoAspect,
        outputDirectory: String,
    ): ExportedClip

    suspend fun loadAudio(uri: String): AudioFile

    suspend fun loadThumbnail(
        video: VideoFile,
        timestampMs: Long,
        aspect: VideoAspect,
    ): ByteArray

    suspend fun transcribe(
        video: VideoFile,
        startMs: Long,
        endMs: Long,
    ): String

    suspend fun openClip(uri: String)

    fun defaultOutputDirectory(): String
}
