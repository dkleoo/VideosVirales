package com.example.videosvirales.platform.picker

import androidx.compose.runtime.Composable

@Composable
actual fun rememberVideoUriPicker(onResult: (String?) -> Unit): () -> Unit = {
    onResult(null)
}
