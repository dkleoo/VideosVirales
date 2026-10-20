package com.example.videosvirales.platform.picker

import androidx.compose.runtime.Composable

@Composable
actual fun rememberVideoUrisPicker(onResult: (List<String>) -> Unit): () -> Unit = {
    onResult(emptyList())
}
