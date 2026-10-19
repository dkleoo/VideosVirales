package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.videosvirales.data.mappers.editor.ExportedClip
import com.example.videosvirales.presentation.screens.components.InfoCard
import com.example.videosvirales.presentation.screens.components.PrimaryButton
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.screens.editor.EditorScreenState
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.ColorApp
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_done_clip
import videosvirales.shared.generated.resources.editor_done_subtitle
import videosvirales.shared.generated.resources.editor_done_title
import videosvirales.shared.generated.resources.editor_export_progress
import videosvirales.shared.generated.resources.editor_exporting_subtitle
import videosvirales.shared.generated.resources.editor_exporting_title
import videosvirales.shared.generated.resources.editor_open_clip
import videosvirales.shared.generated.resources.editor_restart
import kotlin.math.roundToInt

@Composable
fun ExportStep(state: EditorScreenState) {
    val total = state.clips.size
    val current = (state.progress.coerceIn(0f, 1f) * total).roundToInt().coerceIn(0, total)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_exporting_title),
            style = tiendasOnTextStyle(AppTextStyle.Headline),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_exporting_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Xxl))
        LinearProgressIndicator(
            progress = { state.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = ColorApp.Primary,
        )
        Spacer(Modifier.height(Spacing.Md))
        Text(
            text = stringResource(Res.string.editor_export_progress, current, total),
            style = tiendasOnTextStyle(
                AppTextStyle.Caption,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun DoneStep(
    state: EditorScreenState,
    onRestart: () -> Unit,
    onOpenClip: (ExportedClip) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_done_title),
            style = tiendasOnTextStyle(AppTextStyle.Headline),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_done_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Xl))

        state.exportedClips.forEach { clip ->
            InfoCard(
                title = stringResource(Res.string.editor_done_clip, clip.index),
                value = clip.path,
            )
            Spacer(Modifier.height(Spacing.Sm))
            SecondaryButton(
                text = stringResource(Res.string.editor_open_clip),
                onClick = { onOpenClip(clip) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.Lg))
        }

        state.error?.let { error ->
            EditorErrorBanner(error)
            Spacer(Modifier.height(Spacing.Lg))
        }

        PrimaryButton(
            text = stringResource(Res.string.editor_restart),
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
