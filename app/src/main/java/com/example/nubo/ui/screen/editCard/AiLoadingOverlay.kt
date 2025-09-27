package com.example.nubo.ui.screen.editCard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/* 한글 주석: 카드 본문만 덮는 흰색 스크림
   - padding = Scaffold의 innerPadding을 그대로 넣어 앱바 제외
   - consumeTouch = true면 입력 차단  false면 통과 */
@Composable
fun AiLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    consumeTouch: Boolean = true,
    message: @Composable (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
        exit  = fadeOut() + slideOutVertically(targetOffsetY = { it / 6 })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .then(
                    if (consumeTouch)
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { /* 터치 소비만 */ }
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DotsPulse()            // 아래 정의
                message?.invoke()
            }
        }
    }
}

/* 한글 주석: 점 3개 펄스 */
@Composable
private fun DotsPulse() {
    val infinite = rememberInfiniteTransition(label = "dots")
    val delays = listOf(0, 90, 180)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val phase = delays[i]
            val scale by infinite.animateFloat(
                initialValue = 0.7f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = phase, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "scale$i"
            )
            val alpha by infinite.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = phase, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "alpha$i"
            )
            val ty by infinite.animateFloat(
                initialValue = 2f, targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = phase, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "ty$i"
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationY = ty
                    }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}
