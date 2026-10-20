package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.example.videosvirales.presentation.screens.components.InfoCard
import com.example.videosvirales.presentation.screens.components.PrimaryButton
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.screens.editor.EditorScreenState
import com.example.videosvirales.presentation.screens.editor.EditorViewModel
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.Radius
import com.example.videosvirales.presentation.theme.Sizes
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import com.example.videosvirales.presentation.util.formatDuration
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_aspect
import videosvirales.shared.generated.resources.editor_audio_clip
import videosvirales.shared.generated.resources.editor_audio_replaces_narrator
import videosvirales.shared.generated.resources.editor_change_video
import videosvirales.shared.generated.resources.editor_manual_end
import videosvirales.shared.generated.resources.editor_manual_export
import videosvirales.shared.generated.resources.editor_manual_generate_subtitle
import videosvirales.shared.generated.resources.editor_manual_start
import videosvirales.shared.generated.resources.editor_manual_subtitle
import videosvirales.shared.generated.resources.editor_manual_title
import videosvirales.shared.generated.resources.editor_manual_total
import videosvirales.shared.generated.resources.editor_narration
import videosvirales.shared.generated.resources.editor_narration_placeholder
import videosvirales.shared.generated.resources.editor_preview_unavailable
import videosvirales.shared.generated.resources.editor_selected_video
import videosvirales.shared.generated.resources.editor_subtitle_label
import videosvirales.shared.generated.resources.editor_subtitle_placeholder
import videosvirales.shared.generated.resources.editor_subtitles_unavailable

@Composable
fun ManualStep(
    state: EditorScreenState,
    viewModel: EditorViewModel,
    onPick: () -> Unit,
) {
    val video = state.video ?: return
    val durationSeconds = (video.durationMs / 1_000f).coerceAtLeast(1f)
    val startSeconds = (state.manualStartMs / 1_000f).coerceIn(0f, durationSeconds)
    val endSeconds = (state.manualEndMs / 1_000f).coerceIn(startSeconds, durationSeconds)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_manual_title),
            style = tiendasOnTextStyle(AppTextStyle.Headline),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_manual_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Xl))

        InfoCard(
            title = stringResource(Res.string.editor_selected_video),
            value = "${video.displayName} · ${formatDuration(video.durationMs)}",
        )
        Spacer(Modifier.height(Spacing.Xl))

        val previewModifier = Modifier
            .height(Sizes.PreviewHeight)
            .aspectRatio(state.aspect.width.toFloat() / state.aspect.height)
            .clip(RoundedCornerShape(Radius.Sm))
        val thumbnail = state.manualThumbnail
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                val image = remember(thumbnail) { thumbnail.decodeToImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = previewModifier,
                )
            } else {
                Box(
                    modifier = previewModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.editor_preview_unavailable),
                        style = tiendasOnTextStyle(
                            AppTextStyle.Caption,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.Lg))
        RangeSlider(
            value = startSeconds..endSeconds,
            onValueChange = { range ->
                viewModel.updateManualRange(range.start, range.endInclusive)
            },
            valueRange = 0f..durationSeconds,
            steps = 0,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionValue(
                "${stringResource(Res.string.editor_manual_start)} ${formatDuration(state.manualStartMs)}"
            )
            SectionValue(
                "${stringResource(Res.string.editor_manual_end)} ${formatDuration(state.manualEndMs)}"
            )
        }
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = "${stringResource(Res.string.editor_manual_total)}: " +
                formatDuration(state.manualEndMs - state.manualStartMs),
            style = tiendasOnTextStyle(
                AppTextStyle.Caption,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        if (state.isPreparingClips) {
            Spacer(Modifier.height(Spacing.Md))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.subtitlesNotice) {
            Spacer(Modifier.height(Spacing.Md))
            Text(
                text = stringResource(Res.string.editor_subtitles_unavailable),
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

        Spacer(Modifier.height(Spacing.Xl))
        SectionTitle(stringResource(Res.string.editor_aspect))
        Spacer(Modifier.height(Spacing.Sm))
        AspectSelector(
            selected = state.aspect,
            onSelect = viewModel::updateAspect,
        )

        Spacer(Modifier.height(Spacing.Xl))
        OutlinedTextField(
            value = state.manualSubtitle,
            onValueChange = viewModel::updateManualSubtitle,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = stringResource(Res.string.editor_subtitle_label),
                    style = tiendasOnTextStyle(AppTextStyle.Caption),
                )
            },
            placeholder = {
                Text(
                    text = stringResource(Res.string.editor_subtitle_placeholder),
                    style = tiendasOnTextStyle(
                        AppTextStyle.Caption,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            },
            textStyle = tiendasOnTextStyle(AppTextStyle.Body),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(Radius.Sm),
        )
        Spacer(Modifier.height(Spacing.Sm))
        SecondaryButton(
            text = stringResource(Res.string.editor_manual_generate_subtitle),
            onClick = viewModel::generateManualSubtitle,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isPreparingClips,
        )

        Spacer(Modifier.height(Spacing.Xl))
        OutlinedTextField(
            value = state.narrationText,
            onValueChange = viewModel::updateNarration,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = stringResource(Res.string.editor_narration),
                    style = tiendasOnTextStyle(AppTextStyle.Caption),
                )
            },
            placeholder = {
                Text(
                    text = stringResource(Res.string.editor_narration_placeholder),
                    style = tiendasOnTextStyle(
                        AppTextStyle.Caption,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            },
            textStyle = tiendasOnTextStyle(AppTextStyle.Body),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(Radius.Sm),
        )

        Spacer(Modifier.height(Spacing.Xl))
        AudioPicker(
            title = stringResource(Res.string.editor_audio_clip),
            audio = state.manualAudio,
            onPick = viewModel::setManualAudio,
        )
        if (state.manualAudio != null) {
            Spacer(Modifier.height(Spacing.Xs))
            Text(
                text = stringResource(Res.string.editor_audio_replaces_narrator),
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

        Spacer(Modifier.height(Spacing.Xl))
        PrimaryButton(
            text = stringResource(Res.string.editor_manual_export),
            onClick = viewModel::export,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isPreparingClips,
        )
        Spacer(Modifier.height(Spacing.Sm))
        SecondaryButton(
            text = stringResource(Res.string.editor_change_video),
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isPreparingClips,
        )
    }
}
