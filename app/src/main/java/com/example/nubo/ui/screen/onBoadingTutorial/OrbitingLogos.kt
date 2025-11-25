package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Center logo with two orbiting logos around it
 * - Nubo logo is fixed in the center
 * - Instagram and Shorts logos rotate along a circular orbit
 */
@Composable
fun OrbitingLogos(
    modifier: Modifier = Modifier
) {
    // Infinite rotation angle 0f -> 360f
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 13000, // one full rotation duration
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
        // Center Nubo logo
        Image(
            painter = painterResource(id = R.drawable.ai_prompt_logo),
            contentDescription = null,
            modifier = Modifier.size(112.dp),
            contentScale = ContentScale.Fit
        )

        // Instagram logo (0도 기준)
        OrbitIcon(
            imageResId = R.drawable.insta_logo, // 실제 인스타 아이콘 리소스로 교체
            baseAngleDeg = angle,
            radius = 70.dp,
            iconSize = 56.dp
        )

        // Shorts logo (반대편 180도 기준)
        OrbitIcon(
            imageResId = R.drawable.shorts_logo, // 실제 숏츠 아이콘 리소스로 교체
            baseAngleDeg = angle + 180f,
            radius = 70.dp,
            iconSize = 56.dp
        )
    }
}

/**
 * One orbiting icon.
 * - Position is calculated from angle and radius using sin/cos
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
            // Offset from center of Box
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
