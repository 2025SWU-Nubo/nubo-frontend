package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.example.nubo.R
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * - Nubo logo는 가운데 고정
 * - Instagram, Shorts logo가 중심을 원형으로 돌도록
 */
@Composable
fun OrbitingLogos(
    modifier: Modifier = Modifier
) {
    // 무한 회전(360도)
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 13000, // 전체 회전 지연 시간
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = modifier.fillMaxSize(),     // 전체 애니메이션 영역 크기
        contentAlignment = Alignment.Center
    ) {


        // 누보 로고(가운데)
        Image(
            painter = painterResource(id = R.drawable.intro_nubo_symbol),
            contentDescription = null,
            modifier = Modifier.size(250.dp),
            contentScale = ContentScale.Fit
        )

        // 인스타 로고 (0도 기준)
        OrbitIcon(
            imageResId = R.drawable.insta_logo,
            baseAngleDeg = angle,
            radius = 80.dp,
            iconSize = 66.dp
        )

        // 숏츠 로고 (반대편 180도 기준)
        OrbitIcon(
            imageResId = R.drawable.shorts_logo,
            baseAngleDeg = angle + 180f,
            radius = 80.dp,
            iconSize = 66.dp
        )
    }
}


/**
 *  sin/cos으로 각도와 지름을 계산한 아이콘 위치
 */
@Composable
private fun OrbitIcon(
    imageResId: Int,
    baseAngleDeg: Float,
    radius: Dp,
    iconSize: Dp
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }

    // Convert degree to radian for sin/cos
    val angleRad = Math.toRadians(baseAngleDeg.toDouble())
    val x = cos(angleRad) * radiusPx
    val y = sin(angleRad) * radiusPx

    Image(
        painter = painterResource(id = imageResId),
        contentDescription = null,
        modifier = Modifier
            .offset {
                IntOffset(
                    x.roundToInt(),
                    y.roundToInt()
                )
            }
            .size(iconSize),
        contentScale = ContentScale.Fit
    )
}
