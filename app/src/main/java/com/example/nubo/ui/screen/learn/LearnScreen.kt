package com.example.nubo.ui.screen.learn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState

// BottomProgressCard 애니메이션 시간 조절
// 전역에서 쓸 상수 (밀리초)
private const val ENTER_MS = 800     // 카드 안 콘텐츠가 위로 올라오는 시간
private const val EXIT_MS = 700      // 내려가는 시간
private const val LEVEL_HOLD_MS = 3500 // 레벨업 화면 유지 시간

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = hiltViewModel(),
    navController: NavController
) {
    // 인포 팝업 노출 상태
    var showInfoPopup by rememberSaveable { mutableStateOf(false) }

    // 화면 첫 진입 시 1회만 자동 표시
    LaunchedEffect(Unit) {
        if (!viewModel.hasSeenInfoPopup()) {
            showInfoPopup = true
            viewModel.markInfoPopupSeen()
        }
    }

    // 전역 토스트 호스트 상태 생성
    val toastHostState = LocalAppToastHostState.current

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

                // 1. 배경 이미지 (Text 제거 후 배경만 유지)
                GraphicBackgroundView(
                    modifier = Modifier.fillMaxSize(),
                    todayVideoCount = 3, // 에러 시 보여줄 기본 값 (필요에 따라 수정)
                    level = 3
                )
                // 화면 진입 시 커스텀 토스트 호출 (1회성)
                LaunchedEffect(Unit) {
                    toastHostState.show(
                        title = AnnotatedString("정보를 불러오지 못했어요"),
                        summary = "네트워크 확인 후 다시 시도해주세요.", // 두 번째 줄은 summary로 처리
                        type = AppToastType.NEGATIVE,         // 에러 아이콘 타입
                        layout = AppToastLayout.TitleWithSummary // 제목 + 설명 레이아웃
                    )
                }
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
                val nextStageRemaining = dashboardData.nextStageRemaining


                // 오늘 학습 카운트 계산 (물방울 개수 적용)
                val todayCount = bubbleCounts.getOrNull(todayIndex) ?: 0

                // 레벨에 맞춰 그래픽 띄우기
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
                        title = "성장보드",
                        onClickInfo = { showInfoPopup = true }
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
                        currentStep = dashboardData.stage, // 현재 stage 전달
                        nextStageRemaining = nextStageRemaining, // 남은 카드 개수 전달
                        // 애니메이션이 끝나면 ViewModel에 알려줄 콜백 전달
                        onLevelUpAnimationDone = {
                            viewModel.onLevelUpAnimationFinished()
                        },
                        onClickBerryBadge = {
                            // 베리 배지 클릭 시 모은 베리 화면으로 이동하면서 개수 전달
                            navController.navigate("learnBerry/$berryCount")
                        }
                    )
                    Spacer(Modifier.height(140.dp))
                }
            }
        }
        // 누베리 획득 팝업
        if (berryGained) {
            NuberryGet(
                onDismiss = { viewModel.onBerryAnimationFinished() },
                onClickBerryPage = {
                    // uiState가 Success 상태인지 확인하고 berryCount를 가져옴
                    // 만약 로딩/에러 상태라면 안전하게 0으로 처리
                    val currentCount = (uiState as? DashboardUiState.Success)?.data?.berryCount ?: 0

                    // 가져온 개수를 경로에 포함시켜 이동
                    navController.navigate("learnBerry/$currentCount")
                }
            )
        }
        // 인포 팝업
        if (showInfoPopup) {
            LearnScreenInformation(
                onClose = { showInfoPopup = false } // close on X or "시작하기"
            )
        }
    }
}

// 상단 탑바 (타이틀 + 인포 버튼)
@Composable
private fun TopBar(
    title: String,
    onClickInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 20.dp),
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
        // 인포 버튼
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .noRippleClickable { onClickInfo() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.learn_ic_info),
                contentDescription = "정보",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

//================= 상단 캘린더 영역 =================
// 캘린더 영역 (요일 + 날짜)
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

