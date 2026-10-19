package com.example.videosvirales.data.repositoyImpl.editor

import com.example.videosvirales.data.mappers.editor.EditorError

class EditorException(
    val error: EditorError,
    cause: Throwable? = null,
) : Exception(cause)
