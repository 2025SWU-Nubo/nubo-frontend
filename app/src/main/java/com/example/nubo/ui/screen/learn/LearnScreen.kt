package com.example.nubo.ui.screen.learn

import android.widget.Toast
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastOverlay
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.Purple100


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
    // 인포 팝업 노출 상태
    var showInfoPopup by rememberSaveable { mutableStateOf(false) }

    // 화면 첫 진입 시 1회만 자동 표시
    LaunchedEffect(Unit) {
        if (!viewModel.hasSeenInfoPopup()) {
            showInfoPopup = true
            viewModel.markInfoPopupSeen()
        }
    }

    // 커스텀 토스트 호스트 상태 생성
    val toastHostState = rememberAppToastHostState()

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
                val context = LocalContext.current

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
                        showBerry = berryGained,
                        currentStep = dashboardData.stage, // 현재 stage 전달
                        nextStageRemaining = nextStageRemaining, // 남은 카드 개수 전달
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
                        },
                        onClickBerryBadge = {
                            // 베리 배지 클릭 시 모은 베리 화면으로 이동하면서 개수 전달
                            navController.navigate("learnBerry/$berryCount")
                        }
                    )
                    Spacer(Modifier.height(130.dp))
                }
            }
        }
        // 누베리 획득 팝업
        if (berryGained) {
            NuberryPopup(
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
        // 커스텀 토스트 오버레이 배치
        // 화면 최상단 레이어에 위치
        AppToastOverlay(
            hostState = toastHostState,
            extraBottomOffset = 100.dp // 하단 탭바 높이 등을 고려하여 위치 조정
        )
    }
}

//================= 상단 탑바 (타이틀 + 차트 버튼) =================

@Composable
private fun TopBar(
    title: String,
    onClickInfo: () -> Unit
) {
    // 상단 바: 타이틀 중앙 + 우측 아이콘 버튼
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

        // 차트 버튼
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
                // 물방울 PNG 아이콘
                Image(
                    painter = painterResource(id = R.drawable.learn_waterdrop_calender),
                    contentDescription = null, // decorative image
                    modifier = Modifier
                        .size(24.dp) // icon size
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
    showBerry: Boolean,
    currentStep: Int,
    nextStageRemaining: Int,
    currentProgressFromServer: Float,
    cardMinHeight: Dp = 50.dp,
    levelUpText: String,
    onLevelUpAnimationDone: () -> Unit,
    onBerryAnimationDone: () -> Unit,
    onClickBerryBadge: () -> Unit
) {

    // 현재 단계 값을 받아서 다음 단계로
    var state by rememberSaveable { mutableStateOf("normal") }
    val nextStep = (currentStep + 1).coerceAtMost(5)

    // 카드 앞/뒤 상태와 높이/회전 애니메이션
    var isFlipped by rememberSaveable { mutableStateOf(false) }

    // 회전 값 애니메이션
    val rotationYAnim by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 1300),
        label = "cardFlip"
    )

    // 애니메이션 전환 (기존 UI → levelup → next)
    LaunchedEffect(showLevelUp) {
        if (showLevelUp) {
            delay(600)        // 기존 UI 잠깐 보여주기
            state = "levelup" // 레벨업 화면으로 전환
            delay(LEVEL_HOLD_MS.toLong()) //레벨업 화면 애니메이션 처리 후
            state = "next"    // 다음 단계 화면으로 전환
            // 이벤트가 소비되었음을 ViewModel에 알림
            onLevelUpAnimationDone()

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
            // 카드와 그림자가 함께 회전하도록 상위 레이어에 회전 적용
            modifier = Modifier.graphicsLayer {
                rotationY = rotationYAnim
                cameraDistance = 12 * density
            },
            cornerRadius = 11.dp,
            shadowColor = Color.Black,
            shadowAlpha = 0.2f,
            shadowBlurRadius = 8.dp,
            offsetX = 6.dp,
            offsetY = 6.dp
        ) {
            // 카드 본체에 회전/클릭 추가
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = cardMinHeight)          // 기존 cardMinHeight 대신 animatedHeight 사용
                    .graphicsLayer {
                        // 카드에 적용할 실제 회전 값
                        rotationY = rotationYAnim
                        cameraDistance = 12 * density
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.80f))
                    .noRippleClickable {
                        // 레벨업/베리 화면이 아닐 때만 카드 뒤집기 허용
                        if (state == "normal" || state == "next") {
                            isFlipped = !isFlipped
                        }
                    }
            ) {
                // 앞면/뒷면 선택 기준
                val showFront = rotationYAnim <= 90f

                if (showFront) {
                    // 기존 AnimatedContent 는 그대로 유지
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
                            .padding(horizontal = 28.dp, vertical = 16.dp)
                    ) { current ->
                    when (current) {
                        "levelup" -> {
                            // 레벨업 화면
                            LevelUpSection(
                                totalSteps = 5,
                                prevStep = currentStep,   // 레벨업 직전 단계
                                nextStep = nextStep,      // 레벨업 후 단계
                                levelUpText = levelUpText,
                                onStepAnimDone = {
                                    // 바 이동 및 체크 애니메이션이 끝났을 때 추가 동작이 있으면 여기에
                                    // (상위 LaunchedEffect에서 LEVEL_HOLD_MS 후 state = "next"로 넘어감)
                                }
                            )
                        }
                        "next" -> {
                            // 다음 성장률 UI
                            GrowthFrontContent(
                                title = title,
                                percent = percent,
                                progress = progress
                            )
                        }
                        else -> {
                            // 기본 화면
                            GrowthFrontContent(
                                title = title,
                                percent = percent,
                                progress = progress
                            )
                        }
                    }
                    }
                } else {
                    // 뒤집힌 면 UI
                    Column(
                        modifier = Modifier
                           /* .graphicsLayer { rotationY = 180f } // 텍스트 반전 방지*/
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BackNextLevelSection(
                            currentStage = currentStep, // 현재 단계 전달
                            remainingCards = nextStageRemaining
                        )
                    }
                }
            }
        }
        // 누베리 개수 원형 카드
        // 배지는 카드 콘텐츠와 독립적으로 Box(부모) 기준에 고정
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd) // 부모(Box) 기준
                .offset(y = (-52).dp)    // 스크린샷과 같은 시각적 위치
                .shadow(6.dp, RoundedCornerShape(999.dp), clip = true)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.80f))
                .noRippleClickable {
                    // 베리 배지 클릭 시 콜백 호출
                    onClickBerryBadge()
                }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.learn_nuberry_total),
                contentDescription = null, // decorative image
                modifier = Modifier
                    .size(28.dp) // icon size
            )
            Spacer(Modifier.width(3.dp))
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

