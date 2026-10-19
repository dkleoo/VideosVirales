package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.videosvirales.presentation.screens.components.InfoCard
import com.example.videosvirales.presentation.screens.components.PrimaryButton
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.screens.editor.EditorScreenState
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.ColorApp
import com.example.videosvirales.presentation.theme.Radius
import com.example.videosvirales.presentation.theme.Sizes
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import com.example.videosvirales.presentation.util.formatDuration
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_change_video
import videosvirales.shared.generated.resources.editor_pick_video
import videosvirales.shared.generated.resources.editor_retry
import videosvirales.shared.generated.resources.editor_selected_video
import videosvirales.shared.generated.resources.editor_upload_subtitle
import videosvirales.shared.generated.resources.editor_upload_title
import videosvirales.shared.generated.resources.ic_play_circle

@Composable
fun UploadStep(
    state: EditorScreenState,
    onPick: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_upload_title),
            style = tiendasOnTextStyle(AppTextStyle.Headline),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_upload_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Xl))

        val video = state.video
        if (video == null) {
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Lg),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_play_circle),
                        contentDescription = null,
                        tint = ColorApp.Primary,
                        modifier = Modifier.size(Sizes.Logo),
                    )
                    Text(
                        text = stringResource(Res.string.editor_upload_subtitle),
                        style = tiendasOnTextStyle(
                            AppTextStyle.Caption,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        textAlign = TextAlign.Center,
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.editor_pick_video),
                        onClick = onPick,
                    )
                }
            }
        } else {
            InfoCard(
                title = stringResource(Res.string.editor_selected_video),
                value = "${video.displayName} · ${formatDuration(video.durationMs)}",
            )
            Spacer(Modifier.height(Spacing.Lg))
            if (state.error != null) {
                PrimaryButton(
                    text = stringResource(Res.string.editor_retry),
                    onClick = onRetry,
                )
                Spacer(Modifier.height(Spacing.Sm))
            }
            SecondaryButton(
                text = stringResource(Res.string.editor_change_video),
                onClick = onPick,
            )
        }
    }
}
