package com.example.nubo.ui.screen.learn

import androidx.compose.animation.core.EaseInOutSine
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

/**
 * 2D PNG 이미지를 사용하여 대시보드 배경을 렌더링하는 Composable 입니다.
 * 기존 GlbBackgroundView를 대체합니다.
 *
 * @param modifier Modifier
 * @param todayVideoCount 오늘 학습 수. 물방울 개수를 결정하는 데 사용됩니다.
 */
@Composable
fun GraphicBackgroundView(
    modifier: Modifier = Modifier,
    todayVideoCount: Int
) {
    // 표시할 물방울 개수 (샘플 이미지를 기준으로 최대 5개로 제한)
    val dropCount = todayVideoCount.coerceAtMost(5)

    // --- ✅ [추가] 애니메이션 설정 ---

    // 1. 꽃(Flower) 애니메이션 (3D 로직: 6초 주기, 6도 각도)
    val flowerTransition = rememberInfiniteTransition(label = "flower-sway")
    val flowerRotation by flowerTransition.animateFloat(
        initialValue = -2f, // -6 degrees (좌)
        targetValue = 2f,  // +6 degrees (우)
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = EaseInOutSine), // 3초
            repeatMode = RepeatMode.Reverse // 3초마다 방향 반전 (총 6초 주기)
        ),
        label = "flowerRotation"
    )

    // 2. 구름(Cloud) 애니메이션 (3D 로직: 5초 주기)
    val cloudTransition = rememberInfiniteTransition(label = "cloud-float")
    val cloudOffsetY by cloudTransition.animateFloat(
        initialValue = -8f, // -8 픽셀 (위)
        targetValue = 8f,  // +8 픽셀 (아래)
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = EaseInOutSine), // 2.5초
            repeatMode = RepeatMode.Reverse // 2.5초마다 방향 반전 (총 5초 주기)
        ),
        label = "cloudOffsetY"
    )
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
        // 이 Box가 모든 오브젝트(구름, 꽃 등)를 감싸고 중앙에 배치합니다.
        Box(
            modifier = Modifier
                .align(Alignment.Center) // ✅ [변경] 그룹을 화면 중앙에 정렬
                .fillMaxWidth()
                // ✅ [중요] 이 padding 값으로 오브젝트 그룹 전체의 '높이'를 조절합니다.
                // 하단 카드와 겹치는 위치를 보면서 이 값을 (e.g., 100.dp) 조절하세요.
                .padding(bottom = 320.dp),
            contentAlignment = Alignment.Center // 자식들을 이 Box의 중앙에 겹침
        ) {
            // Box는 선언 순서대로 그립니다. (먼저 선언 = 맨 아래에 깔림)

            // Z-Order 1: 줄기 (맨 아래)
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
                    // Y축 오프셋 (그룹 중앙에서 +20dp 아래로, 줄기와 겹침)
                    .scale(1.7f)
                    .graphicsLayer {
                        rotationZ = flowerRotation
                    }
            )

            // Z-Order 3: 물방울
            Box(
                modifier = Modifier,
                    // ✅ [추가] 물방울 그룹의 Y축 오프셋 (그룹 중앙에서 -50dp 위로)
                contentAlignment = Alignment.TopCenter
            ) {
                // 물방울 개별 위치 로직 (이전과 동일)
                val pos1 = Pair((-48).dp, 0.dp)
                val pos2 = Pair((-24).dp, 8.dp)
                val pos3 = Pair(0.dp, 12.dp)
                val pos4 = Pair(24.dp, 8.dp)
                val pos5 = Pair(48.dp, 0.dp)

                val positionsToShow = when (dropCount) {
                    1 -> listOf(pos3)
                    2 -> listOf(pos2, pos4)
                    3 -> listOf(pos2, pos3, pos4)
                    4 -> listOf(pos1, pos2, pos4, pos5)
                    5 -> listOf(pos1, pos2, pos3, pos4, pos5)
                    else -> emptyList()
                }

                positionsToShow.forEach { (xOffset, yOffset) ->
                    Image(
                        painter = painterResource(id = R.drawable.learn_waterdrop),
                        contentDescription = "물방울",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = xOffset, y = yOffset)
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
