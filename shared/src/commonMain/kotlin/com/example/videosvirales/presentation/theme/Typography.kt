package com.example.videosvirales.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppTextStyle {
    Display,
    Headline,
    Title,
    Body,
    Label,
    Caption,
}

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun tiendasOnTextStyle(
    style: AppTextStyle = AppTextStyle.Body,
    color: Color = Color.Unspecified,
): TextStyle {
    val typography = MaterialTheme.typography
    val baseStyle = when (style) {
        AppTextStyle.Display -> typography.displaySmall
        AppTextStyle.Headline -> typography.headlineMedium
        AppTextStyle.Title -> typography.titleMedium
        AppTextStyle.Body -> typography.bodyLarge
        AppTextStyle.Label -> typography.labelLarge
        AppTextStyle.Caption -> typography.bodySmall
    }
    return baseStyle.copy(
        color = color.takeOrElse { MaterialTheme.colorScheme.onBackground },
    )
}
