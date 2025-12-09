package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple300
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun KeywordChip(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = GreyMain300,
    backgroundColor: Color = Grey20,
){
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ){
        Text(
            text = text,
            style = AppTextStyles.b3_regular_14,
            color = textColor
        )
    }
}
