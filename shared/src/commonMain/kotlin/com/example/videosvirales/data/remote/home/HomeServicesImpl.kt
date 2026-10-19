package com.example.videosvirales.data.remote.home

import com.example.videosvirales.platform.getPlatform

class HomeServicesImpl : HomeServices {
    override fun getPlatformName(): String = getPlatform().name
}
