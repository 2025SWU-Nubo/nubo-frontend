package com.example.nubo.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object AppTextStyles {

    // Helper function
    private fun style(size: TextUnit, weight: FontWeight) = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontSize = size,
        fontWeight = weight
    )

    // Title
    val title_bold_24 = style(24.sp, FontWeight.Bold)

    // Subtitle
    val subtitle_semibold_20 = style(20.sp, FontWeight.SemiBold)
    val subtitle_medium_16 = style(16.sp, FontWeight.Medium)

    // Body 18
    val body_bold_18 = style(18.sp, FontWeight.Bold)
    val body_semibold_18 = style(18.sp, FontWeight.SemiBold)
    val body_medium_18 = style(18.sp, FontWeight.Medium)
    val body_regular_18 = style(18.sp, FontWeight.Normal)

    // Body 16
    val body_bold_16 = style(16.sp, FontWeight.Bold)
    val body_semibold_16 = style(16.sp, FontWeight.SemiBold)
    val body_medium_16 = style(16.sp, FontWeight.Medium)
    val body_regular_16 = style(16.sp, FontWeight.Normal)

    // Body 14
    val body_bold_14 = style(14.sp, FontWeight.Bold)
    val body_semibold_14 = style(14.sp, FontWeight.SemiBold)
    val body_medium_14 = style(14.sp, FontWeight.Medium)
    val body_regular_14 = style(14.sp, FontWeight.Normal)

    // Button
    val button_semibold_14 = style(14.sp, FontWeight.SemiBold)
    val button_medium_12 = style(12.sp, FontWeight.Medium)

    // Caption
    val caption_regular_9 = style(9.sp, FontWeight.Normal)
}
