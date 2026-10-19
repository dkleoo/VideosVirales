package com.example.videosvirales.presentation.screens.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.ColorApp
import com.example.videosvirales.presentation.theme.Radius
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(Radius.Md),
        colors = ButtonDefaults.buttonColors(
            containerColor = ColorApp.Primary,
            contentColor = ColorApp.OnPrimary,
        ),
    ) {
        Text(
            text = text,
            style = tiendasOnTextStyle(AppTextStyle.Label, ColorApp.OnPrimary),
        )
    }
}
