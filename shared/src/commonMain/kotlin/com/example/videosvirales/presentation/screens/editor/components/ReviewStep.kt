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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.data.mappers.editor.VideoAspect
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
import com.example.videosvirales.presentation.util.formatScore
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_aspect
import videosvirales.shared.generated.resources.editor_audio_replaces_narrator
import videosvirales.shared.generated.resources.editor_audio_scene
import videosvirales.shared.generated.resources.editor_back
import videosvirales.shared.generated.resources.editor_clip_number
import videosvirales.shared.generated.resources.editor_export
import videosvirales.shared.generated.resources.editor_narration_placeholder
import videosvirales.shared.generated.resources.editor_narration_scene
import videosvirales.shared.generated.resources.editor_narration_section
import videosvirales.shared.generated.resources.editor_narration_section_help
import videosvirales.shared.generated.resources.editor_preparing_clips
import videosvirales.shared.generated.resources.editor_preview_unavailable
import videosvirales.shared.generated.resources.editor_review_subtitle
import videosvirales.shared.generated.resources.editor_review_title
import videosvirales.shared.generated.resources.editor_score
import videosvirales.shared.generated.resources.editor_subtitle_label
import videosvirales.shared.generated.resources.editor_subtitle_placeholder
import videosvirales.shared.generated.resources.editor_subtitles_unavailable

@Composable
fun ReviewStep(
    state: EditorScreenState,
    viewModel: EditorViewModel,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_review_title),
            style = tiendasOnTextStyle(AppTextStyle.Headline),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_review_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Lg))
        SectionTitle(stringResource(Res.string.editor_aspect))
        Spacer(Modifier.height(Spacing.Sm))
        AspectSelector(
            selected = state.aspect,
            onSelect = viewModel::updateAspect,
        )

        if (state.isPreparingClips) {
            Spacer(Modifier.height(Spacing.Lg))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.Sm))
            Text(
                text = stringResource(Res.string.editor_preparing_clips),
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
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
        SectionTitle(stringResource(Res.string.editor_narration_section))
        Spacer(Modifier.height(Spacing.Xs))
        Text(
            text = stringResource(Res.string.editor_narration_section_help),
            style = tiendasOnTextStyle(
                AppTextStyle.Caption,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Lg))
        val maxScore = state.clips.maxOfOrNull { it.score }?.takeIf { it > 0f } ?: 1f
        state.clips.forEach { clip ->
            ClipEditorCard(
                clip = clip,
                scorePercent = formatScore(clip.score / maxScore),
                thumbnail = state.thumbnails[clip.index],
                onSubtitleChange = { viewModel.updateSubtitle(clip.index, it) },
                onNarrationChange = { viewModel.updateClipNarration(clip.index, it) },
                onAudioChange = { uri -> viewModel.setClipAudio(clip.index, uri) },
            )
            Spacer(Modifier.height(Spacing.Lg))
        }

        PrimaryButton(
            text = stringResource(Res.string.editor_export),
            onClick = viewModel::export,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isPreparingClips,
        )
        Spacer(Modifier.height(Spacing.Sm))
        SecondaryButton(
            text = stringResource(Res.string.editor_back),
            onClick = viewModel::backToSettings,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ClipEditorCard(
    clip: ClipDraft,
    scorePercent: Int,
    thumbnail: ByteArray?,
    onSubtitleChange: (String) -> Unit,
    onNarrationChange: (String) -> Unit,
    onAudioChange: (String?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.Md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Xl),
        ) {
            val aspectModifier = Modifier
                .height(Sizes.PreviewHeight)
                .aspectRatio(clip.aspect.width.toFloat() / clip.aspect.height)
                .clip(RoundedCornerShape(Radius.Sm))

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
                        modifier = aspectModifier,
                    )
                } else {
                    Box(
                        modifier = aspectModifier.background(MaterialTheme.colorScheme.surfaceVariant),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.editor_clip_number, clip.index),
                    style = tiendasOnTextStyle(AppTextStyle.Title),
                )
                Text(
                    text = stringResource(Res.string.editor_score, scorePercent),
                    style = tiendasOnTextStyle(
                        AppTextStyle.Caption,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            Spacer(Modifier.height(Spacing.Xs))
            Text(
                text = "${formatDuration(clip.startMs)} - ${formatDuration(clip.endMs)}",
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(Spacing.Lg))
            AudioPicker(
                title = stringResource(Res.string.editor_audio_scene),
                audio = clip.audio,
                onPick = onAudioChange,
            )
            if (clip.audio != null) {
                Spacer(Modifier.height(Spacing.Xs))
                Text(
                    text = stringResource(Res.string.editor_audio_replaces_narrator),
                    style = tiendasOnTextStyle(
                        AppTextStyle.Caption,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            Spacer(Modifier.height(Spacing.Lg))
            OutlinedTextField(
                value = clip.narration,
                onValueChange = onNarrationChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = stringResource(Res.string.editor_narration_scene),
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
            Spacer(Modifier.height(Spacing.Md))
            OutlinedTextField(
                value = clip.subtitle,
                onValueChange = onSubtitleChange,
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
        }
    }
}