// 선택한 날짜별 카드 카운트 물방울 표시
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
                modifier = Modifier.padding(end = 11.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 물방울 PNG 아이콘
                Image(
                    painter = painterResource(id = R.drawable.learn_waterdrop_calender),
                    contentDescription = null, // decorative image
                    modifier = Modifier
                        .size(20.dp) // icon size
                )
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
    currentStep: Int,
    nextStageRemaining: Int,
    cardMinHeight: Dp = 50.dp,
    onLevelUpAnimationDone: () -> Unit,
    onClickBerryBadge: () -> Unit
) {

    // current state of level animation (normal / levelup / next)
    var state by rememberSaveable { mutableStateOf("normal") }
    val nextStep = (currentStep).coerceAtMost(5)

    // front / back content toggle
    var showBack by rememberSaveable { mutableStateOf(false) }

    // base badge offset
    val baseBadgeOffsetY = (-45).dp

    // simple fixed offset
    val badgeOffsetY by remember {
        mutableStateOf(baseBadgeOffsetY)
    }

    // 애니메이션 전환 (기존 UI → levelup → next)
    LaunchedEffect(showLevelUp) {
        if (showLevelUp) {
            delay(600)        // 기존 UI 잠깐 보여주기
            state = "levelup" // 레벨업 화면으로 전환
            delay(LEVEL_HOLD_MS.toLong()) //레벨업 화면 애니메이션 처리 후
            state = "next"    // 다음 단계 화면으로 전환
            // 이벤트가 소비되었음을 ViewModel에 알림
            onLevelUpAnimationDone()

        } else if (state != "normal") {
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
        // 카드 본체에 회전/클릭 추가
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = cardMinHeight)
                .noRippleClickable {
                    // allow toggle only when not in levelup animation
                    if (state == "normal" || state == "next") {
                        showBack = !showBack
                    }
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.80f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            AnimatedContent(
                targetState = if (showBack) "back" else state,
                transitionSpec = {
                    (
                        slideInHorizontally(
                            animationSpec = tween(ENTER_MS)
                        ) { width -> width } + fadeIn()
                        ) togetherWith
                        (
                            slideOutHorizontally(
                                animationSpec = tween(EXIT_MS)
                            ) { width -> -width } + fadeOut()
                            )
                },
                label = "BottomProgressContentAnim",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 17.dp, vertical = 16.dp)
            ) { current ->
                Box(
                    modifier = Modifier.padding(horizontal = 11.dp)
                ) {
                    when (current) {

                        // ------------------- leveup 화면 -------------------
                        "levelup" -> {
                            LevelUpSection(
                                totalSteps = 5,
                                prevStep = currentStep,
                                nextStep = nextStep,
                                onStepAnimDone = { /* 필요 시 처리 */ }
                            )
                        }

                        // ------------------- 다음 단계 안내 화면 -------------------
                        "next" -> {
                            GrowthFrontContent(
                                title = title,
                                percent = percent,
                                progress = progress
                            )
                        }

                        // ------------------- 기본 front 화면 -------------------
                        "normal" -> {
                            GrowthFrontContent(
                                title = title,
                                percent = percent,
                                progress = progress
                            )
                        }

                        // ------------------- 뒷면(back) 화면 -------------------
                        "back" -> {
                            BackNextLevelSection(
                                currentStage = currentStep,
                                remainingCards = nextStageRemaining
                            )
                        }

                        // fallback
                        else -> {
                            GrowthFrontContent(
                                title = title,
                                percent = percent,
                                progress = progress
                            )
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
                .offset(y = badgeOffsetY)
                /* .shadow(6.dp, RoundedCornerShape(999.dp), clip = true)*/
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.80f))
                .noRippleClickable {
                    // 베리 배지 클릭 시 콜백 호출
                    onClickBerryBadge()
                }
                .padding(start = 14.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.learn_nuberry_total),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
            )
            Text(
                text = "${topBadgeCount}개",
                style = AppTextStyles.b2_medium_16,
                color = Grey1000
            )
        }

    }
}

//================= 하단 성장률 카드 기본 UI =================
// next/기본 상태에서 사용하는 앞면 공통 UI
@Composable
private fun GrowthFrontContent(
    title: String,
    percent: Int,
    progress: Float
) {
    Column {
        // 상단 한 줄: 왼쪽 타이틀, 오른쪽 퍼센트
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = AppTextStyles.b2_semibold_16,
                color = Grey1000
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$percent",
                    style = AppTextStyles.b1_semibold_18,
                    color = PurpleMain500
                )
                Text(
                    text = "%",
                    style = AppTextStyles.b1_semibold_18,
                    color = PurpleMain500
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        AnimatedProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 앞면 스텝바 바
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
            .height(10.dp) // desired height
            .clip(RoundedCornerShape(999.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBEBEBE), // top color
                        Color(0xFFC8C8C8)  // bottom color
                    )
                )
            )
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

