package com.example.videosvirales.di

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatform

@Composable
inline fun <reified T : ScreenModel> Screen.koinScreenModel(
    tag: String? = null,
    qualifier: Qualifier? = null,
): T = rememberScreenModel(tag) {
    KoinPlatform.getKoin().get<T>(qualifier = qualifier)
}
