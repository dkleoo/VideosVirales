package com.example.videosvirales

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform