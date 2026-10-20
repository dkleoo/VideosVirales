package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class MergeVideosUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(
        videos: List<VideoFile>,
        narrations: List<String>,
        sceneAudios: List<AudioFile?>,
        fullAudio: AudioFile?,
        aspect: VideoAspect,
    ): EditorResult<ExportedClip> = repository.mergeVideos(
        videos = videos,
        narrations = narrations,
        sceneAudios = sceneAudios,
        fullAudio = fullAudio,
        aspect = aspect,
        outputDirectory = repository.defaultOutputDirectory(),
    )
}
