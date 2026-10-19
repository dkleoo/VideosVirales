package com.example.videosvirales.data.remote.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.StaticOverlaySettings
import com.example.videosvirales.data.mappers.editor.ClipAnimation
import kotlin.math.min

@androidx.annotation.OptIn(UnstableApi::class)
class AnimatedSubtitleOverlay(
    text: String,
    private val durationUs: Long,
    private val animation: ClipAnimation,
) : BitmapOverlay() {
    private val bitmap = render(text)

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val safeDurationUs = durationUs.coerceAtLeast(1)
        val enterProgress = (presentationTimeUs / FADE_IN_US.toFloat()).coerceIn(0f, 1f)
        val exitProgress = ((safeDurationUs - presentationTimeUs) / FADE_OUT_US.toFloat())
            .coerceIn(0f, 1f)

        var alpha = min(enterProgress, exitProgress)
        var anchorY = BASE_ANCHOR_Y
        var scale = 1f

        when (animation) {
            ClipAnimation.NONE -> alpha = 1f
            ClipAnimation.FADE -> Unit
            ClipAnimation.SLIDE_UP -> {
                anchorY = BASE_ANCHOR_Y - 0.08f * (1f - enterProgress)
            }
            ClipAnimation.ZOOM_IN,
            ClipAnimation.ZOOM_OUT -> {
                scale = 0.85f + 0.15f * enterProgress
            }
        }

        return StaticOverlaySettings.Builder()
            .setAlphaScale(alpha)
            .setBackgroundFrameAnchor(0f, anchorY)
            .setOverlayFrameAnchor(0f, -0.5f)
            .setScale(scale, scale)
            .build()
    }

    private fun render(text: String): Bitmap {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = TEXT_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 0f, 3f, Color.argb(200, 0, 0, 0))
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, CONTENT_WIDTH)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(4)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(6f, 1f)
            .build()

        val bitmapHeight = layout.height + PADDING * 2
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(175, 0, 0, 0)
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, BITMAP_WIDTH.toFloat(), bitmapHeight.toFloat()),
            CORNER_RADIUS,
            CORNER_RADIUS,
            backgroundPaint,
        )
        canvas.save()
        canvas.translate(PADDING.toFloat(), PADDING.toFloat())
        layout.draw(canvas)
        canvas.restore()
        return bitmap
    }

    private companion object {
        const val FADE_IN_US = 300_000L
        const val FADE_OUT_US = 300_000L
        const val BASE_ANCHOR_Y = -0.62f
        const val BITMAP_WIDTH = 900
        const val CONTENT_WIDTH = 828
        const val PADDING = 36
        const val TEXT_SIZE = 62f
        const val CORNER_RADIUS = 28f
    }
}
