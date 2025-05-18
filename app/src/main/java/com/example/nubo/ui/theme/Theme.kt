package com.example.nubo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun NuboAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = AppTypography,
        content = content
    )
}

// LightColor
private val LightColorScheme = lightColorScheme(
    primary = PurpleMain500,
    onPrimary = OnDarkText,
    secondary = GreyMain300,
    background = WhiteBg,
    onBackground = DefaultText,
    surface = WhiteBg,
    onSurface = DefaultText,
    error = PinkError
)
