package com.example.nubo.ui.screen.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * 2D PNG 이미지를 사용하여 대시보드 배경을 렌더링하는 Composable
 * 레벨(level) 값에 따라 다른 요소와 애니메이션을 표시
 *
 * @param modifier Modifier
 * @param todayVideoCount 오늘 학습 수. 물방울 개수를 결정하는 데 사용
 * @param level 현재 레벨. 표시할 그래픽과 애니메이션을 결정
 */
@Composable
fun GraphicBackgroundView(
    modifier: Modifier = Modifier,
    todayVideoCount: Int,
    level: Int,
) {
    // 표시할 물방울 개수 (최대 5개로 제한)
    val dropCount = todayVideoCount.coerceAtMost(5)

    // --- 공통 애니메이션 설정 ---

    // 1. 구름(Cloud) 애니메이션
    val cloudTransition = rememberInfiniteTransition(label = "cloud-float")
    val cloudOffsetY by cloudTransition.animateFloat(
        initialValue = -9f, // -9 픽셀 (위)
        targetValue = 8f,  // +9 픽셀 (아래)
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = EaseInOutSine), // 2.5초
            repeatMode = RepeatMode.Reverse // 2.5초마다 방향 반전 (총 5초)
        ),
        label = "cloudOffsetY"
    )

    // 2. 물방울 애니메이션
    val floatCycleSeconds = 5.0
    val floatCycleMillis = (floatCycleSeconds * 1000).toInt()
    val bobbingAmount = 20.dp // 둥실거리는 폭

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

    // Y축 오프셋 계산 (sin 함수) - 모든 물방울이 공유하는 둥실거림 값
    val animatedRaindropBobbingY = sin(raindropTime) * bobbingAmount.value

    //-----------------------------
    // 클릭 상태 관리
    //-----------------------------

    val bobbingY = sin(raindropTime) * 20f // 위아래 진폭

    // 구름 클릭 시 확대
    var cloudClicked by remember { mutableStateOf(false) }
    val cloudScale by animateFloatAsState(
        targetValue = if (cloudClicked) 1.2f else 1f,
        animationSpec = tween(300, easing = EaseInOutSine),
        label = "cloudScale"
    )
    // 물방울 낙하 상태 (구름 클릭 시 트리거)
    var fallingDrops by remember { mutableStateOf(false) }
    val fallAnim = remember { Animatable(0f) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(fallingDrops) {
        if (fallingDrops) {
            // 아래로 떨어짐
            fallAnim.animateTo(
                targetValue = 250f,
                animationSpec = tween(600, easing = EaseInOutSine)
            )
            // 다시 제자리로 복귀
            fallAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(700, easing = EaseInOutSine)
            )
            fallingDrops = false
        }
    }


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
                .fillMaxWidth(),
            // padding 값으로 오브젝트 그룹 전체의 '높이'를 조절
            contentAlignment = Alignment.Center // 자식들을 이 Box의 중앙에 겹침
        ) {
            when (level) {
                1 -> {// 레벨 1 새싹

                    // --- 애니메이션 설정 ---
                    val transition = rememberInfiniteTransition(label = "leaf-wave-L1")
                    val leafWaveRotation by transition.animateFloat(
                        initialValue = -9f,
                        targetValue = 8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 3000, easing = EaseInOutSine), // 3초
                            repeatMode = RepeatMode.Reverse // 부드럽게 왕복
                        ),
                        label = "leafWave"
                    )
                    // --- 이미지 파일 설정 ---
                    // 1. 새싹 줄기
                    Image(
                        painter = painterResource(id = R.drawable.learn_0_plant),
                        contentDescription = "새싹 줄기",
                        modifier = Modifier
                            .scale(0.9f)
                            .offset(y = 55.dp)
                    )

                    // 2. 새싹 머리
                    Image(
                        painter = painterResource(id = R.drawable.learn_0_head),
                        contentDescription = "새싹 머리",
                        modifier = Modifier
                            .scale(0.8f)
                            .offset(y = 69.dp, x = 2.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1.0f)
                                rotationY = leafWaveRotation
                            }
                    )
                }

                2 -> {// 레벨 2 묘목

                    // --- 애니메이션 설정 ---
                    val transition = rememberInfiniteTransition(label = "leaf-wave-L1")
                    val leafWaveRotation by transition.animateFloat(
                        initialValue = -9f, // -8도 (왼쪽)
                        targetValue = 9f,  // +8도 (오른쪽)
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2700, easing = EaseInOutSine), // 2.7초
                            repeatMode = RepeatMode.Reverse // 부드럽게 왕복
                        ),
                        label = "leafWave"
                    )

                    // --- 이미지 파일 설정 ---
                    // 1. 묘목 줄기
                    Image(
                        painter = painterResource(id = R.drawable.learn_1_plant),
                        contentDescription = "묘목 줄기",
                        modifier = Modifier
                            .scale(2.3f)
                            .offset(y = -15.dp)
                    )

                    // 2. 묘목 왼쪽 잎
                    Image(
                        painter = painterResource(id = R.drawable.learn_1_leafl),
                        contentDescription = "묘목 왼쪽 잎",
                        modifier = Modifier
                            .scale(2.3f)
                            .offset(y = -13.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1.1f)
                                rotationY = leafWaveRotation
                            }
                    )

                    // 3. 묘목 오른쪽 잎
                    Image(
                        painter = painterResource(id = R.drawable.learn_1_leafr),
                        contentDescription = "묘목 오른쪽 잎",
                        modifier = Modifier
                            .scale(2.3f)
                            .offset(y = -13.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1.1f)
                                rotationY = -leafWaveRotation
                            }
                    )

                    // 2. 묘목 3번째 잎
                    Image(
                        painter = painterResource(id = R.drawable.learn_1_leaf3),
                        contentDescription = "묘목 3번째 잎",
                        modifier = Modifier
                            .scale(2.3f)
                            .offset(y = -13.dp, x = (-0.5).dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1.1f)
                                rotationY = -leafWaveRotation
                            }
                    )
                }

                3 -> {// 레벨 3 꽃봉오리

                    // --- 애니메이션 설정 ---
                    val transition = rememberInfiniteTransition(label = "leaf-wave-L1")
                    val leafWaveRotation by transition.animateFloat(
                        initialValue = -7f, //
                        targetValue = 7f,  //
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2700, easing = EaseInOutSine), // 2.7초
                            repeatMode = RepeatMode.Reverse // 부드럽게 왕복
                        ),
                        label = "leafWave"
                    )

                    // 1. 무한 반복 애니메이션 트랜지션 생성
                    val infiniteTransition = rememberInfiniteTransition(label = "flower_bob_transition")

                    // 2. Y축(위아래) 값을 애니메이션으로 정의
                    val translationY by infiniteTransition.animateFloat(
                        initialValue = -6f,
                        targetValue = 6f,
                        animationSpec = infiniteRepeatable(
                            // 1.5초(1500ms) 동안
                            animation = tween(
                                durationMillis = 2300,
                                easing = EaseInOut // 부드럽게 시작하고 끝나도록
                            ),
                            // RepeatMode.Reverse: -8 -> 8로 갔다가 8 -> -8로 돌아옵니다. (왕복)
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "flower_y_translation"
                    )

                    // --- 이미지 파일 설정 ---
                    // 1. 줄기
                    Image(
                        painter = painterResource(id = R.drawable.learn_2_plant),
                        contentDescription = "줄기",
                        modifier = Modifier
                            .scale(scaleX = 0.9f, scaleY = 1.1f)
                            .offset(y = -25.dp)
                    )

                    // 2. 꽃봉오리
                    Image(
                        painter = painterResource(id = R.drawable.learn_2_flower),
                        contentDescription = "꽃봉오리",
                        modifier = Modifier
                            .scale(0.8f)
                            .offset(y = 13.dp)
                            .graphicsLayer {
                                this.translationY = translationY
                            }
                    )
                    // 3. 잎 왼쪽
                    Image(
                        painter = painterResource(id = R.drawable.learn_2_leafl),
                        contentDescription = "왼쪽 잎",
                        modifier = Modifier
                            .scale(0.9f)
                            .offset(y = 20.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                                rotationY = leafWaveRotation
                            }
                    )

                    // 4. 잎 오른쪽
                    Image(
                        painter = painterResource(id = R.drawable.learn_2_leafr),
                        contentDescription = "오른쪽 잎",
                        modifier = Modifier
                            .scale(0.9f)
                            .offset(y = 20.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                                rotationY = -leafWaveRotation
                            }
                    )
                }

                4 -> { // 레벨 4 꽃

                    // --- 애니메이션 설정 ---
                    val transition = rememberInfiniteTransition(label = "leaf-wave-L1")
                    val leafWaveRotation by transition.animateFloat(
                        initialValue = -4f, //
                        targetValue = 4f,  //
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2700, easing = EaseInOutSine), // 2.7초
                            repeatMode = RepeatMode.Reverse // 부드럽게 왕복
                        ),
                        label = "leafWave"
                    )

                    // 1. 꽃(Flower) 애니메이션
                    val flowerTransition = rememberInfiniteTransition(label = "flower-sway")
                    val flowerRotation by flowerTransition.animateFloat(
                        initialValue = -3f, // -2 degrees (좌)
                        targetValue = 3f,  // +2 degrees (우)
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2500, easing = EaseInOutSine), // 4초
                            repeatMode = RepeatMode.Reverse // 4초마다 방향 반전 (총 8초)
                        ),
                        label = "flowerRotation"
                    )

                    // --- 이미지 파일 설정 ---
                    // 1. 줄기 (맨 아래 배경 바로 위)
                    Image(
                        painter = painterResource(id = R.drawable.learn_3_plant),
                        contentDescription = "줄기",
                        modifier = Modifier
                            .scale(0.9f)
                            .offset(y = 20.dp)
                    )

                    // 2. 꽃
                    Image(
                        painter = painterResource(id = R.drawable.learn_3_flower),
                        contentDescription = "꽃",
                        modifier = Modifier
                            .scale(0.9f)
                            .offset(y = 22.dp)
                            .graphicsLayer {
                                // rotationZ를 사용하여 Z축(화면을 뚫는 축) 기준으로 회전시킵니다.
                                rotationZ = flowerRotation

                                // [전문가 팁]
                                // 꽃이 줄기 끝(하단 중앙)을 기준으로 흔들리게 하려면
                                // 회전 중심(transformOrigin)을 변경하세요.
                                // 기본값은 (0.5f, 0.5f) - 중앙입니다.
                            }
                    )

                    // 3. 잎 왼쪽
                    Image(
                        painter = painterResource(id = R.drawable.learn_3_leafl),
                        contentDescription = "왼쪽 잎",
                        modifier = Modifier
                            .scale(0.8f)
                            .offset(y = 42.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                                rotationY = leafWaveRotation
                            }
                    )

                    // 4. 잎 오른쪽
                    Image(
                        painter = painterResource(id = R.drawable.learn_3_leafr),
                        contentDescription = "오른쪽 잎",
                        modifier = Modifier
                            .scale(0.8f)
                            .offset(y = 42.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                                rotationY = -leafWaveRotation
                            }
                    )
                }

                5 -> {// 레벨 5 열매

                    // --- 애니메이션 설정 ---
                    val transition = rememberInfiniteTransition(label = "leaf-wave-L1")
                    val leafWaveRotation by transition.animateFloat(
                        initialValue = -4f, //
                        targetValue = 4f,  //
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2700, easing = EaseInOutSine), // 2.7초
                            repeatMode = RepeatMode.Reverse // 부드럽게 왕복
                        ),
                        label = "leafWave"
                    )

                    // --- 이미지 파일 설정 ---
                    // 1. 줄기
                    Image(
                        painter = painterResource(id = R.drawable.learn_4_plant),
                        contentDescription = "줄기",
                        modifier = Modifier
                            .scale(0.82f)
                            .offset(y = 55.dp)
                    )

                    // 2. 베리1
                    Image(
                        painter = painterResource(id = R.drawable.learn_4_berry),
                        contentDescription = "베리1",
                        modifier = Modifier
                            .scale(0.82f)
                            .offset(y = 15.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.6f, pivotFractionY = 1f)
                                rotationY = leafWaveRotation
                            }
                    )
                    // 3. 베리2
                    Image(
                        painter = painterResource(id = R.drawable.learn_4_berry2),
                        contentDescription = "베리2",
                        modifier = Modifier
                            .scale(0.82f)
                            .offset(y = 50.dp, x=(-3).dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                                rotationY = leafWaveRotation
                            }
                    )
                    // 4. 베리3
                    Image(
                        painter = painterResource(id = R.drawable.learn_4_berry3),
                        contentDescription = "베리3",
                        modifier = Modifier
                            .scale(0.82f)
                            .offset(y = 50.dp, x =(-4).dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                                rotationY = -leafWaveRotation
                            }
                    )
                }

                else -> {
                    // 정의되지 않은 레벨일 경우 (기본값, 예: 레벨 1 표시)
                }
            }

            // --- 공통 이미지 파일 설정 ---

            // Z-Order 3: 물방울
            Box(
                modifier = Modifier
                    .scale(0.13f),
                contentAlignment = Alignment.TopCenter
            ) {
                // 물방울 개별 위치 로직 (이전과 동일)
                val pos1 = Pair((-700).dp, (-255).dp)
                val pos2 = Pair((-350).dp, (-140).dp)
                val pos3 = Pair(0.dp, (-225).dp)
                val pos4 = Pair(350.dp, (-140).dp)
                val pos5 = Pair(700.dp, (-255).dp)

                val positionsToShow = when (dropCount) {
                    1 -> listOf(pos3)
                    2 -> listOf(pos3, pos4)
                    3 -> listOf(pos2, pos3, pos4)
                    4 -> listOf(pos3, pos2, pos4, pos5)
                    5 -> listOf(pos1, pos2, pos3, pos4, pos5)
                    else -> emptyList()
                }

                // 각 물방울을 그릴 때 개별 애니메이션 상태를 유지하기 위해
                // 별도의 Composable 함수(RaindropItem)를 호출합니다.
                positionsToShow.forEach { (xOffset, yOffset) ->
                    RaindropItem(
                        xOffset = xOffset,
                        yOffset = yOffset,
                        bobbingY = animatedRaindropBobbingY,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            // 구름 클릭 시
            val coroutineScope = rememberCoroutineScope()

            Image(
                painter = painterResource(id = R.drawable.learn_cloud),
                contentDescription = "구름",
                modifier = Modifier
                    .offset(y = (-130).dp + cloudOffsetY.dp)
                    .scale(1.9f * cloudScale)
                    .noRippleClickable{
                        if (!cloudClicked) {
                            cloudClicked = true
                            coroutineScope.launch {
                                delay(300)
                                cloudClicked = false
                            }
                        }
                    }
            )
        }
    }
}

