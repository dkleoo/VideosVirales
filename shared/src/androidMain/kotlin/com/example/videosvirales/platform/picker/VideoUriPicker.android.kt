package com.example.videosvirales.platform.picker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberVideoUriPicker(onResult: (String?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onResult(uri?.toString())
    }
    return { launcher.launch("video/*") }
}
