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

@androidx.annotation.OptIn(UnstableApi::class)
class SubtitleOverlay(
    text: String,
) : BitmapOverlay() {
    private val bitmap = render(text)

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings =
        StaticOverlaySettings.Builder()
            .setBackgroundFrameAnchor(0f, BASE_ANCHOR_Y)
            .setOverlayFrameAnchor(0f, -0.5f)
            .build()

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
        const val BASE_ANCHOR_Y = -0.62f
        const val BITMAP_WIDTH = 900
        const val CONTENT_WIDTH = 828
        const val PADDING = 36
        const val TEXT_SIZE = 62f
        const val CORNER_RADIUS = 28f
    }
}
