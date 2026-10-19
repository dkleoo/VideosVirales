package com.example.videosvirales.di

import com.example.videosvirales.data.remote.editor.EditorServices
import com.example.videosvirales.data.remote.editor.UnsupportedEditorServices
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformEditorModule(): Module = module {
    single<EditorServices> { UnsupportedEditorServices() }
}
