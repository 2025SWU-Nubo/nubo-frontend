package com.example.nubo.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object AppTextStyles {

    private fun style(size: TextUnit, weight: FontWeight) = TextStyle(
        fontFamily = AppFonts.Pretendard,
        fontSize = size,
        fontWeight = weight
    )

    //Learn Screen
    val learn_percentage_46 = style(46.sp, FontWeight.Bold)
    val learn_percentage_30 = style(30.sp, FontWeight.Bold)

    // HeadLine
    val headline_regular_32 = style(32.sp, FontWeight.Normal)
    val headline_regular_28 = style(28.sp, FontWeight.Normal)
    val headline_bold_28 = style(28.sp, FontWeight.Bold)
    val headline_bold_26 = style(26.sp, FontWeight.Bold)
    val headline_regular_26 = style(26.sp, FontWeight.Normal)

    // Title
    val title_bold_24 = style(24.sp, FontWeight.Bold)
    val title_semibold_24 = style(24.sp, FontWeight.SemiBold)
    val title_regular_24 = style(24.sp, FontWeight.Normal)

    val title_semibold_22 = style(22.sp, FontWeight.SemiBold)

    // Subtitle
    val subtitle_semibold_20 = style(20.sp, FontWeight.SemiBold)
    val subtitle_medium_16 = style(16.sp, FontWeight.Medium)

    // Body B1 (18)
    val b1_bold_18 = style(18.sp, FontWeight.Bold)
    val b1_semibold_18 = style(18.sp, FontWeight.SemiBold)
    val b1_medium_18 = style(18.sp, FontWeight.Medium)
    val b1_regular_18 = style(18.sp, FontWeight.Normal)

    // Body B2 (16)
    val b2_bold_16 = style(16.sp, FontWeight.Bold)
    val b2_bold_15 = style(16.sp, FontWeight.Bold)
    val b2_semibold_16 = style(16.sp, FontWeight.SemiBold)
    val b2_medium_16 = style(16.sp, FontWeight.Medium)
    val b2_regular_16 = style(16.sp, FontWeight.Normal)

    // Body B3 (14)
    val b3_bold_14 = style(14.sp, FontWeight.Bold)
    val b3_semibold_14 = style(14.sp, FontWeight.SemiBold)
    val b3_medium_14 = style(14.sp, FontWeight.Medium)
    val b3_regular_14 = style(14.sp, FontWeight.Normal)

    // Label
    val label_semibold_14 = style(14.sp, FontWeight.SemiBold)
    val label_medium_14 = style(14.sp, FontWeight.Medium)
    val label_medium_12 = style(12.sp, FontWeight.Medium)
    val label_SemiBold_12 = style(12.sp, FontWeight.SemiBold)

    // Caption
    val caption_regular_9 = style(10.sp, FontWeight.Normal)
}
