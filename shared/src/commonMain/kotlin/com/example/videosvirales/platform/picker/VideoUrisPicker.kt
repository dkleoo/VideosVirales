package com.example.videosvirales.platform.picker

import androidx.compose.runtime.Composable

@Composable
expect fun rememberVideoUrisPicker(onResult: (List<String>) -> Unit): () -> Unit
