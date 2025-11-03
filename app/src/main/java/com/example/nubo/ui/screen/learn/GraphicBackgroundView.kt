package com.example.nubo.ui.screen.learn

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R // R 파일 경로는 프로젝트에 맞게 확인해주세요.
import kotlin.math.PI
import kotlin.math.sin

/**
 * 2D PNG 이미지를 사용하여 대시보드 배경을 렌더링하는 Composable
 * 기존 GlbBackgroundView를 대체
 *
 * @param modifier Modifier
 * @param todayVideoCount 오늘 학습 수. 물방울 개수를 결정하는 데 사용
 */
@Composable
fun GraphicBackgroundView(
    modifier: Modifier = Modifier,
    todayVideoCount: Int
) {
    // 표시할 물방울 개수 (최대 5개로 제한)
    val dropCount = todayVideoCount.coerceAtMost(5)

    // --- 애니메이션 설정 ---

    // 1. 꽃(Flower) 애니메이션
    val flowerTransition = rememberInfiniteTransition(label = "flower-sway")
    val flowerRotation by flowerTransition.animateFloat(
        initialValue = -2f, // -2 degrees (좌)
        targetValue = 2f,  // +2 degrees (우)
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = EaseInOutSine), // 4초
            repeatMode = RepeatMode.Reverse // 4초마다 방향 반전 (총 8초)
        ),
        label = "flowerRotation"
    )

    // 2. 구름(Cloud) 애니메이션
    val cloudTransition = rememberInfiniteTransition(label = "cloud-float")
    val cloudOffsetY by cloudTransition.animateFloat(
        initialValue = -9f, // -9 픽셀 (위)
        targetValue = 9f,  // +9 픽셀 (아래)
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = EaseInOutSine), // 2.5초
            repeatMode = RepeatMode.Reverse // 2.5초마다 방향 반전 (총 5초)
        ),
        label = "cloudOffsetY"
    )

    // 3. 물방울 애니메이션
    val floatCycleSeconds = 5.0
    val floatCycleMillis = (floatCycleSeconds * 1000).toInt()
    val bobbingAmount = 4.dp // 둥실거리는 폭

    val raindropTransition = rememberInfiniteTransition(label = "RaindropTime")
    val raindropTime by raindropTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(), // 1 사이클 (0 ~ 360도)
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = floatCycleMillis,
                easing = LinearEasing // 선형 진행
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RaindropTime"
    )

    // Y축 오프셋 계산 (sin 함수)
    val animatedRaindropY = sin(raindropTime) * bobbingAmount.value

    // --- 애니메이션 설정 끝 ---

    Box(
        modifier = modifier
    ) {
        // Layer 1: 배경 (맨 밑)
        Image(
            painter = painterResource(id = R.drawable.learn_background),
            contentDescription = "배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter
        )

        // Layer 2: 오브젝트 그룹
        // 이 Box가 모든 오브젝트(구름, 꽃 등)를 감싸고 중앙에 배치
        Box(
            modifier = Modifier
                .align(Alignment.Center) // 그룹을 화면 중앙에 정렬
                .fillMaxWidth()
                // padding 값으로 오브젝트 그룹 전체의 '높이'를 조절
                .padding(bottom = 320.dp),
            contentAlignment = Alignment.Center // 자식들을 이 Box의 중앙에 겹침
        ) {
            // Box는 선언 순서대로 배치
            // Z-Order 1: 줄기 (맨 아래 배경 바로 위)
            Image(
                painter = painterResource(id = R.drawable.learn_plant),
                contentDescription = "줄기",
                modifier = Modifier
                    //  Y축 오프셋으로 위치 조정 (그룹 중앙에서 +50dp 아래로)
                    .scale(1.7f)
            )

            // Z-Order 2: 꽃
            Image(
                painter = painterResource(id = R.drawable.learn_flower),
                contentDescription = "꽃",
                modifier = Modifier
                    .scale(1.7f)
                    .graphicsLayer {
                        rotationZ = flowerRotation
                    }
            )

            // Z-Order 3: 물방울
            Box(
                modifier = Modifier
                .scale(1.3f),
                contentAlignment = Alignment.TopCenter
            ) {
                // 물방울 개별 위치 로직 (이전과 동일)
                val pos1 = Pair((-70).dp, 30.dp)
                val pos2 = Pair((-35).dp, 46.dp)
                val pos3 = Pair(0.dp, 30.dp)
                val pos4 = Pair(35.dp, 46.dp)
                val pos5 = Pair(70.dp, 30.dp)

                val positionsToShow = when (dropCount) {
                    1 -> listOf(pos3)
                    2 -> listOf(pos3, pos4)
                    3 -> listOf(pos2, pos3, pos4)
                    4 -> listOf(pos3, pos2, pos4, pos5)
                    5 -> listOf(pos1, pos2, pos3, pos4, pos5)
                    else -> emptyList()
                }

                positionsToShow.forEach { (xOffset, yOffset) ->
                    val finalY = yOffset + animatedRaindropY.dp

                    Image(
                        painter = painterResource(id = R.drawable.learn_waterdrop),
                        contentDescription = "물방울",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = xOffset, y = finalY)
                    )
                }
            }

            // Z-Order 4: 구름 (맨 위)
            Image(
                painter = painterResource(id = R.drawable.learn_cloud),
                contentDescription = "구름",
                modifier = Modifier
                    .offset(y = 40.dp)
                    .scale(1.5f)
                    .graphicsLayer {
                        translationY = cloudOffsetY
                    }
            )
        }
    }
}
