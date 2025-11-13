package com.example.nubo.ui.screen.learn

import com.example.nubo.ui.screen.learn.GlbBackgroundView
import com.example.nubo.ui.screen.learn.GraphicBackgroundView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey0
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import java.time.LocalDate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.Grey50
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


// BottomProgressCard 애니메이션 시간 조절
// 전역에서 쓸 상수 (밀리초)
private const val ENTER_MS = 800     // 카드 안 콘텐츠가 위로 올라오는 시간
private const val EXIT_MS = 700      // 내려가는 시간
private const val LEVEL_HOLD_MS = 3500 // 레벨업 화면 유지 시간
private const val STEP_ANIM_MS = 700   // 바(프로그레스) 옆칸 이동 시간
private const val CHECK_BOUNCE_MS = 150 // 체크 살짝 튀는 시간

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = hiltViewModel(),
    navController: NavController
) {

    // ViewModel의 UI 상태를 구독
    val uiState by viewModel.uiState.collectAsState()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // --- ViewModel로부터 이벤트 직접 구독 ---
    val levelUpStage: Int? by viewModel.levelUpEvent.collectAsState()
    val berryGained: Boolean by viewModel.berryGainedEvent.collectAsState()

    // levelUpStage가 null이 아닌지 여부로 애니메이션 트리거 결정
    val showLevelUp = levelUpStage != null

//================= 화면 UI 시작 =================
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(0xFF96C1D2))
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is DashboardUiState.Error -> {
                GraphicBackgroundView(
                    modifier = Modifier.fillMaxSize(),
                    todayVideoCount = 3, // 오늘 카운트 값 2D 그래픽 파일에 전달
                    level = 4
                )
                Text(
                    text = "시스템 오류가 발생했습니다.",
                    style = AppTextStyles.title_semibold_24,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top=300.dp),
                    color = Color.Black)
            }

            is DashboardUiState.Success -> {
                val dashboardData = state.data

                // --- 데이터 연결 ---
                // 1. 캘린더 날짜/카운트
                val weeklyCounts = dashboardData.weeklyVideoCounts
                val weekDatesFromServer = weeklyCounts.map { it.date }
                val weekDayNumbers = weeklyCounts.map { LocalDate.parse(it.date).dayOfMonth }
                val bubbleCounts = weeklyCounts.map { it.count }

                // 2. 오늘 날짜 계산
                val todayString = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
                val todayIndex = remember(weekDatesFromServer) {
                    weekDatesFromServer.indexOf(todayString).coerceAtLeast(0)
                }

                // 3. 성장률 및 누베리
                val growthRate = dashboardData.growthRate
                val berryCount = dashboardData.berryCount
                val currentStage = dashboardData.stage

                // 오늘 학습 카운트 계산 (물방울 개수 적용)
                val todayCount = bubbleCounts.getOrNull(todayIndex) ?: 0

                /*GlbBackgroundView(
                    modifier = Modifier.fillMaxSize(),
                    glbUrl = dashboardData.dashboardBackground,
                    todayVideoCount = todayCount // 오늘 카운트 값 3D 그래픽 파일에 전달
                )*/
                GraphicBackgroundView(
                    modifier = Modifier.fillMaxSize(),
                    todayVideoCount = todayCount, // 오늘 카운트 값 2D 그래픽 파일에 전달
                    level = currentStage
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    TopBar(
                        title = "대시보드",
                        onClickChart = { /* TODO */ }
                    )
                    Spacer(Modifier.height(12.dp))


                    // 주간 캘린더
                    WeeklyCalendar(
                        daysLabel = remember { listOf("일", "월", "화", "수", "목", "금", "토") },
                        // 서버에서 받은 날짜 숫자로 변경
                        datesAsInt = weekDayNumbers,
                        todayIndex = todayIndex,
                        selectedIndex = selectedIndex,
                        hasSelection = selectedIndex != null,
                        onClickDay = { idx ->
                            // 같은 날짜를 다시 클릭하면 해제, 다른 날짜 클릭 시 해당 날짜로 교체
                            selectedIndex = if (selectedIndex == idx) null else idx
                        },
                        getBubbleCount = { idx -> bubbleCounts.getOrNull(idx) ?: 0 },
                        circleSize = 36.dp
                    )

                    Spacer(Modifier.weight(1f))

                    // 하단 성장률 카드 + 누베리 개수 원형 카드
                    BottomProgressCard(
                        // 서버 데이터 연결
                        percent = growthRate,
                        title = "누베리 성장률",
                        progress = growthRate / 100f,
                        topBadgeCount = berryCount,
                        // 레벨업 UI에 서버 데이터 연결
                        showLevelUp = showLevelUp,
                        showBerry = berryGained,
                        currentStep = dashboardData.stage, // 현재 stage 전달
                        // 레벨업 후 텍스트를 동적으로 생성하여 전달
                        levelUpText = levelUpStage?.let { newStage ->
                            // getStageName이 1단계부터 이름을 반환한다고 가정
                            "레벨${newStage}. ${getStageName(newStage)}로 성장했어요."
                        } ?: "", // null일 경우 빈 문자열 (보일 일 없음)
                        currentProgressFromServer = growthRate / 100f,

                        // 애니메이션이 끝나면 ViewModel에 알려줄 콜백 전달
                        onLevelUpAnimationDone = {
                            viewModel.onLevelUpAnimationFinished()
                        },
                        onBerryAnimationDone = {
                            // ViewModel에 이 함수를 추가해야 합니다.
                            viewModel.onBerryAnimationFinished()
                        }
                    )
                    Spacer(Modifier.height(130.dp))
                }
            }
        }
    }
}

