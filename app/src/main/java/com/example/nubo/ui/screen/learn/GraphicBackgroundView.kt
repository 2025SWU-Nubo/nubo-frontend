package com.example.nubo.ui.screen.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInOutBack
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            painter = painterResource(id = R.drawable.learn_bg),
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

                    // 새싹 머리 클릭 시
                    val sproutShakeAnim = remember { Animatable(0f) }

                    val onClick: () -> Unit = {
                        coroutineScope.launch {
                            sproutShakeAnim.snapTo(0f) // 리셋

                            // 기존 살랑거림보다 크게 2번 왕복
                            repeat(2) {
                                // 한쪽으로 휙
                                sproutShakeAnim.animateTo(
                                    targetValue = 30f,
                                    animationSpec = tween(durationMillis = 150, easing = EaseInOutSine)
                                )
                                // 반대쪽으로 휙
                                sproutShakeAnim.animateTo(
                                    targetValue = -10f,
                                    animationSpec = tween(durationMillis = 150, easing = EaseInOutSine)
                                )
                            }
                            // 제자리로 복귀 (탄성 효과)
                            sproutShakeAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                    }

                    // --- 애니메이션 설정 (기본 살랑거림) ---
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
                            .offset(y = 169.dp, x = (-8).dp)
                            .graphicsLayer {
                                // 머리 하단 중앙 고정
                                transformOrigin = TransformOrigin(pivotFractionX = 1.0f, pivotFractionY = 0.9f)

                                rotationY = leafWaveRotation + sproutShakeAnim.value
                            }
                            .noRippleClickable(onClick = onClick)
                    )
                }

                2 -> {// 레벨 2 묘목

                    // 클릭하면 묘목 잎 으쓱
                    val saplingShrugAnim = remember { Animatable(0f) }

                    val onClick: () -> Unit = {
                        coroutineScope.launch {
                            saplingShrugAnim.snapTo(0f)
                            repeat(2) {
                                saplingShrugAnim.animateTo(
                                    targetValue = 5f,
                                    animationSpec = tween(durationMillis = 200, easing = EaseInOutSine)
                                )
                                // 아래로 살짝 (-4도)
                                saplingShrugAnim.animateTo(
                                    targetValue = -4f,
                                    animationSpec = tween(durationMillis = 200, easing = EaseInOutSine)
                                )
                            }
                            saplingShrugAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                    }

                    // --- 기본 애니메이션 설정 (바람 + 으쓱) ---
                    val transition = rememberInfiniteTransition(label = "leaf-wave-L1")

                    // 1. 기본 펄럭임 (Y축: 앞뒤 입체감)
                    val leafWaveY by transition.animateFloat(
                        initialValue = -8f,
                        targetValue = 8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "leafWaveY"
                    )

                    // 2. 기본 으쓱임 (Z축: 위아래 들썩임)
                    val leafShrugZ by transition.animateFloat(
                        initialValue = -3f,  // 살짝 내려갔다가
                        targetValue = 3f,   // 위로 12도 정도 들림 (숨쉬는 느낌)
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "leafShrugZ"
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
                            .offset(x = (-10).dp, y = 30.dp)
                            .graphicsLayer {
                                // 오른쪽 끝 하단 고정
                                transformOrigin = TransformOrigin(pivotFractionX = 1.0f, pivotFractionY = 0.9f)

                                // Y축: 기본 펄럭임
                                rotationY = leafWaveY
                                // Z축: 기본 으쓱임 + 클릭했을 때 강한 으쓱임
                                rotationZ = leafShrugZ + saplingShrugAnim.value
                            }
                            .noRippleClickable(onClick = onClick)
                    )

                    // 3. 묘목 오른쪽 잎
                    Image(
                        painter = painterResource(id = R.drawable.learn_1_leafr),
                        contentDescription = "묘목 오른쪽 잎",
                        modifier = Modifier
                            .scale(2.3f)
                            .offset(y = 29.dp, x = 5.dp)
                            .graphicsLayer {
                                // 왼쪽 끝 하단 고정
                                transformOrigin = TransformOrigin(pivotFractionX = 0.0f, pivotFractionY = 0.9f)

                                // 반대 방향 움직임
                                rotationY = -leafWaveY
                                rotationZ = -leafShrugZ - saplingShrugAnim.value
                            }
                            .noRippleClickable(onClick = onClick)
                    )

                    // 4. 묘목 3번째 잎 (중앙 뒤)
                    Image(
                        painter = painterResource(id = R.drawable.learn_1_leaf3),
                        contentDescription = "묘목 3번째 잎",
                        modifier = Modifier
                            .scale(2.3f)
                            .offset(y = 70.dp, x = 9.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.0f, pivotFractionY = 0.4f)

                                rotationY = -leafWaveY * 0.4f
                                // 뒤쪽 잎은 으쓱임을 절반만 적용해서 깊이감 주기
                                rotationZ = (-leafShrugZ - saplingShrugAnim.value) * 0.5f
                            }
                            .noRippleClickable(onClick = onClick)
                    )
                }

                3 -> {// 레벨 3 꽃봉오리

                    // 꽃봉오리 클릭 시 "가로로 커졌다가 작아지기"
                    val budScaleAnim = remember { Animatable(1f) }

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
                            // 왕복
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
                            .offset(y = (-25).dp)
                    )

                    // 2. 꽃봉오리
                    Image(
                        painter = painterResource(id = R.drawable.learn_2_flower),
                        contentDescription = "꽃봉오리",
                        modifier = Modifier
                            .scale(scaleX = 0.8f* budScaleAnim.value,scaleY=0.8f)
                            .offset(y = 120.dp)
                            .graphicsLayer {
                                // 커질 때 줄기에서 자라나듯 하단 고정
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1.0f)
                                this.translationY = translationY
                            }
                            .noRippleClickable {
                                coroutineScope.launch {
                                    // 1.2배로 커졌다가
                                    budScaleAnim.animateTo(1.4f, tween(200))
                                    // 원래대로 쫀득하게 복귀
                                    // 2. 부드럽게 돌아오기 (EaseInOutSine) - 튕김 제거
                                    budScaleAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 250, easing = EaseInOutSine)
                                    )
                                }
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

                    // 꽃 클릭 시 좌우로 크게 3번 흔들흔들
                    val flowerShakeAnim = remember { Animatable(0f)}

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

                    // 2. 꽃
                    Image(
                        painter = painterResource(id = R.drawable.learn_3_flower),
                        contentDescription = "꽃",
                        modifier = Modifier
                            .scale(0.9f)
                            // Y 오프셋에 바운스 값 추가 (아래로 내려갔다 옴)
                            .offset(x=(-1).dp,y = 120.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.1f)

                                // 기본 살랑거림 + 클릭 시 큰 흔들림 합산
                                rotationZ = flowerRotation + flowerShakeAnim.value

                                // 꽃이 줄기 끝(하단 중앙)을 기준으로 흔들리게 하려면
                                // 회전 중심(transformOrigin)을 변경
                                // 기본값은 (0.5f, 0.5f) - 중앙
                            }
                            .noRippleClickable {
                                coroutineScope.launch {
                                    // 이미 흔들리고 있다면 리셋
                                    flowerShakeAnim.snapTo(0f)

                                    // 좌우로 크게 3번 왕복 (총 4번 이동)
                                    repeat(2) {
                                        // 왼쪽으로 휙 (-25도)
                                        flowerShakeAnim.animateTo(
                                            targetValue = -15f,
                                            animationSpec = tween(durationMillis = 200, easing = EaseInOutSine)
                                        )
                                        // 오른쪽으로 휙 (25도)
                                        flowerShakeAnim.animateTo(
                                            targetValue = 15f,
                                            animationSpec = tween(durationMillis = 200, easing = EaseInOutSine)
                                        )
                                    }
                                    // 중앙으로 복귀 (탄성 효과)
                                    flowerShakeAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }
                    )
                }

                5 -> {// 레벨 5 열매
                    // ★ 모든 베리가 공유하는 클릭 상태
                    var areBerriesActive by remember { mutableStateOf(false) }

                    // 클릭 시 실행할 동작 (한 번 누르면 0.3초간 모든 베리 변신)
                    val onBerryClick = {
                        if (!areBerriesActive) {
                            coroutineScope.launch {
                                areBerriesActive = true
                                delay(300) // 0.3초 유지
                                areBerriesActive = false
                            }
                        }
                    }

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

                    // 3. 베리2 (왼쪽 아래) -> 노란색(Yellow)으로 변신
                    BerryItem(
                        resId = R.drawable.learn_4_berry2,
                        clickedResId = R.drawable.learn_4_berry_orange, // 업로드해주신 파일명
                        offsetX = (-33).dp,
                        offsetY = 102.dp,
                        scale = 0.82f,
                        clickedScale = 0.82f,
                        swayRotation = leafWaveRotation,
                        swayOrigin = TransformOrigin(0.5f, 1.3f),
                        isActive = areBerriesActive,
                        onClick = { onBerryClick() }
                    )

                    // 4. 베리3 (오른쪽 아래) -> 주황색(Orange)으로 변신
                    BerryItem(
                        resId = R.drawable.learn_4_berry3,
                        clickedResId = R.drawable.learn_4_berry_yellow, // 업로드해주신 파일명
                        offsetX = 23.dp,
                        offsetY = 107.dp,
                        scale = 0.82f,
                        clickedScale = 0.82f,
                        swayRotation = -leafWaveRotation,
                        swayOrigin = TransformOrigin(0.5f, 1.3f),
                        isActive = areBerriesActive,
                        onClick = { onBerryClick() }
                    )

                    // 2. 베리1 (중앙) -> 빨간색(Red)으로 변신
                    BerryItem(
                        resId = R.drawable.learn_4_berry,
                        clickedResId = R.drawable.learn_4_berry_red, // 업로드해주신 파일명
                        offsetX = (-2).dp,
                        offsetY = 90.dp,
                        scale = 0.82f,
                        clickedScale = 0.82f, // 클릭 시 커지는 크기
                        swayRotation = leafWaveRotation,
                        swayOrigin = TransformOrigin(0.6f, 1.3f),
                        isActive = areBerriesActive, // 공유 상태 전달
                        onClick = { onBerryClick() } // 공유 클릭 함수 전달
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
                val pos1 = Pair((-700).dp, (-195).dp)
                val pos2 = Pair((-350).dp, (-95).dp)
                val pos3 = Pair(0.dp, (-170).dp)
                val pos4 = Pair(350.dp, (-95).dp)
                val pos5 = Pair(700.dp, (-195).dp)

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
// 베리 클릭 애니메이션
@Composable
fun BerryItem(
    resId: Int,          // 기본 이미지
    clickedResId: Int,   // 클릭 시 보여줄 이미지
    offsetX: Dp,
    offsetY: Dp,
    scale: Float,        // 기본 크기
    clickedScale: Float, // 클릭 시 커질 크기
    swayRotation: Float, // 흔들림 각도
    swayOrigin: TransformOrigin, // 매달린 위치
    isActive: Boolean,   // ★ 부모가 내려주는 활성 상태 (true면 변신)
    onClick: () -> Unit  // ★ 클릭 이벤트 리스너
) {
    // isActive 상태에 따라 크기 애니메이션
    val currentScale by animateFloatAsState(
        targetValue = if (isActive) clickedScale else scale,
        animationSpec = tween(durationMillis = 500, easing = EaseInOutSine),
        label = "berryScale"
    )

    // 1. 바깥 Box: 줄기에 매달려 살랑거리는 역할
    Box(
        modifier = Modifier
            .scale(1f) // 박스 스케일은 고정
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                transformOrigin = swayOrigin
                rotationY = swayRotation
            }
    ) {
        // 2. 안쪽 Image: 클릭 시 이미지 교체 + 크기 변경
        Image(
            // 활성 상태면 클릭 이미지, 아니면 기본 이미지 표시
            painter = painterResource(id = if (isActive) clickedResId else resId),
            contentDescription = "베리",
            modifier = Modifier
                .align(Alignment.Center)
                .scale(currentScale) // 애니메이션된 크기 적용
                .noRippleClickable { onClick() } // 클릭 시 부모에게 알림
        )
    }
}
