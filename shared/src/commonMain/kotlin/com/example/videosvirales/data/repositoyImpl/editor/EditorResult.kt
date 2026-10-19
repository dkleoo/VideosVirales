package com.example.videosvirales.data.repositoyImpl.editor

import com.example.videosvirales.data.mappers.editor.EditorError

sealed class EditorResult<out T> {
    data class Success<T>(val data: T) : EditorResult<T>()
    data class Error(val error: EditorError) : EditorResult<Nothing>()
}
