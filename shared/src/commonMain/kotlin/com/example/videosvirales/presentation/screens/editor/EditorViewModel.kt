package com.example.videosvirales.presentation.screens.editor

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorConfig
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.params.EditorViewModelParams
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val params: EditorViewModelParams,
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
                    thumbnails = emptyMap(),
                )
            }
            when (val result = params.loadVideo(uri)) {
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

    fun updateAspect(aspect: VideoAspect) {
        val current = _state.value
        _state.update {
            it.copy(
                aspect = aspect,
                clips = it.clips.map { clip -> clip.copy(aspect = aspect) },
                thumbnails = emptyMap(),
            )
        }
        val video = current.video
        if (video != null && current.clips.isNotEmpty()) {
            screenModelScope.launch {
                loadThumbnails(video, current.clips.map { it.copy(aspect = aspect) }, aspect)
            }
        }
    }

    fun generateClips() {
        val current = _state.value
        val video = current.video ?: return
        val clips = params.buildClips(
            video = video,
            signals = current.signals,
            config = EditorConfig(
                clipCount = current.clipCount,
                clipDurationMs = current.clipDurationSeconds * 1_000L,
                aspect = current.aspect,
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
                thumbnails = emptyMap(),
                isPreparingClips = true,
                subtitlesNotice = false,
            )
        }
        screenModelScope.launch {
            try {
                prepareClips(video, clips)
            } finally {
                _state.update { it.copy(isPreparingClips = false) }
            }
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
            when (val result = params.exportClips(video, clips) { progress ->
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

    fun openClip(clip: ExportedClip) {
        screenModelScope.launch {
            when (val result = params.openClip(clip)) {
                is EditorResult.Success -> _state.update { it.copy(error = null) }
                is EditorResult.Error -> _state.update { it.copy(error = result.error) }
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
        when (val result = params.analyzeVideo(video) { progress ->
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

    private suspend fun prepareClips(video: VideoFile, clips: List<ClipDraft>) {
        clips.forEach { clip ->
            loadThumbnail(video, clip)
            transcribe(video, clip)
        }
    }

    private suspend fun loadThumbnails(
        video: VideoFile,
        clips: List<ClipDraft>,
        aspect: VideoAspect,
    ) {
        _state.update { it.copy(isPreparingClips = true, thumbnails = emptyMap()) }
        try {
            clips.forEach { clip -> loadThumbnail(video, clip) }
        } finally {
            if (_state.value.aspect == aspect) {
                _state.update { it.copy(isPreparingClips = false) }
            }
        }
    }

    private suspend fun loadThumbnail(video: VideoFile, clip: ClipDraft) {
        val timestampMs = (clip.startMs + clip.endMs) / 2
        when (val result = params.loadClipThumbnail(video, timestampMs, clip.aspect)) {
            is EditorResult.Success -> {
                if (_state.value.aspect == clip.aspect) {
                    _state.update { it.copy(thumbnails = it.thumbnails + (clip.index to result.data)) }
                }
            }
            is EditorResult.Error -> Unit
        }
    }

    private suspend fun transcribe(video: VideoFile, clip: ClipDraft) {
        when (val result = params.transcribeClip(video, clip.startMs, clip.endMs)) {
            is EditorResult.Success -> {
                val transcript = formatSubtitle(result.data)
                if (transcript.isNotBlank()) {
                    _state.update { state ->
                        state.copy(
                            clips = state.clips.map { current ->
                                if (current.index == clip.index && current.subtitle.isBlank()) {
                                    current.copy(subtitle = transcript)
                                } else {
                                    current
                                }
                            },
                        )
                    }
                }
            }
            is EditorResult.Error -> {
                _state.update { it.copy(subtitlesNotice = true) }
            }
        }
    }

    private fun formatSubtitle(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed
        val capitalized = trimmed.replaceFirstChar { character ->
            if (character.isLowerCase()) character.uppercase() else character.toString()
        }
        return if (capitalized.last() in ".!?") capitalized else "$capitalized."
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
    val thumbnails: Map<Int, ByteArray> = emptyMap(),
    val clipCount: Int = 3,
    val clipDurationSeconds: Int = 15,
    val aspect: VideoAspect = VideoAspect.PORTRAIT_9_16,
    val progress: Float = 0f,
    val isPreparingClips: Boolean = false,
    val subtitlesNotice: Boolean = false,
    val error: EditorError? = null,
)
