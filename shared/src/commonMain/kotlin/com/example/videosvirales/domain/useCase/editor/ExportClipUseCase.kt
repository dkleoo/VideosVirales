package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class ExportClipUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(
        video: VideoFile,
        clip: ClipDraft,
        narrationText: String,
    ): EditorResult<ExportedClip> = repository.exportClip(
        video = video,
        clip = clip,
        outputDirectory = repository.defaultOutputDirectory(),
        narrationText = narrationText,
    )
}
