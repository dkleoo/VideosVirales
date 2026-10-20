package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.videosvirales.data.mappers.editor.AudioFile
import com.example.videosvirales.platform.picker.rememberAudioUriPicker
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_audio_change
import videosvirales.shared.generated.resources.editor_audio_none
import videosvirales.shared.generated.resources.editor_audio_pick
import videosvirales.shared.generated.resources.editor_audio_remove

@Composable
fun AudioPicker(
    title: String,
    audio: AudioFile?,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchPicker = rememberAudioUriPicker { uri -> onPick(uri) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = tiendasOnTextStyle(
                AppTextStyle.Caption,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Xs))
        Text(
            text = audio?.displayName ?: stringResource(Res.string.editor_audio_none),
            style = tiendasOnTextStyle(AppTextStyle.Body),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            SecondaryButton(
                text = stringResource(
                    if (audio == null) Res.string.editor_audio_pick else Res.string.editor_audio_change
                ),
                onClick = launchPicker,
                modifier = Modifier.weight(1f),
            )
            if (audio != null) {
                SecondaryButton(
                    text = stringResource(Res.string.editor_audio_remove),
                    onClick = { onPick(null) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
