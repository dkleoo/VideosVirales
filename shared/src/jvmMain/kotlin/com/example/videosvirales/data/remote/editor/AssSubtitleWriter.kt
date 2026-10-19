package com.example.videosvirales.data.remote.editor

import java.io.File
import java.util.Locale

object AssSubtitleWriter {
    fun write(
        file: File,
        text: String,
        durationMs: Long,
        width: Int,
        height: Int,
    ) {
        val dialogue = text
            .replace("\\", "\\\\")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("\r\n", "\n")
            .replace("\n", "\\N")

        val content = buildString {
            appendLine("[Script Info]")
            appendLine("ScriptType: v4.00+")
            appendLine("PlayResX: $width")
            appendLine("PlayResY: $height")
            appendLine("WrapStyle: 0")
            appendLine("ScaledBorderAndShadow: yes")
            appendLine()
            appendLine("[V4+ Styles]")
            appendLine(
                "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, " +
                    "OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, " +
                    "ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, " +
                    "Alignment, MarginL, MarginR, MarginV, Encoding"
            )
            appendLine(
                "Style: Default,Arial,64,&H00FFFFFF,&H000000FF,&H00000000,&H80000000," +
                    "1,0,0,0,100,100,0,0,1,4,2,2,80,80,160,1"
            )
            appendLine()
            appendLine("[Events]")
            appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
            appendLine(
                "Dialogue: 0,${assTime(0)},${assTime(durationMs)},Default,,0,0,0,,$dialogue"
            )
        }
        file.writeText(content, Charsets.UTF_8)
    }

    private fun assTime(durationMs: Long): String {
        val totalCentiseconds = (durationMs.coerceAtLeast(0) / 10)
        val centiseconds = totalCentiseconds % 100
        val totalSeconds = totalCentiseconds / 100
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return String.format(
            Locale.US,
            "%d:%02d:%02d.%02d",
            hours,
            minutes,
            seconds,
            centiseconds,
        )
    }
}