//================= 상단 탑바 (타이틀 + 차트 버튼) =================

@Composable
private fun TopBar(
    title: String,
    onClickChart: () -> Unit
) {
    // 상단 바: 타이틀 중앙 + 우측 아이콘 버튼
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        // 타이틀 텍스트
        Text(
            text = title,
            style = AppTextStyles.subtitle_semibold_20,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 차트 버튼
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(38.dp)
                .clip(CircleShape),
            //.clickable(onClick = onClickChart),
            contentAlignment = Alignment.Center
        ) {
            /*Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = "통계",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )*/
        }
    }
}

//================= 상단 캘린더 영역 =================

@Composable
private fun WeeklyCalendar(
    daysLabel: List<String>,
    datesAsInt: List<Int>,
    todayIndex: Int,
    selectedIndex: Int?,
    onClickDay: (Int) -> Unit,
    hasSelection: Boolean,
    getBubbleCount: (Int) -> Int,
    circleSize: Dp
) {
    // 요일 라벨 줄
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysLabel.forEachIndexed { idx, dayLabel ->
            val isToday = idx == todayIndex
            Text(
                text = dayLabel,
                style = AppTextStyles.subtitle_semibold_20,
                color = if (isToday) PurpleMain500 else Grey0,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // 날짜 숫자 버튼 줄
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        datesAsInt.forEachIndexed { idx, dayNumber ->
            val isSelected = selectedIndex == idx
            val isToday = idx == todayIndex

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // 화면 진입 시 오늘 날짜에 동그라미 표시
                // 선택하면 해당 날짜에 동그라미 표시
                if (isSelected || (isToday && !hasSelection)) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                    )
                }

                // 날짜 숫자(클릭 영역)
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .noRippleClickable { onClickDay(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    // 오늘 날짜는 보라색
                    Text(
                        text = dayNumber.toString(),
                        style = AppTextStyles.subtitle_semibold_20,
                        color = when {
                            isToday -> PurpleMain500
                            isSelected -> Grey1000
                            else -> Grey0
                        }
                    )
                }
            }
        }
    }

    // 선택한 날짜 하단 말풍선
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        datesAsInt.forEachIndexed { idx, _ ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // 1. 현재 아이템이 선택되었거나 (isSelected)
                // 2. (현재 아이템이 오늘 날짜이고 (isToday) &&
                //    아직 아무것도 선택하지 않았다면 (!hasSelection, selectedIndex가 -1일 때))
                val visible = (selectedIndex == idx || (idx == todayIndex && selectedIndex == -1))
                    && getBubbleCount(idx) > 0

                if (visible) {
                    SpeechBubble(count = getBubbleCount(idx))
                }
            }
        }
    }
}

// 선택한 날짜별 개수 물방울 표시
@Composable
private fun SpeechBubble(
    count: Int
) {
    Box(
        modifier = Modifier.wrapContentWidth(unbounded = true),
        contentAlignment = Alignment.TopCenter
    ) {
        // 몸체 (알약 모양 배지)
        Box(
            modifier = Modifier
                .height(30.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(percent = 100),
                    clip = false
                )
                .background(Color.White, shape = RoundedCornerShape(percent = 100)),
            contentAlignment = Alignment.Center
        ) {
            // 꼬리 (위쪽 삼각형) : 몸체와 겹칠 때 꼬리가 위 레이어로 가도록 배치
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7.5).dp)                // 위로 겹치기
                    .zIndex(1f)                         // 몸체보다 위에
                    .size(width = 12.dp, height = 8.dp)
                    .clip(
                        GenericShape { size, _ ->
                            moveTo(0f, size.height)
                            lineTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            close()
                            RoundedCornerShape(5.dp)
                        }
                    )
                    .background(Color.White)
            )

            // 내용 (물방울 + n개)
            Row(
                modifier = Modifier.padding(end = 6.dp, start = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "💧", fontSize = 14.sp)
                Text(
                    text = "${count}개",
                    fontSize = 16.sp,
                    style = AppTextStyles.b3_medium_14,
                    color = Grey1000
                )
            }
        }
    }
}

