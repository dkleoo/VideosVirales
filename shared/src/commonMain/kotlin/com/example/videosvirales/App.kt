package com.example.videosvirales

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.example.videosvirales.di.appModule
import com.example.videosvirales.di.platformEditorModule
import com.example.videosvirales.presentation.screens.routes.HOME_SCREEN
import com.example.videosvirales.presentation.theme.VideosViralesTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule, platformEditorModule())
        },
    ) {
        VideosViralesTheme {
            Navigator(HOME_SCREEN)
        }
    }
}
