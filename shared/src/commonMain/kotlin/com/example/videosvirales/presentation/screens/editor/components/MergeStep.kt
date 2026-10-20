package com.example.videosvirales.presentation.screens.editor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.presentation.screens.components.PrimaryButton
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.screens.editor.EditorScreenState
import com.example.videosvirales.presentation.screens.editor.EditorViewModel
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.ColorApp
import com.example.videosvirales.presentation.theme.Radius
import com.example.videosvirales.presentation.theme.Sizes
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import com.example.videosvirales.presentation.util.formatDuration
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.editor_aspect
import videosvirales.shared.generated.resources.editor_audio_full
import videosvirales.shared.generated.resources.editor_audio_replaces_narrator
import videosvirales.shared.generated.resources.editor_audio_scene
import videosvirales.shared.generated.resources.editor_merge_change
import videosvirales.shared.generated.resources.editor_merge_empty
import videosvirales.shared.generated.resources.editor_merge_export
import videosvirales.shared.generated.resources.editor_merge_order
import videosvirales.shared.generated.resources.editor_merge_subtitle
import videosvirales.shared.generated.resources.editor_merge_title
import videosvirales.shared.generated.resources.editor_move_down
import videosvirales.shared.generated.resources.editor_move_up
import videosvirales.shared.generated.resources.editor_narration_placeholder
import videosvirales.shared.generated.resources.editor_narration_scene
import videosvirales.shared.generated.resources.editor_narration_section
import videosvirales.shared.generated.resources.editor_narration_section_help
import videosvirales.shared.generated.resources.editor_preview_play
import videosvirales.shared.generated.resources.editor_preview_unavailable
import videosvirales.shared.generated.resources.ic_arrow_down
import videosvirales.shared.generated.resources.ic_arrow_up
import videosvirales.shared.generated.resources.ic_play_circle

@Composable
fun MergeStep(
    state: EditorScreenState,
    viewModel: EditorViewModel,
    onPick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.editor_merge_title),
            style = tiendasOnTextStyle(AppTextStyle.Headline),
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.editor_merge_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(Spacing.Xl))

        if (state.mergeVideos.isEmpty()) {
            Text(
                text = stringResource(Res.string.editor_merge_empty),
                style = tiendasOnTextStyle(
                    AppTextStyle.Body,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        } else {
            SectionTitle(stringResource(Res.string.editor_merge_order))
            Spacer(Modifier.height(Spacing.Md))
            SectionTitle(stringResource(Res.string.editor_narration_section))
            Spacer(Modifier.height(Spacing.Xs))
            Text(
                text = stringResource(Res.string.editor_narration_section_help),
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(Spacing.Md))

            state.mergeVideos.forEachIndexed { index, video ->
                MergeVideoCard(
                    index = index,
                    total = state.mergeVideos.size,
                    video = video,
                    state = state,
                    viewModel = viewModel,
                )
                Spacer(Modifier.height(Spacing.Lg))
            }

            AudioPicker(
                title = stringResource(Res.string.editor_audio_full),
                audio = state.fullAudio,
                onPick = viewModel::setFullAudio,
            )
        }

        if (state.isPreparingClips) {
            Spacer(Modifier.height(Spacing.Md))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(Spacing.Lg))
        SectionTitle(stringResource(Res.string.editor_aspect))
        Spacer(Modifier.height(Spacing.Sm))
        AspectSelector(
            selected = state.aspect,
            onSelect = viewModel::updateAspect,
        )

        Spacer(Modifier.height(Spacing.Xl))
        PrimaryButton(
            text = stringResource(Res.string.editor_merge_export),
            onClick = viewModel::export,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.mergeVideos.size >= 2 && !state.isPreparingClips,
        )
        Spacer(Modifier.height(Spacing.Sm))
        SecondaryButton(
            text = stringResource(Res.string.editor_merge_change),
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isPreparingClips,
        )
    }
}

@Composable
private fun MergeVideoCard(
    index: Int,
    total: Int,
    video: VideoFile,
    state: EditorScreenState,
    viewModel: EditorViewModel,
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
            val previewModifier = Modifier
                .height(Sizes.PreviewHeight)
                .aspectRatio(state.aspect.width.toFloat() / state.aspect.height)
                .clip(RoundedCornerShape(Radius.Sm))
            val thumbnail = state.mergeThumbnails[video.uri]
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = previewModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbnail != null) {
                        val image = remember(thumbnail) { thumbnail.decodeToImageBitmap() }
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.editor_preview_unavailable),
                            style = tiendasOnTextStyle(
                                AppTextStyle.Caption,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.previewVideo(video) },
                        modifier = Modifier
                            .size(Sizes.PlayButton)
                            .background(ColorApp.PlayOverlay, CircleShape),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_play_circle),
                            contentDescription = stringResource(Res.string.editor_preview_play),
                            tint = ColorApp.OnPrimary,
                            modifier = Modifier.size(Sizes.Icon),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.Lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${index + 1}. ${video.displayName}",
                        style = tiendasOnTextStyle(AppTextStyle.Title),
                    )
                    Spacer(Modifier.height(Spacing.Xs))
                    Text(
                        text = formatDuration(video.durationMs),
                        style = tiendasOnTextStyle(
                            AppTextStyle.Caption,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
                IconButton(
                    onClick = { viewModel.moveMergeVideo(index, -1) },
                    enabled = index > 0,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_up),
                        contentDescription = stringResource(Res.string.editor_move_up),
                        tint = if (index > 0) ColorApp.Primary else MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(
                    onClick = { viewModel.moveMergeVideo(index, 1) },
                    enabled = index < total - 1,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_down),
                        contentDescription = stringResource(Res.string.editor_move_down),
                        tint = if (index < total - 1) {
                            ColorApp.Primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.Lg))
            OutlinedTextField(
                value = state.mergeNarrations[video.uri].orEmpty(),
                onValueChange = { text -> viewModel.updateMergeNarration(video.uri, text) },
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
            AudioPicker(
                title = stringResource(Res.string.editor_audio_scene),
                audio = state.mergeAudios[video.uri],
                onPick = { uri -> viewModel.setMergeAudio(video.uri, uri) },
            )
            if (state.mergeAudios[video.uri] != null) {
                Spacer(Modifier.height(Spacing.Xs))
                Text(
                    text = stringResource(Res.string.editor_audio_replaces_narrator),
                    style = tiendasOnTextStyle(
                        AppTextStyle.Caption,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
