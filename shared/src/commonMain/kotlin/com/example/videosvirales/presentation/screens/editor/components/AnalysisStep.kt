package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.videosvirales.presentation.screens.editor.EditorScreenState
import com.example.videosvirales.presentation.screens.editor.EditorStep
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.ColorApp
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_analyzing_progress
import videosvirales.shared.generated.resources.editor_analyzing_subtitle
import videosvirales.shared.generated.resources.editor_analyzing_title
import videosvirales.shared.generated.resources.editor_loading_video
import kotlin.math.roundToInt

@Composable
fun AnalysisStep(state: EditorScreenState) {
    val isLoadingVideo = state.step == EditorStep.LOADING_VIDEO
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Lg),
    ) {
        CircularProgressIndicator(color = ColorApp.Primary)
        Text(
            text = if (isLoadingVideo) {
                stringResource(Res.string.editor_loading_video)
            } else {
                stringResource(Res.string.editor_analyzing_title)
            },
            style = tiendasOnTextStyle(AppTextStyle.Headline),
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.video?.displayName
                ?: stringResource(Res.string.editor_analyzing_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
        )
        if (!isLoadingVideo) {
            Spacer(Modifier.height(Spacing.Sm))
            LinearProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = ColorApp.Primary,
            )
            Text(
                text = stringResource(
                    Res.string.editor_analyzing_progress,
                    (state.progress.coerceIn(0f, 1f) * 100).roundToInt(),
                ),
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
