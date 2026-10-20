package com.example.videosvirales.platform.picker

import androidx.compose.runtime.Composable

@Composable
expect fun rememberAudioUriPicker(onResult: (String?) -> Unit): () -> Unit
