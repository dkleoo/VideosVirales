package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.videosvirales.data.mappers.editor.ClipAnimation
import com.example.videosvirales.data.mappers.editor.ClipDraft
import com.example.videosvirales.presentation.screens.components.PrimaryButton
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.screens.editor.EditorScreenState
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.Radius
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import com.example.videosvirales.presentation.util.formatDuration
import com.example.videosvirales.presentation.util.formatScore
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_animation
import videosvirales.shared.generated.resources.editor_back
import videosvirales.shared.generated.resources.editor_clip_number
import videosvirales.shared.generated.resources.editor_export
import videosvirales.shared.generated.resources.editor_review_subtitle
import videosvirales.shared.generated.resources.editor_review_title
import videosvirales.shared.generated.resources.editor_score
import videosvirales.shared.generated.resources.editor_subtitle_label
import videosvirales.shared.generated.resources.editor_subtitle_placeholder

@Composable
fun ReviewStep(
    state: EditorScreenState,
    onSubtitleChange: (Int, String) -> Unit,
    onAnimationChange: (Int, ClipAnimation) -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
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
        Spacer(Modifier.height(Spacing.Xl))

        val maxScore = state.clips.maxOfOrNull { it.score }?.takeIf { it > 0f } ?: 1f
        state.clips.forEach { clip ->
            ClipEditorCard(
                clip = clip,
                scorePercent = formatScore(clip.score / maxScore),
                onSubtitleChange = { onSubtitleChange(clip.index, it) },
                onAnimationChange = { onAnimationChange(clip.index, it) },
            )
            Spacer(Modifier.height(Spacing.Lg))
        }

        PrimaryButton(
            text = stringResource(Res.string.editor_export),
            onClick = onExport,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.Sm))
        SecondaryButton(
            text = stringResource(Res.string.editor_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ClipEditorCard(
    clip: ClipDraft,
    scorePercent: Int,
    onSubtitleChange: (String) -> Unit,
    onAnimationChange: (ClipAnimation) -> Unit,
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
            Spacer(Modifier.height(Spacing.Lg))
            Text(
                text = stringResource(Res.string.editor_animation),
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(Spacing.Sm))
            AnimationSelector(
                selected = clip.animation,
                onSelect = onAnimationChange,
            )
        }
    }
}
