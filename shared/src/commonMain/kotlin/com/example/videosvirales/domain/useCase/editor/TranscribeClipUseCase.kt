package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class TranscribeClipUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(
        video: VideoFile,
        startMs: Long,
        endMs: Long,
    ): EditorResult<String> = repository.transcribe(video, startMs, endMs)
}
