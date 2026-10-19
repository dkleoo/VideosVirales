package com.example.videosvirales.presentation.screens.editor

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.videosvirales.data.mappers.editor.ClipAnimation
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorConfig
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import com.example.videosvirales.domain.useCase.editor.AnalyzeVideoUseCase
import com.example.videosvirales.domain.useCase.editor.BuildClipsUseCase
import com.example.videosvirales.domain.useCase.editor.ExportClipsUseCase
import com.example.videosvirales.domain.useCase.editor.LoadVideoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val loadVideo: LoadVideoUseCase,
    private val analyzeVideo: AnalyzeVideoUseCase,
    private val buildClips: BuildClipsUseCase,
    private val exportClips: ExportClipsUseCase,
) : ScreenModel {
    private val _state = MutableStateFlow(EditorScreenState())
    val state: StateFlow<EditorScreenState> = _state.asStateFlow()

    fun onVideoPicked(uri: String?) {
        if (uri.isNullOrBlank()) return
        if (_state.value.step == EditorStep.LOADING_VIDEO ||
            _state.value.step == EditorStep.ANALYZING ||
            _state.value.step == EditorStep.EXPORTING
        ) {
            return
        }

        screenModelScope.launch {
            _state.update {
                it.copy(
                    step = EditorStep.LOADING_VIDEO,
                    progress = 0f,
                    error = null,
                    video = null,
                    signals = emptyList(),
                    clips = emptyList(),
                    exportedClips = emptyList(),
                )
            }
            when (val result = loadVideo(uri)) {
                is EditorResult.Success -> {
                    _state.update {
                        it.copy(
                            video = result.data,
                            step = EditorStep.ANALYZING,
                            progress = 0f,
                        )
                    }
                    analyze(result.data)
                }
                is EditorResult.Error -> {
                    _state.update {
                        it.copy(
                            step = EditorStep.UPLOAD,
                            progress = 0f,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun retryAnalysis() {
        val video = _state.value.video ?: return
        if (_state.value.step == EditorStep.ANALYZING) return
        screenModelScope.launch {
            analyze(video)
        }
    }

    fun updateClipCount(value: Int) {
        _state.update { it.copy(clipCount = value.coerceIn(1, 10)) }
    }

    fun updateClipDuration(seconds: Int) {
        _state.update { it.copy(clipDurationSeconds = seconds.coerceIn(5, 60)) }
    }

    fun updateAnimation(animation: ClipAnimation) {
        _state.update { it.copy(animation = animation) }
    }

    fun generateClips() {
        val current = _state.value
        val video = current.video ?: return
        val clips = buildClips(
            video = video,
            signals = current.signals,
            config = EditorConfig(
                clipCount = current.clipCount,
                clipDurationMs = current.clipDurationSeconds * 1_000L,
                animation = current.animation,
            ),
        )
        if (clips.isEmpty()) {
            _state.update { it.copy(error = EditorError.ANALYSIS_FAILED) }
            return
        }
        _state.update {
            it.copy(
                clips = clips,
                step = EditorStep.REVIEW,
                error = null,
            )
        }
    }

    fun updateSubtitle(index: Int, subtitle: String) {
        _state.update { state ->
            state.copy(
                clips = state.clips.map { clip ->
                    if (clip.index == index) clip.copy(subtitle = subtitle) else clip
                },
            )
        }
    }

    fun updateClipAnimation(index: Int, animation: ClipAnimation) {
        _state.update { state ->
            state.copy(
                clips = state.clips.map { clip ->
                    if (clip.index == index) clip.copy(animation = animation) else clip
                },
            )
        }
    }

    fun backToSettings() {
        _state.update { it.copy(step = EditorStep.SETTINGS, error = null) }
    }

    fun export() {
        val current = _state.value
        val video = current.video ?: return
        val clips = current.clips
        if (clips.isEmpty() || current.step == EditorStep.EXPORTING) return

        screenModelScope.launch {
            _state.update {
                it.copy(
                    step = EditorStep.EXPORTING,
                    progress = 0f,
                    error = null,
                    exportedClips = emptyList(),
                )
            }
            when (val result = exportClips(video, clips) { progress ->
                _state.update { state -> state.copy(progress = progress) }
            }) {
                is EditorResult.Success -> {
                    _state.update {
                        it.copy(
                            step = EditorStep.DONE,
                            progress = 1f,
                            exportedClips = result.data,
                        )
                    }
                }
                is EditorResult.Error -> {
                    _state.update {
                        it.copy(
                            step = EditorStep.REVIEW,
                            progress = 0f,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun restart() {
        _state.value = EditorScreenState()
    }

    private suspend fun analyze(video: VideoFile) {
        _state.update {
            it.copy(
                step = EditorStep.ANALYZING,
                progress = 0f,
                error = null,
            )
        }
        when (val result = analyzeVideo(video) { progress ->
            _state.update { state -> state.copy(progress = progress) }
        }) {
            is EditorResult.Success -> {
                _state.update {
                    it.copy(
                        signals = result.data,
                        step = EditorStep.SETTINGS,
                        progress = 1f,
                    )
                }
            }
            is EditorResult.Error -> {
                _state.update {
                    it.copy(
                        step = EditorStep.UPLOAD,
                        progress = 0f,
                        error = result.error,
                    )
                }
            }
        }
    }
}

enum class EditorStep {
    UPLOAD,
    LOADING_VIDEO,
    ANALYZING,
    SETTINGS,
    REVIEW,
    EXPORTING,
    DONE,
}

data class EditorScreenState(
    val step: EditorStep = EditorStep.UPLOAD,
    val video: VideoFile? = null,
    val signals: List<VideoSignal> = emptyList(),
    val clips: List<ClipDraft> = emptyList(),
    val exportedClips: List<ExportedClip> = emptyList(),
    val clipCount: Int = 3,
    val clipDurationSeconds: Int = 15,
    val animation: ClipAnimation = ClipAnimation.ZOOM_IN,
    val progress: Float = 0f,
    val error: EditorError? = null,
)
