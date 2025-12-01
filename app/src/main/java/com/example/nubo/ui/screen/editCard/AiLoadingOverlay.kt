package com.example.nubo.ui.screen.editCard

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/* 카드 본문만 덮는 흰색 스크림
   - padding = Scaffold의 innerPadding을 그대로 넣어 앱바 제외
   - consumeTouch = true면 입력 차단  false면 통과 */
@Composable
fun AiLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    consumeTouch: Boolean = true,
    @DrawableRes leftDotResId: Int? = null,             // 왼쪽 점 PNG
    @DrawableRes centerDotResId: Int? = null,           // 가운데 점 PNG
    @DrawableRes rightDotResId: Int? = null,            // 오른쪽 점 PNG
    message: @Composable (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
        .fillMaxSize(),
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top =230.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DotsPulse(
                    leftDotResId = leftDotResId,
                    centerDotResId = centerDotResId,
                    rightDotResId = rightDotResId
                )
                message?.invoke()
            }
        }
    }
}

/* 점 3개 펄스 */
@Composable
private fun DotsPulse(
    @DrawableRes leftDotResId: Int? = null,
    @DrawableRes centerDotResId: Int? = null,
    @DrawableRes rightDotResId: Int? = null
) {
    // 각 점에 대응하는 리소스 리스트
    val dotResList = listOf(leftDotResId, centerDotResId, rightDotResId)

    val infinite = rememberInfiniteTransition(label = "dots")
    val delays = listOf(0, 90, 180)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val phase = delays[i]
            // 크기 변화
            val scale by infinite.animateFloat(
                initialValue = 0.8f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = phase, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "scale$i"
            )
            // 색 변화
            val alpha by infinite.animateFloat(
                initialValue = 0.8f, targetValue = 1f,
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
            val dotResId = dotResList.getOrNull(i)
            if (dotResId != null) {
                // PNG 이미지를 애니메이션 점으로 사용
                Image(
                    painter = painterResource(id = dotResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(12.dp) // 필요하면 조절
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            translationY = ty
                        }
                )
            } else {
                // 리소스가 없으면 기존 원형 점으로 폴백
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
}
