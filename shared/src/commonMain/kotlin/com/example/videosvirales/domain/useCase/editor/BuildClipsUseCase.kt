package com.example.videosvirales.domain.useCase.editor

import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.EditorConfig
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.mappers.editor.VideoSignal
import kotlin.math.max

class BuildClipsUseCase {
    operator fun invoke(
        video: VideoFile,
        signals: List<VideoSignal>,
        config: EditorConfig,
    ): List<ClipDraft> {
        if (video.durationMs <= 0) return emptyList()

        val clipCount = config.clipCount.coerceIn(1, 10)
        val clipDurationMs = config.clipDurationMs
            .coerceAtLeast(1_000)
            .coerceAtMost(video.durationMs)
        val maxStartMs = (video.durationMs - clipDurationMs).coerceAtLeast(0)
        val stepMs = max(500L, clipDurationMs / 4)

        val candidates = buildList {
            var startMs = 0L
            while (startMs <= maxStartMs) {
                add(candidate(startMs, clipDurationMs, signals, video.durationMs))
                startMs += stepMs
            }
            if (lastOrNull()?.startMs != maxStartMs) {
                add(candidate(maxStartMs, clipDurationMs, signals, video.durationMs))
            }
        }

        val selected = mutableListOf<Candidate>()
        candidates
            .sortedByDescending { it.score }
            .forEach { candidate ->
                if (selected.size < clipCount && selected.none { it.overlaps(candidate) }) {
                    selected += candidate
                }
            }

        if (selected.size < clipCount) {
            val gapMs = video.durationMs / clipCount
            repeat(clipCount) { index ->
                if (selected.size < clipCount) {
                    val startMs = (index * gapMs).coerceAtMost(maxStartMs)
                    val candidate = candidate(startMs, clipDurationMs, signals, video.durationMs)
                    if (selected.none { it.overlaps(candidate) }) {
                        selected += candidate
                    }
                }
            }
        }

        if (selected.size < clipCount) {
            candidates
                .sortedByDescending { it.score }
                .forEach { candidate ->
                    if (selected.size < clipCount && selected.none { it.startMs == candidate.startMs }) {
                        selected += candidate
                    }
                }
        }

        return selected
            .sortedBy { it.startMs }
            .take(clipCount)
            .mapIndexed { index, candidate ->
                ClipDraft(
                    index = index + 1,
                    startMs = candidate.startMs,
                    endMs = candidate.endMs,
                    score = candidate.score,
                    subtitle = "",
                    narration = "",
                    aspect = config.aspect,
                )
            }
    }

    private fun candidate(
        startMs: Long,
        clipDurationMs: Long,
        signals: List<VideoSignal>,
        videoDurationMs: Long,
    ): Candidate {
        val endMs = (startMs + clipDurationMs).coerceAtMost(videoDurationMs)
        val windowSignals = signals.filter { it.timestampMs in startMs until endMs }
        val score = if (windowSignals.isEmpty()) {
            0f
        } else {
            (windowSignals.sumOf { it.score.toDouble() } / windowSignals.size).toFloat()
        }
        return Candidate(
            startMs = startMs,
            endMs = endMs,
            score = score.coerceIn(0f, 1f),
        )
    }
}

private data class Candidate(
    val startMs: Long,
    val endMs: Long,
    val score: Float,
) {
    fun overlaps(other: Candidate): Boolean =
        startMs < other.endMs && endMs > other.startMs
}
