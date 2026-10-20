package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class LoadAudioUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(uri: String): EditorResult<AudioFile> = repository.loadAudio(uri)
}
