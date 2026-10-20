package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.VideoAspect
import com.example.videosvirales.presentation.screens.editor.EditorStep
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.ColorApp
import com.example.videosvirales.presentation.theme.Radius
import com.example.videosvirales.presentation.theme.Sizes
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_aspect_1_1
import videosvirales.shared.generated.resources.editor_aspect_16_9
import videosvirales.shared.generated.resources.editor_aspect_9_16
import videosvirales.shared.generated.resources.editor_error_analysis
import videosvirales.shared.generated.resources.editor_error_export
import videosvirales.shared.generated.resources.editor_error_ffmpeg
import videosvirales.shared.generated.resources.editor_error_merge
import videosvirales.shared.generated.resources.editor_error_no_video
import videosvirales.shared.generated.resources.editor_error_open
import videosvirales.shared.generated.resources.editor_error_transcription
import videosvirales.shared.generated.resources.editor_error_tts
import videosvirales.shared.generated.resources.editor_error_unsupported
import videosvirales.shared.generated.resources.editor_step_progress
import videosvirales.shared.generated.resources.editor_title

@Composable
fun EditorHeader(step: EditorStep) {
    val stepNumber = when (step) {
        EditorStep.UPLOAD,
        EditorStep.LOADING_VIDEO,
        EditorStep.ANALYZING -> 1
        EditorStep.SETTINGS -> 2
        EditorStep.MANUAL -> 2
        EditorStep.MERGE -> 2
        EditorStep.REVIEW -> 3
        EditorStep.EXPORTING,
        EditorStep.DONE -> 4
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_title),
            style = tiendasOnTextStyle(AppTextStyle.Display),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_step_progress, stepNumber),
            style = tiendasOnTextStyle(
                AppTextStyle.Caption,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Sizes.StepBar)
                        .clip(RoundedCornerShape(Radius.Sm))
                        .background(
                            if (index < stepNumber) {
                                ColorApp.Primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun EditorErrorBanner(error: EditorError) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.Md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = editorErrorMessage(error),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.padding(Spacing.Lg),
        )
    }
}

@Composable
fun AspectSelector(
    selected: VideoAspect,
    onSelect: (VideoAspect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        VideoAspect.entries.forEach { aspect ->
            FilterChip(
                selected = aspect == selected,
                onClick = { onSelect(aspect) },
                label = {
                    Text(
                        text = aspectLabel(aspect),
                        style = tiendasOnTextStyle(AppTextStyle.Caption),
                    )
                },
            )
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = tiendasOnTextStyle(AppTextStyle.Title),
    )
}

@Composable
fun SectionValue(text: String) {
    Text(
        text = text,
        style = tiendasOnTextStyle(
            AppTextStyle.Body,
            MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun aspectLabel(aspect: VideoAspect): String = when (aspect) {
    VideoAspect.PORTRAIT_9_16 -> stringResource(Res.string.editor_aspect_9_16)
    VideoAspect.SQUARE_1_1 -> stringResource(Res.string.editor_aspect_1_1)
    VideoAspect.LANDSCAPE_16_9 -> stringResource(Res.string.editor_aspect_16_9)
}

@Composable
fun editorErrorMessage(error: EditorError): String = when (error) {
    EditorError.NO_VIDEO -> stringResource(Res.string.editor_error_no_video)
    EditorError.ANALYSIS_FAILED -> stringResource(Res.string.editor_error_analysis)
    EditorError.EXPORT_FAILED -> stringResource(Res.string.editor_error_export)
    EditorError.OPEN_FAILED -> stringResource(Res.string.editor_error_open)
    EditorError.TRANSCRIPTION_FAILED -> stringResource(Res.string.editor_error_transcription)
    EditorError.TTS_FAILED -> stringResource(Res.string.editor_error_tts)
    EditorError.MERGE_FAILED -> stringResource(Res.string.editor_error_merge)
    EditorError.FFMPEG_MISSING -> stringResource(Res.string.editor_error_ffmpeg)
    EditorError.UNSUPPORTED -> stringResource(Res.string.editor_error_unsupported)
}
