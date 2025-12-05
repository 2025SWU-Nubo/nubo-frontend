package com.example.nubo.ui.screen.learn

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// LevelUpSection 내부에서만 쓰이는 애니메이션 상수 이동
private const val STEP_ANIM_MS = 700   // 바(프로그레스) 옆칸 이동 시간
private const val CHECK_BOUNCE_MS = 150 // 체크 살짝 튀는 시간

// 레벨 값(1~total)을 0~1 구간으로 바꿔주는 보조 함수
private fun stepToFraction(step: Int, total: Int): Float {
    if (total <= 1) return 1f

    // 레벨(1~total)을 인덱스(0~total-1)로 변환
    val index = (step - 1).coerceIn(0, total - 1)

    // 0번 동그라미(첫 단계) → 0f
    // 마지막 동그라미(마지막 단계) → 1f
    return index.toFloat() / (total - 1).toFloat()
}


@Composable
fun LevelUpSection(
    totalSteps: Int,
    prevStep: Int,
    nextStep: Int,
    onStepAnimDone: () -> Unit,
) {
    // 1) 시작/목표 퍼센트 (목표는 체크 지점으로 스냅)
    val fromFrac = stepToFraction(prevStep-1, totalSteps).coerceIn(0f, 1f)
    val toFrac = stepToFraction(nextStep, totalSteps)

    // 2) 진행도는 단순 Float 상태로 관리 (프레임마다 값 갱신)
    var barProgress by remember { mutableFloatStateOf(fromFrac) }

    // 3) 체크 개수/튀는 효과
    var checkedCount by remember { mutableStateOf(prevStep-1) }
    var showCheck by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (showCheck) 1f else 0.6f,
        animationSpec = tween(durationMillis = CHECK_BOUNCE_MS),
        label = "checkScale"
    )

    // 4) 애니메이션: 진행 중 스냅 지점 통과 시 체크 생성
    LaunchedEffect(prevStep, nextStep) {
        barProgress = fromFrac
        val trigger = toFrac - 0.0005f // 부동소수 안전 마진

        delay(600) // 타이틀 감상 시간

        var spawned = false
        animate(
            initialValue = fromFrac,
            targetValue = toFrac,
            animationSpec = tween(durationMillis = STEP_ANIM_MS, easing = LinearEasing)
        ) { value, _ ->
            // 프레임마다 진행도 업데이트
            barProgress = value

            // 스냅 지점 도달 순간 체크 생성
            if (!spawned && value >= trigger) {
                spawned = true

                // 다음 레벨까지 바가 도달한 시점이므로,
                // 체크 개수도 다음 레벨로 맞춰줌
                checkedCount = nextStep

                showCheck = true

                // 체크 "톡" 효과 잠깐 보여주고 해제
                launch {
                    delay(220)
                    showCheck = false
                }
            }
        }

        onStepAnimDone()
    }

    // 레벨 이름 매핑
    val stageNames = listOf("새싹", "묘목", "꽃봉오리", "꽃", "열매")

    // nextStep 기준으로 현재 레벨 텍스트 자동 생성
    val currentStageIndex = (nextStep - 1).coerceIn(0, stageNames.lastIndex)
    val currentStageName = stageNames[currentStageIndex]

    // ---- UI ----
    Column {
        Text(
            text = "LEVEL UP!",
            style = AppTextStyles.learn_percentage_46.copy(
                brush = Brush.linearGradient(listOf(Color(0xFF8380FF), PurpleMain500))
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "레벨$nextStep. $currentStageName 으로 성장했어요.",
            style = AppTextStyles.b2_semibold_16,
            color = Grey1000
        )
        Spacer(Modifier.height(30.dp))

        StepBar(
            total = totalSteps,
            progress = barProgress,      // 진행 중 값
            checkedCount = checkedCount, // 트리거 순간 증가
            showBounceOnLast = showCheck,
            lastCheckScale = checkScale,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 레벨업 스텝 바
@Composable
private fun StepBar(
    total: Int,
    progress: Float,             // 0f~1f (바 채움 비율, 체크 지점으로 스냅된 값 전달)
    checkedCount: Int,           // 현재 활성 단계(1부터 시작)
    showBounceOnLast: Boolean,   // 마지막 동그라미 튀는 효과 여부
    lastCheckScale: Float,       // 마지막 동그라미 스케일 값
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp,
    circleSize: Dp = 24.dp,      // 단계 동그라미 크기
    berrySize: Dp = 40.dp,       // 베리 아이콘 크기
    berryOffsetUp: Dp = 16.dp,   // 마지막 동그라미 위로 띄우는 높이
    trackColor: Color = Grey50   // 기본 트랙 색 계열
) {
    // 단계 이름 리스트
    val stageNames = listOf("새싹", "묘목", "꽃봉오리", "꽃", "열매")

    // 보라색 바 그라디언트 (좌 → 우)
    val barGradient = remember {
        Brush.horizontalGradient(
            listOf(Color(0xFF7272FF), PurpleMain500)
        )
    }

    // 회색 트랙 그라디언트 (위 → 아래)
    val trackGradient = remember {
        Brush.verticalGradient(
            listOf(Color(0xFFB0B0B0), trackColor)
        )
    }

    // 비활성 회색 동그라미 그라디언트
    val inactiveCircleGradient = remember {
        Brush.verticalGradient(
            listOf(Color(0xFFB0B0B0), Color(0xFFC8C8C8))
        )
    }

    // 현재 활성 단계 인덱스(0 기반)로 변환
    val lastActiveIndex = (checkedCount - 1).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = circleSize / 2)          // 끝 동그라미 안 잘리게 여백
            .height(max(circleSize, barHeight) + 10.dp)   // 전체 높이 확보
    ) {
        val stepFrac = if (total > 1) 1f / (total - 1) else 1f

        // (1) 회색 트랙
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(trackGradient)                 // 단색 → 그라디언트
                .zIndex(0f)
        )

        // (2) 채워진 바 (보라색 그라디언트)
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(barHeight)
                .zIndex(0.1f)
        ) {
            val radius = size.height / 2f
            val filledW = size.width * progress.coerceIn(0f, 1f)
            if (filledW > 0f) {
                drawRoundRect(
                    brush = barGradient,
                    size = Size(filledW, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }

        // (3) 단계 동그라미들 (체크 아이콘 제거, 숫자 + 보라 동그라미로 변경)
        repeat(total) { i ->
            val x = maxWidth * (i * stepFrac)
            val isActive = (i + 1) <= checkedCount
            val isLastActive = isActive && (i == lastActiveIndex) && showBounceOnLast

            // 활성 동그라미 색 계산
            val activeCircleColor: Color = if (!isActive) {
                Color.Unspecified
            } else {
                if (lastActiveIndex <= 0) {
                    // 단계가 하나만 활성일 때는 메인 퍼플
                    PurpleMain500
                } else {
                    // 새싹(첫 단계) 쪽은 밝게, 마지막 활성 단계로 갈수록 진하게
                    val t = i.toFloat() / lastActiveIndex.toFloat()   // 0f ~ 1f
                    val start = Color(0xFF7272FF)
                    val end = PurpleMain500
                    lerp(start, end, t)
                }
            }

            // 동그라미 배경 브러시 결정
            val circleBackgroundModifier = if (isActive) {
                Modifier.background(color = activeCircleColor, shape = CircleShape)
            } else {
                Modifier.background(brush = inactiveCircleGradient, shape = CircleShape)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = x - circleSize / 2)
                    .size(circleSize)
                    .clip(CircleShape)
                    // 마지막 활성 단계일 때만 스케일 애니메이션 적용
                    .graphicsLayer {
                        if (isLastActive) {
                            scaleX = lastCheckScale
                            scaleY = lastCheckScale
                        }
                    }
                    .then(circleBackgroundModifier)
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${i + 1}",
                    style = AppTextStyles.b3_medium_14,
                    color = if (isActive) Color.White else Color(0xFFA2A2A2)
                )
            }
        }

        // (4) 베리 — 맨 마지막 동그라미 위 (기존과 동일)
        val lastIdx = (total - 1).coerceAtLeast(0)
        val berryX = maxWidth * (lastIdx * stepFrac)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(
                    x = berryX - berrySize / 2,
                    y = -(circleSize / 2 + berryOffsetUp + 8.dp)
                )
                .size(berrySize)
                .clip(CircleShape)
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.learn_level_berry),
                contentDescription = "누베리 획득",
                tint = Color.Unspecified
            )
            Image(
                painter = painterResource(id = R.drawable.learn_nuberry_total),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
    Spacer(Modifier.height(4.dp))

    // 단계 이름 텍스트 줄
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = circleSize / 2)
    ) {
        val stepFrac = if (total > 1) 1f / (total - 1) else 1f
        val labelWidth = circleSize * 2f

        stageNames.forEachIndexed { index, name ->
            val isActive = index <= lastActiveIndex
            val x = maxWidth * (index * stepFrac)

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    // 동그라미 중심은 그대로 두고, 글자 박스 폭만 넓혀서 가운데 정렬
                    .offset(x = x - labelWidth / 2)
                    .width(labelWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    style = AppTextStyles.caption_regular_9,
                    textAlign = TextAlign.Center,
                    color = if (isActive) Grey1000 else GreyMain300
                )
            }
        }
    }
}