//------------ 하단 성장률 카드 기본 UI ------------------
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

//하단 성장률 카드 기본 UI 프로그레스 바
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
            .height(10.dp) // 원하는 높이
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

// 뒤집힌 면
@Composable
private fun BackNextLevelSection(
    currentStage: Int,
    remainingCards: Int
) {
    // 가운데 숫자만 보라색으로 스타일 적용
    val text = buildAnnotatedString {
        append("다음 레벨까지 ")
        withStyle(SpanStyle(color = PurpleMain500)) {
            append(remainingCards.toString())
        }
        append("장의 카드가 남았어요.")
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
            listOf(Color(0xFF7272FF), PurpleMain500)
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
    val clampedStep = currentStep.coerceIn(0, totalSteps - 1)

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
                //   - 그 사이 인덱스: 위 두 색 사이에서 선형 보간
                val activeCircleColor: Color = if (!isActive) {
                    Color.Unspecified // 사용 안 함
                } else {
                    if (clampedStep == 0) {
                        // 단계가 1개만 활성인 경우 바로 메인 퍼플
                        PurpleMain500
                    } else {
                        val t = index.toFloat() / clampedStep.toFloat() // 0f ~ 1f
                        val start = Color(0xFF7272FF)   // 새싹 단계용 밝은 보라
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
                        color = if (isActive) Color.White else Grey1000
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
//------------ 하단 성장률 카드 기본 UI 끝 ------------------

// 스테이지 번호에 맞는 이름 반환 함수
private fun getStageName(stage: Int): String {
    return when (stage) {
        1 -> "작은 새싹"
        2 -> "묘목"
        3 -> "꽃봉오리"
        4 -> "꽃"
        5 -> "열매"
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

// ==== 스텝 바 ====
@Composable
private fun StepBar(
    total: Int,
    progress: Float,             // 0f~1f (바 채움 비율, 체크 지점으로 스냅된 값 전달)
    checkedCount: Int,           // 현재 활성 단계(1부터 시작)
    showBounceOnLast: Boolean,   // 마지막 동그라미 튀는 효과 여부
    lastCheckScale: Float,       // 마지막 동그라미 스케일 값
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp,
    circleSize: Dp = 24.dp,    // 단계 동그라미 크기
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
                    color = if (isActive) Color.White else Grey1000
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

// 누베리 획득 시 팝업
@Composable
fun NuberryPopup(
    onDismiss: () -> Unit,
    onClickBerryPage: () -> Unit
) {
    // 팝업 표시 애니메이션 (베리 살짝 커졌다 작아지는 효과)
    val scaleAnim = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            1.2f,
            animationSpec = tween(400, easing = LinearEasing)
        )
        scaleAnim.animateTo(
            1f,
            animationSpec = tween(300, easing = LinearEasing)
        )
    }

    // 반투명 배경 (뒤 비활성화)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .noRippleClickable { /* 배경 클릭 막기 */ },
        contentAlignment = Alignment.Center
    ) {
        // 팝업 카드
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Purple100)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 닫기 버튼 (좌측 상단)
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .noRippleClickable { onDismiss() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close), // 닫기 아이콘
                        contentDescription = "닫기",
                        tint = Grey1000,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // 중앙 원 + 베리 이미지
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.learn_nuberry_get),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 텍스트 영역
                Text(
                    text = "Nuberry Get!",
                    style = AppTextStyles.learn_percentage_30.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFF8380FF), PurpleMain500))
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "멋진 수확이에요.\n다음 성장도 함께 가요.",
                    style = AppTextStyles.b2_medium_16,
                    color = Grey1000,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(36.dp))

                // 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(41.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurpleMain500)
                        .noRippleClickable { onClickBerryPage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "모은 베리 확인하러 가기",
                        style = AppTextStyles.label_semibold_14,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
