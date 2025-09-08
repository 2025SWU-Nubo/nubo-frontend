package com.example.nubo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary
val Purple900 = Color(0xFF3B38C2)
val Purple700 = Color(0xFF4945E0)
val PurpleMain500 = Color(0xFF5955FF)
val Purple300 = Color(0xFFB3B1FF)
val Purple200 = Color(0xFFD8D7FF)
val Purple100 = Color(0xFFE8E7FF)
val Purple50 = Color(0xFFF1F1FF)  //임의로 추가(선택된 버튼 배경)

// Secondary (Gray scale)
val Grey1000 = Color(0xFF000000)
val Grey900 = Color(0xFF1A1A23)
val Grey700 = Color(0xFF2F2F38)
val Grey500 = Color(0xFF595962)
val GreyMain300 = Color(0xFF84848D)
val Grey200 = Color(0xFFAFAFB8)
val GreyMain100 = Color(0xFFC5C5CE)
val Grey50 = Color(0xFFDADAE3)
val Grey30 = Color(0xFFE5E4EB)
val Grey20 = Color(0xFFEAE9EF)
val Grey10 = Color(0xFFF9F9FA)
val Grey5 = Color(0xFFFDFDFF)
val Grey0 = Color(0xFFFFFFFF)

// Error
val PinkError = Color(0xFFFFA6A6)
val RedError = Color(0xFFFF5858)

// Text
val DefaultText = Color(0xFF333339)
val OnDarkText = Color(0xFFFFFFFF)

// Background
val WhiteBg = Color(0xFFFFFFFF)

val LightColorScheme = lightColorScheme(
    primary = PurpleMain500,
    onPrimary = Color.White,
    secondary = GreyMain300,
    onSecondary = Color.White,
    background = WhiteBg,
    surface = Grey0,
    error = PinkError,
    onBackground = DefaultText,
    onSurface = DefaultText,
    onError = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = PurpleMain500,
    onPrimary = Color.Black,
    secondary = GreyMain300,
    onSecondary = Color.Black,
    background = Grey900,
    surface = Grey700,
    error = PinkError,
    onBackground = OnDarkText,
    onSurface = OnDarkText,
    onError = Color.Black
)

