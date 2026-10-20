package com.example.videosvirales.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.videosvirales.di.koinScreenModel
import com.example.videosvirales.presentation.screens.components.InfoCard
import com.example.videosvirales.presentation.screens.components.PrimaryButton
import com.example.videosvirales.presentation.screens.components.SecondaryButton
import com.example.videosvirales.presentation.screens.routes.AUTO_CLIPS_SCREEN
import com.example.videosvirales.presentation.screens.routes.MANUAL_TRIM_SCREEN
import com.example.videosvirales.presentation.screens.routes.MERGE_VIDEOS_SCREEN
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.ColorApp
import com.example.videosvirales.presentation.theme.Sizes
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import videosvirales.shared.generated.resources.Res
import videosvirales.shared.generated.resources.home_create_clips
import videosvirales.shared.generated.resources.home_error
import videosvirales.shared.generated.resources.home_manual_trim
import videosvirales.shared.generated.resources.home_merge_videos
import videosvirales.shared.generated.resources.home_platform_label
import videosvirales.shared.generated.resources.home_retry
import videosvirales.shared.generated.resources.home_subtitle
import videosvirales.shared.generated.resources.home_title
import videosvirales.shared.generated.resources.ic_play_circle

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<HomeViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        HomeContent(
            state = state,
            onRetry = viewModel::load,
            onCreateClips = { navigator.push(AUTO_CLIPS_SCREEN) },
            onManualTrim = { navigator.push(MANUAL_TRIM_SCREEN) },
            onMergeVideos = { navigator.push(MERGE_VIDEOS_SCREEN) },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeScreenState,
    onRetry: () -> Unit,
    onCreateClips: () -> Unit,
    onManualTrim: () -> Unit,
    onMergeVideos: () -> Unit,
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_play_circle),
            contentDescription = null,
            tint = ColorApp.Primary,
            modifier = Modifier.size(Sizes.Logo),
        )
        Spacer(Modifier.height(Spacing.Xl))
        Text(
            text = stringResource(Res.string.home_title),
            style = tiendasOnTextStyle(AppTextStyle.Display),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.Sm))
        Text(
            text = stringResource(Res.string.home_subtitle),
            style = tiendasOnTextStyle(
                AppTextStyle.Body,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.Xxl))
        when {
            state.isLoading -> CircularProgressIndicator(color = ColorApp.Primary)
            state.hasError -> {
                Text(
                    text = stringResource(Res.string.home_error),
                    style = tiendasOnTextStyle(AppTextStyle.Body),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.Lg))
                PrimaryButton(
                    text = stringResource(Res.string.home_retry),
                    onClick = onRetry,
                )
            }
            else -> InfoCard(
                title = stringResource(Res.string.home_platform_label),
                value = state.platformName,
            )
        }
        if (!state.isLoading && !state.hasError) {
            Spacer(Modifier.height(Spacing.Xxl))
            PrimaryButton(
                text = stringResource(Res.string.home_create_clips),
                onClick = onCreateClips,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.Sm))
            SecondaryButton(
                text = stringResource(Res.string.home_manual_trim),
                onClick = onManualTrim,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.Sm))
            SecondaryButton(
                text = stringResource(Res.string.home_merge_videos),
                onClick = onMergeVideos,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
