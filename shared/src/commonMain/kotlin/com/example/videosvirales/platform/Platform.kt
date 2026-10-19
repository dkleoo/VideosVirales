package com.example.videosvirales.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
