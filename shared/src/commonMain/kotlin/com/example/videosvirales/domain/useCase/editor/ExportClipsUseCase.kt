package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.repository.editor.EditorRepository

class ExportClipsUseCase(
    private val repository: EditorRepository,
) {
    suspend operator fun invoke(
        video: VideoFile,
        clips: List<ClipDraft>,
        onProgress: (Float) -> Unit,
    ): EditorResult<List<ExportedClip>> {
        if (clips.isEmpty()) return EditorResult.Success(emptyList())

        val outputDirectory = repository.defaultOutputDirectory()
        val exported = mutableListOf<ExportedClip>()

        clips.forEachIndexed { index, clip ->
            when (val result = repository.exportClip(video, clip, outputDirectory)) {
                is EditorResult.Success -> exported += result.data
                is EditorResult.Error -> return result
            }
            onProgress((index + 1).toFloat() / clips.size)
        }

        return EditorResult.Success(exported)
    }
}
