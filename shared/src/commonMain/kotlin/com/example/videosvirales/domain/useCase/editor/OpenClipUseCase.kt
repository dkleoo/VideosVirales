package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class OpenClipUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(clip: ExportedClip): EditorResult<Unit> = repository.openClip(clip)
}
