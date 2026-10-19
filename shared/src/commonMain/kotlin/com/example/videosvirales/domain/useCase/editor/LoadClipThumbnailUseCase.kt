package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class LoadClipThumbnailUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(
        video: VideoFile,
        timestampMs: Long,
        aspect: VideoAspect,
    ): EditorResult<ByteArray> = repository.loadThumbnail(video, timestampMs, aspect)
}
