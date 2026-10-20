package com.example.videosvirales.presentation.screens.editor

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorConfig
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import com.example.videosvirales.data.params.EditorViewModelParams
import com.example.videosvirales.data.repositoyImpl.editor.EditorResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min

class EditorViewModel(
    private val params: EditorViewModelParams,
) : ScreenModel {
    private val _state = MutableStateFlow(EditorScreenState())
    val state: StateFlow<EditorScreenState> = _state.asStateFlow()
    private var manualPreviewJob: Job? = null

    fun setMode(mode: EditorMode) {
        if (_state.value.mode == mode && _state.value.step == EditorStep.UPLOAD) return
        manualPreviewJob?.cancel()
        _state.value = EditorScreenState(mode = mode)
    }

    fun onVideoPicked(uri: String?) {
        if (_state.value.mode == EditorMode.MERGE_VIDEOS) return
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
                    manualThumbnail = null,
                )
            }
            when (val result = params.loadVideo(uri)) {
                is EditorResult.Success -> {
                    if (_state.value.mode == EditorMode.MANUAL_TRIM) {
                        startManual(result.data)
                    } else {
                        _state.update {
                            it.copy(
                                video = result.data,
                                step = EditorStep.ANALYZING,
                                progress = 0f,
                            )
                        }
                        analyze(result.data)
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

    fun onVideosPicked(uris: List<String>) {
        if (_state.value.mode != EditorMode.MERGE_VIDEOS) return
        if (uris.isEmpty()) return
        if (uris.size < 2) {
            _state.update { it.copy(step = EditorStep.UPLOAD, error = EditorError.MERGE_FAILED) }
            return
        }

        screenModelScope.launch {
            _state.update {
                it.copy(
                    step = EditorStep.LOADING_VIDEO,
                    progress = 0f,
                    error = null,
                    mergeVideos = emptyList(),
                    mergeNarrations = emptyMap(),
                    mergeAudios = emptyMap(),
                    mergeThumbnails = emptyMap(),
                    fullAudio = null,
                    exportedClips = emptyList(),
                )
            }
            val videos = mutableListOf<VideoFile>()
            uris.forEachIndexed { index, uri ->
                when (val result = params.loadVideo(uri)) {
                    is EditorResult.Success -> videos += result.data
                    is EditorResult.Error -> {
                        _state.update {
                            it.copy(
                                step = EditorStep.UPLOAD,
                                progress = 0f,
                                error = result.error,
                            )
                        }
                        return@launch
                    }
                }
                _state.update {
                    it.copy(progress = (index + 1).toFloat() / uris.size)
                }
            }
            _state.update {
                it.copy(
                    mergeVideos = videos,
                    step = EditorStep.MERGE,
                    progress = 1f,
                    mergeThumbnails = emptyMap(),
                    isPreparingClips = true,
                )
            }
            try {
                loadMergeThumbnails(videos, _state.value.aspect)
            } finally {
                _state.update { it.copy(isPreparingClips = false) }
            }
        }
    }

    fun moveMergeVideo(index: Int, delta: Int) {
        val videos = _state.value.mergeVideos.toMutableList()
        val destination = index + delta
        if (index !in videos.indices || destination !in videos.indices) return
        val video = videos.removeAt(index)
        videos.add(destination, video)
        _state.update { it.copy(mergeVideos = videos) }
    }

    fun previewVideo(video: VideoFile) {
        openClip(
            ExportedClip(
                index = 0,
                path = video.uri,
                displayName = video.displayName,
                uri = video.uri,
            )
        )
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
                thumbnails = if (current.mode == EditorMode.AUTO_CLIPS) emptyMap() else it.thumbnails,
                manualThumbnail = if (current.mode == EditorMode.MANUAL_TRIM) null else it.manualThumbnail,
            )
        }
        when (current.mode) {
            EditorMode.AUTO_CLIPS -> {
                val video = current.video
                if (video != null && current.clips.isNotEmpty()) {
                    screenModelScope.launch {
                        loadThumbnails(video, current.clips.map { it.copy(aspect = aspect) }, aspect)
                    }
                }
            }
            EditorMode.MANUAL_TRIM -> {
                val video = current.video ?: return
                requestManualPreview(video, current.manualStartMs, current.manualEndMs, aspect)
            }
            EditorMode.MERGE_VIDEOS -> {
                val videos = current.mergeVideos
                if (videos.isNotEmpty()) {
                    screenModelScope.launch {
                        _state.update {
                            it.copy(
                                isPreparingClips = true,
                                mergeThumbnails = emptyMap(),
                            )
                        }
                        try {
                            loadMergeThumbnails(videos, aspect)
                        } finally {
                            if (_state.value.aspect == aspect) {
                                _state.update { it.copy(isPreparingClips = false) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateManualRange(startSeconds: Float, endSeconds: Float) {
        val current = _state.value
        val video = current.video ?: return
        val durationMs = video.durationMs
        val minimumDurationMs = min(MIN_MANUAL_DURATION_MS, durationMs)
        var startMs = (startSeconds * 1_000).toLong().coerceIn(0, durationMs)
        var endMs = (endSeconds * 1_000).toLong().coerceIn(0, durationMs)
        if (endMs - startMs < minimumDurationMs) {
            endMs = (startMs + minimumDurationMs).coerceAtMost(durationMs)
            startMs = (endMs - minimumDurationMs).coerceAtLeast(0)
        }
        _state.update {
            it.copy(
                manualStartMs = startMs,
                manualEndMs = endMs,
            )
        }
        requestManualPreview(video, startMs, endMs, current.aspect)
    }

    fun updateManualSubtitle(text: String) {
        _state.update { it.copy(manualSubtitle = text) }
    }

    fun updateNarration(text: String) {
        _state.update { it.copy(narrationText = text) }
    }

    fun updateMergeNarration(uri: String, text: String) {
        _state.update {
            it.copy(mergeNarrations = it.mergeNarrations + (uri to text))
        }
    }

    fun setClipAudio(index: Int, uri: String?) {
        if (uri == null) {
            _state.update { state ->
                state.copy(
                    clips = state.clips.map { clip ->
                        if (clip.index == index) clip.copy(audio = null) else clip
                    },
                )
            }
            return
        }
        screenModelScope.launch {
            val audio = loadAudio(uri) ?: return@launch
            _state.update { state ->
                state.copy(
                    clips = state.clips.map { clip ->
                        if (clip.index == index) clip.copy(audio = audio) else clip
                    },
                )
            }
        }
    }

    fun setManualAudio(uri: String?) {
        if (uri == null) {
            _state.update { it.copy(manualAudio = null) }
            return
        }
        screenModelScope.launch {
            val audio = loadAudio(uri) ?: return@launch
            _state.update { it.copy(manualAudio = audio) }
        }
    }

    fun setMergeAudio(videoUri: String, uri: String?) {
        if (uri == null) {
            _state.update {
                it.copy(mergeAudios = it.mergeAudios - videoUri)
            }
            return
        }
        screenModelScope.launch {
            val audio = loadAudio(uri) ?: return@launch
            _state.update {
                it.copy(mergeAudios = it.mergeAudios + (videoUri to audio))
            }
        }
    }

    fun setFullAudio(uri: String?) {
        if (uri == null) {
            _state.update { it.copy(fullAudio = null) }
            return
        }
        screenModelScope.launch {
            val audio = loadAudio(uri) ?: return@launch
            _state.update { it.copy(fullAudio = audio) }
        }
    }

    fun generateManualSubtitle() {
        val current = _state.value
        val video = current.video ?: return
        screenModelScope.launch {
            _state.update { it.copy(isPreparingClips = true, subtitlesNotice = false) }
            when (val result = params.transcribeClip(
                video,
                current.manualStartMs,
                current.manualEndMs,
            )) {
                is EditorResult.Success -> {
                    _state.update {
                        it.copy(
                            manualSubtitle = formatSubtitle(result.data),
                            isPreparingClips = false,
                        )
                    }
                }
                is EditorResult.Error -> {
                    _state.update {
                        it.copy(
                            isPreparingClips = false,
                            subtitlesNotice = true,
                        )
                    }
                }
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

    fun updateClipNarration(index: Int, narration: String) {
        _state.update { state ->
            state.copy(
                clips = state.clips.map { clip ->
                    if (clip.index == index) clip.copy(narration = narration) else clip
                },
            )
        }
    }

    fun backToSettings() {
        _state.update { it.copy(step = EditorStep.SETTINGS, error = null) }
    }

    fun export() {
        when (_state.value.mode) {
            EditorMode.AUTO_CLIPS -> exportAutoClips()
            EditorMode.MANUAL_TRIM -> exportManualClip()
            EditorMode.MERGE_VIDEOS -> exportMergedVideo()
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
        val mode = _state.value.mode
        manualPreviewJob?.cancel()
        _state.value = EditorScreenState(mode = mode)
    }

    private fun startManual(video: VideoFile) {
        val endMs = min(DEFAULT_MANUAL_DURATION_MS, video.durationMs)
        _state.update {
            it.copy(
                video = video,
                step = EditorStep.MANUAL,
                manualStartMs = 0,
                manualEndMs = endMs,
                manualSubtitle = "",
                narrationText = "",
                manualAudio = null,
                manualThumbnail = null,
                error = null,
            )
        }
        requestManualPreview(video, 0, endMs, _state.value.aspect)
    }

    private fun requestManualPreview(
        video: VideoFile,
        startMs: Long,
        endMs: Long,
        aspect: VideoAspect,
    ) {
        manualPreviewJob?.cancel()
        manualPreviewJob = screenModelScope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            _state.update { it.copy(isPreparingClips = true) }
            val midpoint = (startMs + endMs) / 2
            when (val result = params.loadClipThumbnail(video, midpoint, aspect)) {
                is EditorResult.Success -> {
                    if (_state.value.aspect == aspect) {
                        _state.update {
                            it.copy(
                                manualThumbnail = result.data,
                                isPreparingClips = false,
                            )
                        }
                    }
                }
                is EditorResult.Error -> {
                    _state.update { it.copy(isPreparingClips = false) }
                }
            }
        }
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

    private fun exportAutoClips() {
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

    private fun exportManualClip() {
        val current = _state.value
        val video = current.video ?: return
        val clip = ClipDraft(
            index = 1,
            startMs = current.manualStartMs,
            endMs = current.manualEndMs,
            score = 0f,
            subtitle = current.manualSubtitle,
            narration = current.narrationText,
            audio = current.manualAudio,
            aspect = current.aspect,
        )
        if (clip.durationMs <= 0) return

        screenModelScope.launch {
            _state.update {
                it.copy(
                    step = EditorStep.EXPORTING,
                    progress = 0f,
                    error = null,
                    exportedClips = emptyList(),
                )
            }
            when (val result = params.exportClip(video, clip, current.narrationText)) {
                is EditorResult.Success -> {
                    _state.update {
                        it.copy(
                            step = EditorStep.DONE,
                            progress = 1f,
                            exportedClips = listOf(result.data),
                        )
                    }
                }
                is EditorResult.Error -> {
                    _state.update {
                        it.copy(
                            step = EditorStep.MANUAL,
                            progress = 0f,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    private fun exportMergedVideo() {
        val current = _state.value
        if (current.mergeVideos.size < 2) {
            _state.update { it.copy(error = EditorError.MERGE_FAILED) }
            return
        }
        screenModelScope.launch {
            _state.update {
                it.copy(
                    step = EditorStep.EXPORTING,
                    progress = 0f,
                    error = null,
                    exportedClips = emptyList(),
                )
            }
            when (val result = params.mergeVideos(
                videos = current.mergeVideos,
                narrations = current.mergeVideos.map { video ->
                    current.mergeNarrations[video.uri].orEmpty()
                },
                sceneAudios = current.mergeVideos.map { video ->
                    current.mergeAudios[video.uri]
                },
                fullAudio = current.fullAudio,
                aspect = current.aspect,
            )) {
                is EditorResult.Success -> {
                    _state.update {
                        it.copy(
                            step = EditorStep.DONE,
                            progress = 1f,
                            exportedClips = listOf(result.data),
                        )
                    }
                }
                is EditorResult.Error -> {
                    _state.update {
                        it.copy(
                            step = EditorStep.MERGE,
                            progress = 0f,
                            error = result.error,
                        )
                    }
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

    private suspend fun loadMergeThumbnails(
        videos: List<VideoFile>,
        aspect: VideoAspect,
    ) {
        videos.forEach { video ->
            val midpoint = video.durationMs / 2
            when (val result = params.loadClipThumbnail(video, midpoint, aspect)) {
                is EditorResult.Success -> {
                    if (_state.value.aspect == aspect) {
                        _state.update {
                            it.copy(
                                mergeThumbnails = it.mergeThumbnails + (video.uri to result.data)
                            )
                        }
                    }
                }
                is EditorResult.Error -> Unit
            }
        }
    }

    private suspend fun loadAudio(uri: String): AudioFile? =
        when (val result = params.loadAudio(uri)) {
            is EditorResult.Success -> result.data
            is EditorResult.Error -> null
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

    private companion object {
        const val DEFAULT_MANUAL_DURATION_MS = 15_000L
        const val MIN_MANUAL_DURATION_MS = 1_000L
        const val PREVIEW_DEBOUNCE_MS = 250L
    }
}

enum class EditorMode {
    AUTO_CLIPS,
    MANUAL_TRIM,
    MERGE_VIDEOS,
}

enum class EditorStep {
    UPLOAD,
    LOADING_VIDEO,
    ANALYZING,
    SETTINGS,
    REVIEW,
    MANUAL,
    MERGE,
    EXPORTING,
    DONE,
}

data class EditorScreenState(
    val mode: EditorMode = EditorMode.AUTO_CLIPS,
    val step: EditorStep = EditorStep.UPLOAD,
    val video: VideoFile? = null,
    val mergeVideos: List<VideoFile> = emptyList(),
    val mergeNarrations: Map<String, String> = emptyMap(),
    val signals: List<VideoSignal> = emptyList(),
    val clips: List<ClipDraft> = emptyList(),
    val exportedClips: List<ExportedClip> = emptyList(),
    val thumbnails: Map<Int, ByteArray> = emptyMap(),
    val manualThumbnail: ByteArray? = null,
    val clipCount: Int = 3,
    val clipDurationSeconds: Int = 15,
    val aspect: VideoAspect = VideoAspect.PORTRAIT_9_16,
    val manualStartMs: Long = 0,
    val manualEndMs: Long = 0,
    val manualSubtitle: String = "",
    val narrationText: String = "",
    val manualAudio: AudioFile? = null,
    val mergeAudios: Map<String, AudioFile> = emptyMap(),
    val mergeThumbnails: Map<String, ByteArray> = emptyMap(),
    val fullAudio: AudioFile? = null,
    val progress: Float = 0f,
    val isPreparingClips: Boolean = false,
    val subtitlesNotice: Boolean = false,
    val error: EditorError? = null,
)
