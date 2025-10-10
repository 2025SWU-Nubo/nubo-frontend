package com.example.nubo.ui.component

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.drawscope.ContentDrawScope


// 클릭 시 시각적인 리플(ripple) 효과가 없는 clickable Modifier
@Composable
fun Modifier.noRippleClickable(
    enabled : Boolean =true,
    onClick: () -> Unit): Modifier = composed {
    clickable(
        enabled=enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
