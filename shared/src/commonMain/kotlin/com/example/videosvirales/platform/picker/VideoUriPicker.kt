package com.example.videosvirales.platform.picker

import androidx.compose.runtime.Composable

@Composable
expect fun rememberVideoUriPicker(onResult: (String?) -> Unit): () -> Unit
