package com.example.videosvirales.presentation.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.videosvirales.data.repositoyImpl.home.HomeResult
import com.example.videosvirales.domain.useCase.home.GetHomeInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHomeInfo: GetHomeInfoUseCase,
) : ScreenModel {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            _state.value = HomeScreenState()
            _state.value = when (val result = getHomeInfo()) {
                is HomeResult.Success -> HomeScreenState(
                    isLoading = false,
                    platformName = result.info.platformName,
                )
                HomeResult.Error -> HomeScreenState(
                    isLoading = false,
                    hasError = true,
                )
            }
        }
    }
}

data class HomeScreenState(
    val isLoading: Boolean = true,
    val platformName: String = "",
    val hasError: Boolean = false,
)
