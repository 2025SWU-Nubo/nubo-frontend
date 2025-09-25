package com.example.nubo.ui.screen.learn

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
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
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey0
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import java.time.LocalDate
import java.time.temporal.WeekFields
import androidx.compose.animation.core.Animatable


@Composable
fun LearnScreen(
    modifier: Modifier = Modifier
) {
    // 한 주 날짜/요일 계산 ------------------------------------------------------
    // 오늘 날짜
    val today = remember { LocalDate.now() }
    // 한국/일요일 시작 기준으로 주간 계산
    val weekFields = remember { WeekFields.SUNDAY_START }
    val firstDayOfWeek = today.with(weekFields.dayOfWeek(), 1) // 일요일
    val daysOfWeek = remember { listOf("일", "월", "화", "수", "목", "금", "토") }

    // 날짜 리스트(일~토)
    val weekDates = remember(firstDayOfWeek) {
        (0..6).map { firstDayOfWeek.plusDays(it.toLong()) }
    }

    // 오늘이 주간 리스트 안에서 몇 번째인지(요일 텍스트 보라색 표시용)
    val todayIndex = remember(weekDates) {
        weekDates.indexOfFirst { it.isEqual(today) }.coerceAtLeast(0)
    }

    // 날짜 선택 상태 -----------------------------------------------------------
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // 임의 데이터: 날짜별 말풍선 카운트(서버 연동 전)
    val dummyBubbleCountPerDay = remember {
        // 0이면 말풍선 안보이도록
        listOf(2, 0, 0, 1, 0, 0, 0)
    }

    // 하단 카드 임의 데이터 -----------------------------------------------------
    val progress = 0.25f            // 25%
    val progressTitle = "누베리 성장률"
    val topBadgeCount = 5           // 오른쪽 위 둥근 배지 “5개”

    // 화면 ---------------------------------------------------------------------
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF92C8EF)) // 배경 느낌만 잡는 임시색
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1) 상단 타이틀 + 차트 아이콘 -----------------------------------
            TopBar(
                title = "대시보드",
                onClickChart = { /* TODO: 차트 화면 이동 콜백 연결 */ }
            )

            Spacer(Modifier.height(12.dp))

            // 2) 주간 캘린더 ---------------------------------------------------
            WeeklyCalendar(
                daysLabel = daysOfWeek,
                dates = weekDates,
                todayIndex = todayIndex,
                selectedIndex = selectedIndex,
                onClickDay = { idx ->
                    // 같은 날짜를 다시 클릭하면 해제, 다른 날짜 클릭 시 해당 날짜로 교체
                    selectedIndex = if (selectedIndex == idx) null else idx
                },
                getBubbleCount = { idx -> dummyBubbleCountPerDay.getOrNull(idx) ?: 0 },
                bubbleImageRes = R.drawable.learn_day_waterdrop, // 나중에 R.drawable.speech_bubble 로 교체
                circleSize = 36.dp
            )

            Spacer(Modifier.weight(1f))

            // 3) 하단 반투명 박스 + 우상단 둥근 배지 ---------------------------
            BottomProgressCard(
                percent = (progress * 100).toInt(),
                title = progressTitle,
                progress = progress,
                topBadgeCount = topBadgeCount
            )

            Spacer(Modifier.height(140.dp))
        }
    }
}

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
            .padding(horizontal = 8.dp, vertical = 15.dp), // 좌우 여백 조정 가능
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
                .size(38.dp) // 동그라미 크기
                .clip(CircleShape)
                .clickable(onClick = onClickChart), // 클릭 가능
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = "통계",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp) // 아이콘 크기
            )
        }
    }
}


@Composable
private fun WeeklyCalendar(
    daysLabel: List<String>,
    dates: List<LocalDate>,
    todayIndex: Int,
    selectedIndex: Int?,
    onClickDay: (Int) -> Unit,
    getBubbleCount: (Int) -> Int,
    bubbleImageRes: Int?,           // 말풍선 drawable 리소스 (null이면 색 박스)
    circleSize: Dp
) {
    // 요일 라벨 줄 --------------------------------------------------------------
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysLabel.forEachIndexed { idx, day ->
            val isToday = idx == todayIndex
            Text(
                text = day,
                style = AppTextStyles.subtitle_semibold_20,
                color = if (isToday) PurpleMain500 else Grey0,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    // 날짜 숫자 버튼 줄 ---------------------------------------------------------
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dates.forEachIndexed { idx, date ->
            val isSelected = selectedIndex == idx
            val isToday = idx == todayIndex

            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {

                // 선택했거나 오늘인 경우: 뒤에 하얀 동그라미
                if (isSelected || isToday) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }

                // 날짜 숫자(클릭 영역)
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .clickable { onClickDay(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = AppTextStyles.subtitle_semibold_20,
                        color = if (isSelected||isToday) Grey1000 else Grey0
                    )
                }
            }
        }
    }

    // 선택한 날짜 하단 말풍선 줄 ------------------------------------------------
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dates.forEachIndexed { idx, _ ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp), // 말풍선 높이 공간 확보
                contentAlignment = Alignment.TopCenter
            ) {
                val visible = selectedIndex == idx && getBubbleCount(idx) > 0
                if (visible) {
                    SpeechBubble(
                        count = getBubbleCount(idx)
                    )
                }
            }
        }
    }
}

// 날짜별 개수
@Composable
private fun SpeechBubble(
    count: Int
) {
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // 몸체 (알약 모양 배지)  ── clip 제거!
        Box(
            modifier = Modifier
                .width(63.dp)
                .height(30.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(percent = 100),
                    clip = false
                )
                // 둥근 모양은 background의 shape로 처리 (clip 안 씀 → 꼬리 안 잘림)
                .background(Color.White, shape = RoundedCornerShape(percent = 100)),
            contentAlignment = Alignment.Center
        ) {
            // ── 꼬리 (위쪽 삼각형) : 몸체 안에서 위로 겹치기 + 항상 위 레이어
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7).dp)                // 위로 겹치기
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "💧", fontSize = 14.sp)
                Spacer(Modifier.width(2.dp))
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

@Composable
private fun BottomProgressCard(
    percent: Int,
    title: String,
    progress: Float,
    topBadgeCount: Int
) {

    // 하단 큰 박스(성장률 숫자 + 그래프)
    CustomShadowBox(
        cornerRadius = 18.dp,
        shadowColor = Color.Black,
        shadowAlpha = 0.2f,
        shadowBlurRadius = 8.dp,
        offsetX = 6.dp,   // 오른쪽으로
        offsetY = 6.dp    // 아래로
    ) {
        // 반투명 박스 (성장률 표시)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.80f))
                .padding(horizontal = 25.dp, vertical = 25.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$percent",
                    style = AppTextStyles.learn_percentage_54.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8380FF),
                                PurpleMain500
                            )
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

        // 우상단 둥근 배지
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = (-53).dp) // 카드 위로 띄움
                .shadow(6.dp, RoundedCornerShape(999.dp), clip = true) // 배지 그림자
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
            .background(GreyMain300.copy(alpha = 0.3f)) // 배경 (연한 회색/투명)
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