/**
 * 개별 물방울 아이템
 * 자체적인 클릭 상태와 바운스 애니메이션을 관리
 */
@Composable
fun RaindropItem(
    xOffset: Dp,
    yOffset: Dp,
    bobbingY: Float,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // 클릭 시 바운스 되는 추가 Y 오프셋 (기본 0)
    val bounceOffsetY = remember { Animatable(0f) }

    // 클릭 중복 방지
    var isBouncing by remember { mutableStateOf(false) }

    Image(
        painter = painterResource(id = R.drawable.learn_waterdrop3),
        contentDescription = "물방울",
        modifier = modifier
            .scale(scaleX = 0.9f, scaleY = 1f)
            // 위치: 기본 좌표 + 둥실거림(bobbing) + 클릭 바운스(bounce)
            .offset(x = xOffset, y = yOffset + bobbingY.dp + bounceOffsetY.value.dp)
            .noRippleClickable {
                if (!isBouncing) {
                    isBouncing = true
                    scope.launch {
                        // 1. 아래로 크게 떨어짐 (가속)
                        // 좌표계가 0.13배 축소되어 있으므로 600f 정도면 화면상에서 꽤 큰 움직임입니다.
                        bounceOffsetY.animateTo(
                            targetValue = 500f,
                            animationSpec = tween(durationMillis = 300, easing = EaseIn)
                        )
                        // 2. 다시 원래 위치로 복귀 (바운스 효과)
                        bounceOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 600, easing = EaseOutBack)
                        )
                        isBouncing = false
                    }
                }
            }
    )
}