//================= 하단 성장률 표시 카드 와 누베리 개수 원형 카드 =================

@Composable
private fun BottomProgressCard(
    percent: Int,
    title: String,
    progress: Float,
    topBadgeCount: Int,
    showLevelUp: Boolean,
    showBerry: Boolean,
    currentStep: Int,
    currentProgressFromServer: Float,
    cardMinHeight: Dp = 130.dp,
    levelUpText: String,
    onLevelUpAnimationDone: () -> Unit,
    onBerryAnimationDone: () -> Unit
) {

    // 현재 단계 값을 받아서 다음 단계로
    var state by rememberSaveable { mutableStateOf("normal") }
    val nextStep = (currentStep + 1).coerceAtMost(5)

    // '수확' 이벤트 정의: 'showBerry'가 true이고 '그 결과' 최신 스텝(currentStep)이 0인 경우
    val isHarvestEvent = (showBerry && currentStep == 0)


    // 애니메이션 전환 (기존 UI → levelup → next)
    LaunchedEffect(showLevelUp) {
        if (showLevelUp) {
            delay(300)        // 기존 UI 잠깐 보여주기
            state = "levelup" // 레벨업 화면으로 전환
            delay(LEVEL_HOLD_MS.toLong()) //레벨업 화면 애니메이션 처리 후
            state = "next"    // 다음 단계 화면으로 전환
            // 이벤트가 소비되었음을 ViewModel에 알림
            onLevelUpAnimationDone()

        }
        else if (showBerry) {
            // 레벨업이 아닐 때만 베리 획득 화면 표시
            state = "berry" // 베리 획득 화면으로 전환
            delay(LEVEL_HOLD_MS.toLong()) // 레벨업과 동일한 시간 유지
            state = "normal" // 베리 획득 후에는 '다음' 상태 없이 바로 'normal'로 복귀
            onBerryAnimationDone() // 이벤트 소비 알림

        }
        else if (state != "normal") {
            // showLevelUp이 false가 되면(이벤트가 소비되면)
            // state를 다시 "normal"로 리셋
            state = "normal"
        }
    }
    // 하단 성장률 카드 Box
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // 그림자 박스는 카드 자체만 감싼다 (배지는 별도)
        CustomShadowBox(
            cornerRadius = 18.dp,
            shadowColor = Color.Black,
            shadowAlpha = 0.2f,
            shadowBlurRadius = 8.dp,
            offsetX = 6.dp,
            offsetY = 6.dp
        ) {
            // 카드 본체 : 고정/최소 높이를 줘서 상태 변경 시에도 외곽 크기가 유지되도록
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = cardMinHeight)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.80f))
            ) {

                // 내부 패딩은 콘텐츠에만 적용, 내용물 전환 애니메이션
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (
                            (slideInVertically(animationSpec = tween(ENTER_MS)) { it } + fadeIn()) togetherWith
                                (slideOutVertically(animationSpec = tween(EXIT_MS)) { -it } + fadeOut())
                            ).using(
                                SizeTransform(clip = false) { _, _ -> tween(0) } // 사이즈 애니메이션 제거
                            )
                    },
                    label = "BottomProgressContentAnim",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.dp, vertical = 25.dp)
                ) { current ->
                    when (current) {
                        "levelup" -> {
                            // 레벨업 화면 + 바 이동 + 체크 효과 + 끝에 열매
                            LevelUpSection(
                                totalSteps = 6,
                                prevStep = currentStep,   // 레벨업 직전 단계
                                nextStep = nextStep,      // 레벨업 후 단계
                                levelUpText = levelUpText,
                                onStepAnimDone = {
                                    // 바 이동 및 체크 애니메이션이 끝났을 때 추가 동작이 있으면 여기에
                                    // (상위 LaunchedEffect에서 LEVEL_HOLD_MS 후 state = "next"로 넘어감)
                                }
                            )
                        }
                        "berry" -> {
                            val berryPrevStep = 5
                            val berryNextStep = 6

                            // 누베리 획득 화면 (StepBar 애니메이션 포함)
                            NuberrySection(
                                totalSteps = 6,
                                prevStep = berryPrevStep, // 4
                                nextStep = berryNextStep, // 5 (or 4)
                                levelUpText = "멋진 수확이에요. 다음 성장도 함께 가요.",  //
                                onStepAnimDone = { /* no-op */ }
                            )
                        }
                        "next" -> {
                            // 다음 성장률 UI
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$percent",
                                        style = AppTextStyles.learn_percentage_46.copy(
                                            brush = Brush.linearGradient(
                                                listOf(Color(0xFF8380FF), PurpleMain500)
                                            )
                                        )
                                    )
                                    Text(
                                        text = "%",
                                        style = AppTextStyles.learn_percentage_30,
                                        color = Color(0xFF8380FF),
                                        modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)
                                    )
                                }
                                Text(
                                    text = "title",
                                    style = AppTextStyles.b2_semibold_16,
                                    color = Grey1000
                                )
                                Spacer(Modifier.height(16.dp))
                                AnimatedProgressBar(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        else -> {
                            // 기본 화면 (기존 UI)
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$percent",
                                        style = AppTextStyles.learn_percentage_46.copy(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF8380FF), PurpleMain500)
                                            )
                                        ),
                                    )
                                    Text(
                                        text = "%",
                                        style = AppTextStyles.learn_percentage_30,
                                        color = Color(0xFF8380FF),
                                        modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)
                                    )
                                }
                                Text(
                                    text = title,
                                    style = AppTextStyles.b2_semibold_16,
                                    color = Grey1000
                                )
                                Spacer(Modifier.height(16.dp))
                                AnimatedProgressBar(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        // 누베리 개수 원형 카드
        // 배지는 카드 콘텐츠와 독립적으로 Box(부모) 기준에 고정
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd) // 부모(Box) 기준
                .offset(y = (-45).dp)    // 스크린샷과 같은 시각적 위치
                .shadow(6.dp, RoundedCornerShape(999.dp), clip = true)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.80f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🫐", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${topBadgeCount}개",
                style = AppTextStyles.b2_medium_16,
                color = Grey1000
            )
        }
    }
}

