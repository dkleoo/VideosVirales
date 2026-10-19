package com.example.videosvirales.data.repositoyImpl.home

import com.example.videosvirales.data.mappers.home.HomeInfo

sealed class HomeResult {
    data class Success(val info: HomeInfo) : HomeResult()
    data object Error : HomeResult()
}
