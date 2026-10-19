package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class AnalyzeVideoUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(
        video: VideoFile,
        onProgress: (Float) -> Unit,
    ): EditorResult<List<VideoSignal>> = repository.analyze(video, onProgress)
}
