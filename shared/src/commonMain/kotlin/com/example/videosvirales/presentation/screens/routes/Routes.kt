package com.example.videosvirales.presentation.screens.routes

import cafe.adriel.voyager.core.screen.Screen
import com.example.videosvirales.presentation.screens.editor.EditorMode
import com.example.videosvirales.presentation.screens.editor.EditorScreen
import com.example.videosvirales.presentation.screens.home.HomeScreen

val HOME_SCREEN: Screen = HomeScreen()
val AUTO_CLIPS_SCREEN: Screen = EditorScreen(EditorMode.AUTO_CLIPS)
val MANUAL_TRIM_SCREEN: Screen = EditorScreen(EditorMode.MANUAL_TRIM)
val MERGE_VIDEOS_SCREEN: Screen = EditorScreen(EditorMode.MERGE_VIDEOS)
