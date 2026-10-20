package com.example.videosvirales.di

import com.example.videosvirales.data.remote.home.HomeServices
import com.example.videosvirales.data.remote.home.HomeServicesImpl
import com.example.videosvirales.data.remote.network.HttpClientFactory
import com.example.videosvirales.data.repositoyImpl.home.HomeRepositoryImpl
import com.example.videosvirales.data.repositoyImpl.editor.EditorRepositoryImpl
import com.example.videosvirales.domain.repository.editor.EditorRepository
import com.example.videosvirales.domain.repository.home.HomeRepository
import com.example.videosvirales.domain.useCase.editor.AnalyzeVideoUseCase
import com.example.videosvirales.domain.useCase.editor.BuildClipsUseCase
import com.example.videosvirales.domain.useCase.editor.ExportClipsUseCase
import com.example.videosvirales.domain.useCase.editor.ExportClipUseCase
import com.example.videosvirales.domain.useCase.editor.LoadClipThumbnailUseCase
import com.example.videosvirales.domain.useCase.editor.LoadAudioUseCase
import com.example.videosvirales.domain.useCase.editor.LoadVideoUseCase
import com.example.videosvirales.domain.useCase.editor.MergeVideosUseCase
import com.example.videosvirales.domain.useCase.editor.OpenClipUseCase
import com.example.videosvirales.domain.useCase.editor.TranscribeClipUseCase
import com.example.videosvirales.domain.useCase.home.GetHomeInfoUseCase
import com.example.videosvirales.data.params.EditorViewModelParams
import com.example.videosvirales.presentation.screens.editor.EditorViewModel
import com.example.videosvirales.presentation.screens.home.HomeViewModel
import org.koin.dsl.module

val appModule = module {
    single { HttpClientFactory.create() }

    single<HomeServices> { HomeServicesImpl() }
    single<HomeRepository> { HomeRepositoryImpl(get()) }
    single<EditorRepository> { EditorRepositoryImpl(get()) }

    factory { GetHomeInfoUseCase(get()) }
    factory { HomeViewModel(get()) }

    factory { LoadVideoUseCase(get()) }
    factory { AnalyzeVideoUseCase(get()) }
    factory { BuildClipsUseCase() }
    factory { ExportClipsUseCase(get()) }
    factory { ExportClipUseCase(get()) }
    factory { MergeVideosUseCase(get()) }
    factory { OpenClipUseCase(get()) }
    factory { LoadClipThumbnailUseCase(get()) }
    factory { LoadAudioUseCase(get()) }
    factory { TranscribeClipUseCase(get()) }
    factory {
        EditorViewModel(
            EditorViewModelParams(
                loadVideo = get(),
                analyzeVideo = get(),
                buildClips = get(),
                exportClips = get(),
                exportClip = get(),
                mergeVideos = get(),
                openClip = get(),
                loadClipThumbnail = get(),
                loadAudio = get(),
                transcribeClip = get(),
            )
        )
    }
}
