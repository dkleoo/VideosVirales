package com.example.videosvirales.presentation.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import com.example.videosvirales.di.koinScreenModel
import com.example.videosvirales.platform.picker.rememberVideoUriPicker
import com.example.videosvirales.platform.picker.rememberVideoUrisPicker
import com.example.videosvirales.presentation.screens.editor.components.AnalysisStep
import com.example.videosvirales.presentation.screens.editor.components.DoneStep
import com.example.videosvirales.presentation.screens.editor.components.EditorErrorBanner
import com.example.videosvirales.presentation.screens.editor.components.EditorHeader
import com.example.videosvirales.presentation.screens.editor.components.ExportStep
import com.example.videosvirales.presentation.screens.editor.components.ManualStep
import com.example.videosvirales.presentation.screens.editor.components.MergeStep
import com.example.videosvirales.presentation.screens.editor.components.MergeUploadStep
import com.example.videosvirales.presentation.screens.editor.components.ReviewStep
import com.example.videosvirales.presentation.screens.editor.components.SettingsStep
import com.example.videosvirales.presentation.screens.editor.components.UploadStep
import com.example.videosvirales.presentation.theme.Spacing

class EditorScreen(
    private val mode: EditorMode,
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<EditorViewModel>()
        val state by viewModel.state.collectAsState()
        val pickVideo = rememberVideoUriPicker { uri -> viewModel.onVideoPicked(uri) }
        val pickVideos = rememberVideoUrisPicker { uris -> viewModel.onVideosPicked(uris) }

        LaunchedEffect(mode, viewModel) {
            viewModel.setMode(mode)
        }

        EditorContent(
            state = state,
            viewModel = viewModel,
            onPickVideo = pickVideo,
            onPickVideos = pickVideos,
        )
    }
}

@Composable
private fun EditorContent(
    state: EditorScreenState,
    viewModel: EditorViewModel,
    onPickVideo: () -> Unit,
    onPickVideos: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = Spacing.ScreenHorizontal,
                vertical = Spacing.Xxl,
            ),
    ) {
        EditorHeader(state.step)
        Spacer(Modifier.height(Spacing.Xl))

        state.error?.let { error ->
            EditorErrorBanner(error)
            Spacer(Modifier.height(Spacing.Lg))
        }

        when (state.step) {
            EditorStep.UPLOAD -> if (state.mode == EditorMode.MERGE_VIDEOS) {
                MergeUploadStep(onPick = onPickVideos)
            } else {
                UploadStep(
                    state = state,
                    onPick = onPickVideo,
                    onRetry = viewModel::retryAnalysis,
                )
            }
            EditorStep.LOADING_VIDEO,
            EditorStep.ANALYZING -> AnalysisStep(state)
            EditorStep.SETTINGS -> SettingsStep(
                state = state,
                onClipCountChange = viewModel::updateClipCount,
                onClipDurationChange = viewModel::updateClipDuration,
                onAspectChange = viewModel::updateAspect,
                onGenerate = viewModel::generateClips,
                onPick = onPickVideo,
            )
            EditorStep.REVIEW -> ReviewStep(
                state = state,
                viewModel = viewModel,
            )
            EditorStep.MANUAL -> ManualStep(
                state = state,
                viewModel = viewModel,
                onPick = onPickVideo,
            )
            EditorStep.MERGE -> MergeStep(
                state = state,
                viewModel = viewModel,
                onPick = onPickVideos,
            )
            EditorStep.EXPORTING -> ExportStep(state)
            EditorStep.DONE -> DoneStep(
                state = state,
                onRestart = viewModel::restart,
                onOpenClip = viewModel::openClip,
            )
        }

        Spacer(Modifier.height(Spacing.Xxl))
    }
}
