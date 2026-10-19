package com.example.videosvirales.domain.useCase.home

import com.example.videosvirales.data.repositoyImpl.home.HomeResult
import com.example.videosvirales.domain.repository.home.HomeRepository

class GetHomeInfoUseCase(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(): HomeResult = repository.getHomeInfo()
}
