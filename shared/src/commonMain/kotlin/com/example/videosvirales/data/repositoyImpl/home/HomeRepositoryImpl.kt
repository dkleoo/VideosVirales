package com.example.videosvirales.data.repositoyImpl.home

import com.example.videosvirales.data.mappers.home.HomeInfo
import com.example.videosvirales.data.remote.home.HomeServices
import com.example.videosvirales.domain.repository.home.HomeRepository

class HomeRepositoryImpl(
    private val homeServices: HomeServices,
) : HomeRepository {
    override suspend fun getHomeInfo(): HomeResult = runCatching {
        HomeInfo(platformName = homeServices.getPlatformName())
    }.fold(
        onSuccess = { HomeResult.Success(it) },
        onFailure = { HomeResult.Error },
    )
}
