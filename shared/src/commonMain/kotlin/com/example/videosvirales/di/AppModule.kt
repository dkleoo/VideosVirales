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
import com.example.videosvirales.domain.useCase.editor.LoadVideoUseCase
import com.example.videosvirales.domain.useCase.home.GetHomeInfoUseCase
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
    factory { EditorViewModel(get(), get(), get(), get()) }
}