//하단 성장률 표시 카드 형태 + 그림자
@Composable
fun CustomShadowBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    shadowColor: Color = Color.Black, // 그림자 색
    shadowAlpha: Float = 0.2f,        // 그림자 투명도
    shadowBlurRadius: Dp = 6.dp,     // 그림자 번짐 정도
    offsetX: Dp = 6.dp,               // 오른쪽 오프셋
    offsetY: Dp = 6.dp,               // 아래 오프셋
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            // 그림자가 박스 밖으로 나가도록 레이어 분리(클리핑 방지)
            .graphicsLayer { /* no-op, 레이어 강제 생성 */ }
            .drawBehind {
                // Android 네이티브 캔버스/페인트 사용
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(
                            shadowBlurRadius.toPx(),         // 번짐
                            offsetX.toPx(),                  // X 오프셋(오른쪽)
                            offsetY.toPx(),                  // Y 오프셋(아래)
                            shadowColor.copy(alpha = shadowAlpha).toArgb()
                        )
                    }
                    val rect = android.graphics.RectF(
                        0f, 0f, size.width, size.height
                    )
                    // 네이티브 캔버스로 라운드 사각형 "그림자"만 그림
                    canvas.nativeCanvas.drawRoundRect(
                        rect,
                        cornerRadius.toPx(),
                        cornerRadius.toPx(),
                        paint
                    )
                }
            }
    ) {
        content()
    }
}

//하단 성장률 카드 기본 UI 그래프
@Composable
fun AnimatedProgressBar(
    progress: Float, // 최종 목표 progress (0f ~ 1f)
    modifier: Modifier = Modifier
) {
    // 최초 진입 시 0f에서 시작하여 목표 progress 로 애니메이션
    val anim = remember { Animatable(0f) }

    // 화면 입장/또는 progress 변경 시 목표값으로 부드럽게 이동
    LaunchedEffect(progress) {
        anim.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 700)
        )
    }

    // UI
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp) // 원하는 높이
            .clip(RoundedCornerShape(999.dp))
            .background(GreyMain300) // 배경 (연한 회색/투명)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(anim.value) // 애니메이션된 값 사용
                .clip(RoundedCornerShape(999.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF90D0FF), // 시작 색
                            Color(0xFF5955FF)  // 끝 색
                        )
                    )
                )
        )
    }
}

// 스테이지 번호에 맞는 이름 반환 함수
private fun getStageName(stage: Int): String {
    return when (stage) {
        0 -> "작은 새싹"
        1 -> "묘목"
        2 -> "꽃봉오리"
        3 -> "꽃"
        4 -> "열매"
        5 -> "누베리 수확"
        else -> ""
    }
}

