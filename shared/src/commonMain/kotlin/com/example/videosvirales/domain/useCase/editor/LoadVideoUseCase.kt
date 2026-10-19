package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class LoadVideoUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(uri: String): EditorResult<VideoFile> = repository.loadVideo(uri)
}
