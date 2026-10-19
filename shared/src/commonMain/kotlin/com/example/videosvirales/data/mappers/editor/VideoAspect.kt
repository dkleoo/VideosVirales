package com.example.videosvirales.data.mappers.editor

enum class VideoAspect(
    val width: Int,
    val height: Int,
) {
    PORTRAIT_9_16(width = 1080, height = 1920),
    SQUARE_1_1(width = 1080, height = 1080),
    LANDSCAPE_16_9(width = 1920, height = 1080),
    ;

    val thumbnailWidth: Int
        get() = when (this) {
            PORTRAIT_9_16, SQUARE_1_1 -> 360
            LANDSCAPE_16_9 -> 640
        }

    val thumbnailHeight: Int
        get() = when (this) {
            PORTRAIT_9_16 -> 640
            SQUARE_1_1 -> 360
            LANDSCAPE_16_9 -> 360
        }
}
