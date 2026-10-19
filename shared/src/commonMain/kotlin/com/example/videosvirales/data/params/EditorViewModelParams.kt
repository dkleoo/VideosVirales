package com.example.videosvirales.data.params

import com.example.videosvirales.domain.useCase.editor.AnalyzeVideoUseCase
import com.example.videosvirales.domain.useCase.editor.BuildClipsUseCase
import com.example.videosvirales.domain.useCase.editor.ExportClipsUseCase
import com.example.videosvirales.domain.useCase.editor.LoadClipThumbnailUseCase
import com.example.videosvirales.domain.useCase.editor.LoadVideoUseCase
import com.example.videosvirales.domain.useCase.editor.OpenClipUseCase
import com.example.videosvirales.domain.useCase.editor.TranscribeClipUseCase

data class EditorViewModelParams(
    val loadVideo: LoadVideoUseCase,
    val analyzeVideo: AnalyzeVideoUseCase,
    val buildClips: BuildClipsUseCase,
    val exportClips: ExportClipsUseCase,
    val openClip: OpenClipUseCase,
    val loadClipThumbnail: LoadClipThumbnailUseCase,
    val transcribeClip: TranscribeClipUseCase,
)
