package com.example.videosvirales.presentation.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.videosvirales.presentation.theme.AppTextStyle
import com.example.videosvirales.presentation.theme.Radius
import com.example.videosvirales.presentation.theme.Spacing
import com.example.videosvirales.presentation.theme.tiendasOnTextStyle

@Composable
fun InfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.Md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Xl),
        ) {
            Text(
                text = title,
                style = tiendasOnTextStyle(
                    AppTextStyle.Caption,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(Spacing.Sm))
            Text(
                text = value,
                style = tiendasOnTextStyle(AppTextStyle.Title),
            )
        }
    }
}
