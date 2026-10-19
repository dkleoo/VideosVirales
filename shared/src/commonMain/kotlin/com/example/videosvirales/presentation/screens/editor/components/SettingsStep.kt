package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.videosvirales.data.mappers.editor.ClipAnimation
import com.example.videosvirales.presentation.screens.components.InfoCard
import com.example.videosvirales.presentation.screens.components.PrimaryButton
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.screens.editor.EditorScreenState
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import com.example.videosvirales.presentation.util.formatDuration
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_animation
import videosvirales.shared.generated.resources.editor_change_video
import videosvirales.shared.generated.resources.editor_clip_count
import videosvirales.shared.generated.resources.editor_clip_duration
import videosvirales.shared.generated.resources.editor_generate_clips
import videosvirales.shared.generated.resources.editor_seconds
import videosvirales.shared.generated.resources.editor_selected_video
import videosvirales.shared.generated.resources.editor_settings_subtitle
import videosvirales.shared.generated.resources.editor_settings_title
import kotlin.math.roundToInt

@Composable
fun SettingsStep(
    state: EditorScreenState,
    onClipCountChange: (Int) -> Unit,
    onClipDurationChange: (Int) -> Unit,
    onAnimationChange: (ClipAnimation) -> Unit,
    onGenerate: () -> Unit,
    onPick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_settings_title),
            style = tiendasOnTextStyle(AppTextStyle.Headline),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_settings_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Xl))

        state.video?.let { video ->
            InfoCard(
                title = stringResource(Res.string.editor_selected_video),
                value = "${video.displayName} · ${formatDuration(video.durationMs)}",
            )
            Spacer(Modifier.height(Spacing.Xl))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(stringResource(Res.string.editor_clip_count))
            SectionValue(state.clipCount.toString())
        }
        Slider(
            value = state.clipCount.toFloat(),
            onValueChange = { onClipCountChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.Lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(stringResource(Res.string.editor_clip_duration))
            SectionValue(
                stringResource(Res.string.editor_seconds, state.clipDurationSeconds)
            )
        }
        Slider(
            value = state.clipDurationSeconds.toFloat(),
            onValueChange = { onClipDurationChange(it.roundToInt()) },
            valueRange = 5f..60f,
            steps = 10,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.Lg))
        SectionTitle(stringResource(Res.string.editor_animation))
        Spacer(Modifier.height(Spacing.Sm))
        AnimationSelector(
            selected = state.animation,
            onSelect = onAnimationChange,
        )

        Spacer(Modifier.height(Spacing.Xl))
        PrimaryButton(
            text = stringResource(Res.string.editor_generate_clips),
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.Sm))
        SecondaryButton(
            text = stringResource(Res.string.editor_change_video),
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