// next/기본 상태에서 사용하는 뒤집힌 면 공통 UI
@Composable
private fun BackNextLevelSection(
    currentStage: Int,
    remainingCards: Int
) {
    val text = if (currentStage == 5) {
        if (remainingCards > 0) {
            buildAnnotatedString {
                append("누베리 수확까지 ") // '다음 레벨' 대신 '수확'으로 문구 변경
                withStyle(SpanStyle(color = PurpleMain500)) {
                    append(remainingCards.toString())
                }
                append("장의 카드가 남았어요.")
            }
        } else {
            // 남은 카드가 0장일 때
            AnnotatedString("이제 곧 누베리를 수확할 수 있어요!")
        }
    } else {
        // 그외 다른 레벨들
        buildAnnotatedString {
            append("다음 레벨까지 ")
            withStyle(SpanStyle(color = PurpleMain500)) {
                append(remainingCards.toString())
            }
            append("장의 카드가 남았어요.")
        }
    }

    Column {
        // 문장 한 줄
        Text(
            text = text,
            style = AppTextStyles.b2_semibold_16,
            color = Grey1000
        )

        Spacer(Modifier.height(16.dp))

        // 아래 정적 스텝바
        StaticLevelStepBar(
            totalSteps = 5,
            currentStep = currentStage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 뒤집힌 면 스텝 바
@Composable
private fun StaticLevelStepBar(
    totalSteps: Int = 5,
    currentStep: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp,
    circleSize: Dp = 24.dp,
    trackColor: Color = Color(0xFFC8C8C8)
) {
    // 단계 이름 리스트 (마지막 누베리 수확은 제외)
    val stageNames = listOf("새싹", "묘목", "꽃봉오리", "꽃", "열매")

    // 보라색 바 그라디언트 (좌 → 우)
    val barGradient = remember {
        Brush.horizontalGradient(
            listOf(Color(0xFF727EFF), PurpleMain500)
        )
    }

    // 회색 트랙 그라디언트 (위 → 아래)  c8c8c8 + 진한 회색
    val trackGradient = remember {
        Brush.verticalGradient(
            listOf(Color(0xFFB0B0B0), trackColor) // 위는 밝게, 아래는 파라미터 값
        )
    }

    // 비활성 회색 동그라미 그라디언트 (위 → 아래)
    val inactiveCircleGradient = remember {
        Brush.verticalGradient(
            listOf(Color(0xFFB0B0B0), Color(0xFFC8C8C8))
        )
    }

    // 현재 단계 인덱스를 0..totalSteps-1 로 보정
    val clampedStep = (currentStep - 1).coerceIn(0, totalSteps - 1)

    Column(modifier = modifier) {

        // 위쪽 라인 + 동그라미 영역
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = circleSize / 2)
                .height(max(circleSize, barHeight))
        ) {
            val stepFrac = if (totalSteps > 1) 1f / (totalSteps - 1) else 1f

            // 전체 회색 트랙 (세로 그라디언트)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(trackGradient) // ← 단색 대신 그라디언트
            )

            // 채워진 구간 (현재 단계까지) – 보라색 그라디언트
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
                val radius = size.height / 2f
                val progress = if (totalSteps > 1) {
                    clampedStep.toFloat() / (totalSteps - 1).toFloat()
                } else {
                    1f
                }
                val filledW = size.width * progress.coerceIn(0f, 1f)
                if (filledW > 0f) {
                    drawRoundRect(
                        brush = barGradient,
                        size = Size(filledW, size.height),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
            }

            // 각 단계 동그라미와 숫자
            repeat(totalSteps) { index ->
                val x = maxWidth * (index * stepFrac)
                val isActive = index <= clampedStep

                // 활성 동그라미 색 계산
                //   - 0번 인덱스: 가장 밝은 보라 (#7272FF)
                //   - 마지막 활성 인덱스: 메인 퍼플(PurpleMain500)
                val activeCircleColor: Color = if (!isActive) {
                    Color.Unspecified // 사용 안 함
                } else {
                    if (clampedStep == 0) {
                        // 단계가 1개만 활성인 경우 바로 메인 퍼플
                        PurpleMain500
                    } else {
                        val t = index.toFloat() / clampedStep.toFloat() // 0f ~ 1f
                        val start = Color(0xFF727EFF)   // 새싹 단계용 밝은 보라
                        val end = PurpleMain500        // 마지막 단계 메인 퍼플
                        lerp(start, end, t)
                    }
                }

                // 실제로 사용할 브러시/색 결정
                val circleModifier = if (isActive) {
                    Modifier.background(
                        color = activeCircleColor,
                        shape = CircleShape
                    )
                } else {
                    // 비활성 동그라미는 기존처럼 회색 계열
                    Modifier.background(
                        brush = inactiveCircleGradient, // 위에서 만든 회색 그라데이션
                        shape = CircleShape
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = x - circleSize / 2)
                        .size(circleSize)
                        .clip(CircleShape)
                        .then(circleModifier),          // 여기서 색 적용
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = AppTextStyles.b3_medium_14,
                        color = if (isActive) Color.White else Color(0xFFA2A2A2)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 단계 이름 텍스트 줄
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = circleSize / 2)
        ) {
            val stepFrac = if (totalSteps > 1) 1f / (totalSteps - 1) else 1f

            stageNames.forEachIndexed { index, name ->
                val isActive = index <= clampedStep
                val x = maxWidth * (index * stepFrac)

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        // 동그라미와 동일한 x 에 두고, 텍스트 박스 폭만 넓혀서 가운데 정렬
                        .offset(x = x - (circleSize * 1.6f) / 2)
                        .width(circleSize * 1.6f),
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
}