// ==== 단계 → 퍼센트 보조 함수 ====
private fun stepToFraction(step: Int, total: Int): Float {
    if (total <= 1) return 1f
    val s = step.coerceIn(0, total - 1)
    return s.toFloat() / (total - 1).toFloat()
}

@Composable
private fun LevelUpSection(
    totalSteps: Int,
    prevStep: Int,
    nextStep: Int,
    nextPercentFromServer: Float? = null, // 예: 0.50f (최종은 체크 지점으로 스냅)
    onStepAnimDone: () -> Unit,
    levelUpText: String,
) {
    // 1) 시작/목표 퍼센트 (목표는 체크 지점으로 스냅)
    val fromFrac = stepToFraction(prevStep - 1, totalSteps).coerceIn(0f, 1f)
    val toFrac = stepToFraction(nextStep - 1, totalSteps)

    // 2) 진행도는 단순 Float 상태로 관리 (프레임마다 값 갱신)
    var barProgress by remember { mutableFloatStateOf(fromFrac) }

    // 3) 체크 개수/튀는 효과
    var checkedCount by remember { mutableStateOf(prevStep) }
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
            text = levelUpText,
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

@Composable
private fun NuberrySection(
    totalSteps: Int,
    prevStep: Int,
    nextStep: Int,
    nextPercentFromServer: Float? = null, // 예: 0.50f (최종은 체크 지점으로 스냅)
    onStepAnimDone: () -> Unit,
    levelUpText: String,
) {
    // 1) 시작/목표 퍼센트 (목표는 체크 지점으로 스냅)
    val fromFrac = stepToFraction(prevStep - 1, totalSteps).coerceIn(0f, 1f)
    val toFrac = stepToFraction(nextStep - 1, totalSteps)

    // 2) 진행도는 단순 Float 상태로 관리 (프레임마다 값 갱신)
    var barProgress by remember { mutableFloatStateOf(fromFrac) }

    // 3) 체크 개수/튀는 효과
    var checkedCount by remember { mutableStateOf(prevStep) }
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

    // ---- UI ----
    Column {
        Text(
            text = "Nuberry Get!",
            style = AppTextStyles.learn_percentage_46.copy(
                brush = Brush.linearGradient(listOf(Color(0xFF8380FF), PurpleMain500))
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = levelUpText,
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

// ==== 스텝 바 ====
@Composable
private fun StepBar(
    total: Int,
    progress: Float,             // 0f~1f (바 채움 비율, 체크 지점으로 스냅된 값 전달)
    checkedCount: Int,           // 현재 표시할 체크 개수
    showBounceOnLast: Boolean,   // 마지막 체크 튀는 효과
    lastCheckScale: Float,       // 마지막 체크 스케일 값
    modifier: Modifier = Modifier,
    barHeight: Dp = 16.dp,       // 기본 UI 그래프 두께와 동일
    circleSize: Dp = 32.dp,      // 체크 원 크기
    berrySize: Dp = 40.dp,       // 베리 아이콘 크기
    berryOffsetUp: Dp = 16.dp,    // 마지막 동그라미 위로 8dp
    trackColor: Color = Grey50 // 기본 그래프 트랙 색과 동일 계열
) {
    val gradient = remember {
        // 기본 그래프와 동일한 그라디언트
        Brush.horizontalGradient(listOf(Color(0xFF6863FF), PurpleMain500))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = circleSize / 2)                 // 끝 원이 잘리지 않게
            .height(max(circleSize, barHeight) + 10.dp)       // 전체 높이 확보
    ) {
        val stepFrac = if (total > 1) 1f / (total - 1) else 1f

        // (1) 트랙
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
                .zIndex(0f)
        )

        // (2) 채워진 바
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
                    brush = gradient,
                    size = Size(filledW, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }

        // (3) 체크 원들 — 바 위에 정확히 겹치기
        repeat(total) { i ->
            val x = maxWidth * (i * stepFrac)
            val isChecked = (i + 1) <= checkedCount
            val isLastChecked = isChecked && (i + 1) == checkedCount && showBounceOnLast

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = x - circleSize / 2)
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(if (isChecked) PurpleMain500 else Grey50)
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(circleSize * 0.6f)
                            .then(
                                if (isLastChecked) Modifier.graphicsLayer {
                                    scaleX = lastCheckScale
                                    scaleY = lastCheckScale
                                } else Modifier
                            )
                    )
                }
            }
        }

        // (4) 베리 — 맨 마지막 동그라미 위
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
            Text("🫐")
        }
    }
}
