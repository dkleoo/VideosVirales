package com.example.videosvirales.domain.repository.home

import com.example.videosvirales.data.repositoyImpl.home.HomeResult

interface HomeRepository {
    suspend fun getHomeInfo(): HomeResult
}
