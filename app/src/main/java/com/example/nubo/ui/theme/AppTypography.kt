package com.example.nubo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    )
)
