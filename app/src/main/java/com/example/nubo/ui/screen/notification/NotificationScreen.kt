@file:Suppress("UnusedMaterial3ScaffoldPaddingParameter")
package com.example.nubo.ui.screen.notification

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.nubo.R
import com.example.nubo.ui.theme.*
import com.example.nubo.utils.buildAppNotificationSettingsIntent
import com.example.nubo.utils.rememberNotificationSettingsLauncher
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

// ===== Models =====
enum class NotiSection { Recent, Past }

// ===== Screen =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    state: NotificationFeedState,
    onRefresh: () -> Unit,
    onBack: ()-> Unit,
    onAlarmSetting:()-> Unit,
    onClickItem: (NotificationItem) -> Unit,
    onAcceptInvite: (NotificationItem) -> Unit,
    onRejectInvite: (NotificationItem) -> Unit,
    onShowMore: (NotiSection) -> Unit,
) {

    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- 시스템 앱 알림 허용 여부 (설정 다녀오면 재체크) ---
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val settingsLauncher = rememberNotificationSettingsLauncher(
        onReturn = {
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    )
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // --- 더보기 상태 (초기엔 2개 프리뷰) ---
    var expandedRecentCount by remember(state.recent) { mutableStateOf(minOf(2, state.recent.size)) }
    var expandedPastCount by remember(state.past) { mutableStateOf(minOf(2, state.past.size)) }

    // --- 스크롤 애니메이션을 위한 상태/스코프 ---
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 시스템 네비게이션바 패딩값
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 현재 리스트가 스크롤 가능한 상태인지 계산 (앞/뒤 어느 한쪽이라도 스크롤 가능하면 true)
    val isScrollable by remember {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }

    // 오버레이 푸터가 차지할 예상 높이 (디자인 변경되면 숫자만 조정)
    val footerHeight = 44.dp

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus(force = true)
                        onBack()
                    }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onAlarmSetting()
                    }) {
                        Text(text = "설정", style = AppTextStyles.b2_semibold_16, color = Color.Black)
                    }
                },
                modifier = Modifier.drawBehind {
                    val y = size.height
                    drawLine(
                        color = Grey50,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },

        ) { inner ->

        // ▼ 네비게이션 인셋 계산
        val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // ▼ 리스트 스크롤 가능 여부
        val isScrollable by remember {
            derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
        }

        // ▼ 푸터 실제 높이를 픽셀로 보관 (오버레이일 때 리스트 하단 패딩 확보용)
        val density = LocalDensity.current
        var footerHeightPx by remember { mutableIntStateOf(0) }
        val footerHeightDp = with(density) { footerHeightPx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                // 오버레이가 떠 있을 때는 리스트 하단 패딩을 푸터 높이만큼 늘려 아이템이 가려지지 않게 함
                contentPadding = PaddingValues(
                    bottom = navPadding + 12.dp + if (!isScrollable) footerHeightDp else 0.dp
                )
            ) {
                // ===== 알림 섹션 =====
                item { SectionHeader("알림") }

                // ===== 알림 설정 배너 ====
                if (!notificationsEnabled) {
                    item {
                        NotificationPermissionBanner(
                            onClickEnable = { settingsLauncher.launch(buildAppNotificationSettingsIntent(context)) }
                        )
                    }
                    // 알림 배너와 목록 사이 구분 바
                    item {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Grey10)
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }

                // ===== 최근 알림 리스트 =====
                val visibleRecentItems = state.recent.take(expandedRecentCount)
                itemsIndexed(
                    visibleRecentItems,
                    key = { _, it -> notiStableKey("recent", it) }
                ) { _, item ->
                    val loading = item.notificationId in state.actionLoadingIds
                    NotiCard(
                        item = item,
                        tinted = true,
                        loading = loading,
                        onClick = { onClickItem(item) },
                        onAcceptInvite = { onAcceptInvite(item) },
                        onRejectInvite = { onRejectInvite(item) }
                    )
                }

                // ===== 최근 알림 더보기 버튼 =====
                if (state.recent.size >= 3 && expandedRecentCount < state.recent.size) {
                    item{
                        TextButton(
                            onClick = {
                                val bannerExtra = if (!notificationsEnabled) 2 else 0
                                val firstNewRecentIndex = 1 + bannerExtra + expandedRecentCount

                                expandedRecentCount = state.recent.size
                                onShowMore(NotiSection.Recent)

                                scope.launch {
                                    delay(18)
                                    val layout = listState.layoutInfo
                                    val target = layout.visibleItemsInfo.firstOrNull { it.index == firstNewRecentIndex }

                                    if (target != null) {
                                        val distancePx = (target.offset - layout.viewportStartOffset).toFloat()
                                        listState.animateScrollBy(
                                            value = distancePx,
                                            animationSpec = tween(
                                                durationMillis = 900,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    } else {
                                        listState.animateScrollToItem(firstNewRecentIndex)
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal =  16.dp)
                        ) {
                            Text(
                                text = "${state.recent.size - expandedRecentCount}건 더보기",
                                style = AppTextStyles.b2_semibold_16,
                                color = PurpleMain500
                            )
                        }
                    }
                }

                // ===== 지난 알림 섹션 =====
                item { Spacer(Modifier.height(12.dp)) }

                if (state.past.isNotEmpty()) {
                    item { SectionSub("지난 알림") }
                }

                // ===== 지난 알림 리스트 =====
                val visiblePastItems = state.past.take(expandedPastCount)
                itemsIndexed(
                    visiblePastItems,
                    key = { _, it -> notiStableKey("past", it) }
                ) { _, item ->
                    val loading = item.notificationId in state.actionLoadingIds
                    NotiCard(
                        item = item,
                        tinted = false,
                        loading = loading,
                        onClick = { onClickItem(item) },
                        onAcceptInvite = { onAcceptInvite(item) },
                        onRejectInvite = { onRejectInvite(item) }
                    )
                }

                // ===== 지난 알림 더보기 버튼 =====
                if (state.past.size >= 3 && expandedPastCount < state.past.size) {
                    item {
                        TextButton(
                            onClick = {
                                val bannerExtra = if (!notificationsEnabled) 2 else 0
                                val recentMoreBtnExtra =
                                    if (state.recent.size >= 3 && expandedRecentCount < state.recent.size) 1 else 0
                                val pastSectionStartIndex =
                                    1 + bannerExtra + expandedRecentCount + recentMoreBtnExtra + 1 + 1
                                val firstNewPastIndex = pastSectionStartIndex + expandedPastCount

                                expandedPastCount = state.past.size
                                onShowMore(NotiSection.Past)

                                scope.launch {
                                    delay(18)
                                    val layout = listState.layoutInfo
                                    val target = layout.visibleItemsInfo.firstOrNull { it.index == firstNewPastIndex }

                                    if (target != null) {
                                        val distancePx = (target.offset - layout.viewportStartOffset).toFloat()
                                        listState.animateScrollBy(
                                            value = distancePx,
                                            animationSpec = tween(
                                                durationMillis = 900,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    } else {
                                        listState.animateScrollToItem(firstNewPastIndex)
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${state.past.size - expandedPastCount}건 더보기",
                                style = AppTextStyles.b2_semibold_16,
                                color = PurpleMain500
                            )
                        }
                    }
                }

                // ===== 로딩 인디케이터 =====
                item {
                    if (state.loading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) { CircularProgressIndicator() }
                    }
                }

                // 푸터는 “스크롤 가능할 때만” 리스트 아이템으로 렌더 → 스크롤과 함께 이동
                if (isScrollable) {
                    item {
                        NotificationFooter(
                            label = "7일 전 알림까지 확인할 수 있어요",
                            lineColor = Grey50,
                            labelColor = Grey200,
                        )
                    }
                }
            }

            if (!isScrollable) {
                NotificationFooter(
                    label = "7일 전 알림까지 확인할 수 있어요",
                    lineColor = Grey50,
                    labelColor = Grey200,
                    // 화면 바닥에 붙이기 + 네비게이션 인셋 고려
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navPadding)
                        .onSizeChanged { footerHeightPx = it.height }
                )
            }
        }
    }
}

// ==== 알림 배너 ====
@Composable
private fun NotificationPermissionBanner(
    onClickEnable: () -> Unit,
) {
    Box(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Purple50, shape = RoundedCornerShape(72.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bell_off),
                contentDescription = null,
                tint = GreyMain300,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "휴대폰의 앱 알림이 꺼져있어요.",
                style = AppTextStyles.b2_medium_16,
                color = GreyMain300,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClickEnable, contentPadding = PaddingValues(0.dp)) {
                Text(text = "알림 켜기", style = AppTextStyles.b2_semibold_16, color = PurpleMain500)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTextStyles.title_semibold_24,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun SectionSub(title: String) {
    Text(
        text = title,
        style = AppTextStyles.b1_semibold_18,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun NotiCard(
    item: NotificationItem,
    tinted: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    onAcceptInvite: () -> Unit,
    onRejectInvite: () -> Unit,
) {
    val targetBg = if (item.unread && tinted) Purple50 else Color.Transparent
    val container by animateColorAsState(targetValue = targetBg, label = "notiBg")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container)
            .padding(vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val subtitle = when (item.type) {
                NotiType.UnviewedReminder -> "미시청 카드 리마인드"
                NotiType.NewCard -> "추가하기"
                NotiType.Invite -> "공유하기"
                NotiType.System -> "기타"
            }

            Text(subtitle, style = AppTextStyles.b2_regular_16, modifier = Modifier.weight(1f), color = GreyMain300)
            Text(item.timeLabel, style = AppTextStyles.b2_regular_16, color = GreyMain300)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                item.message,
                style = AppTextStyles.b1_medium_18,
                color = Color.Black,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        when(val action = item.action){
            is NotiAction.Invite -> {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NuboPrimaryButton(
                        label = action.acceptLabel,
                        onClick = onAcceptInvite,
                        modifier = Modifier.height(40.dp),
                        enabled = !loading
                    )
                    NuboPrimaryButton(
                        label = action.rejectLabel,
                        onClick = onRejectInvite,
                        modifier = Modifier.height(40.dp),
                        bgColor = Grey30,
                        contentColor = Grey500,
                        enabled = !loading
                    )
                }
            }
            is NotiAction.ShowMore, null -> {}
        }
    }
}

@Composable
private fun NotificationFooter(
    label: String,
    modifier: Modifier = Modifier,
    lineColor: Color = GreyMain100,
    labelColor: Color = GreyMain300,
) {
    // ▼▼ 변경점: 하단 여백/인셋은 LazyColumn(contentPadding)에서 처리하므로
    // 여기서는 단순한 구분선 + 라벨만 렌더링하여 리스트 아이템처럼 스크롤되게 함
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        CenterLabelDivider(
            label = label,
            lineColor = lineColor,
            labelColor = labelColor,
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 12.dp)
        )
        // 하단 Spacer 제거 (고정/붙박이 느낌을 없애기 위함)
    }
}

@Composable
fun NuboPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = PurpleMain500,
    pressedBgColor: Color = bgColor.copy(alpha = 0.9f),
    disabledBgColor: Color = bgColor.copy(alpha = 0.5f),
    contentColor: Color = Color.White,
    disabledContentColor: Color = Color.White.copy(alpha = 0.7f),
    enabled: Boolean = true,
    shape: androidx.compose.foundation.shape.CornerBasedShape = MaterialTheme.shapes.small,
) {
    var pressed by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) pressedBgColor else bgColor,
            contentColor = contentColor,
            disabledContainerColor = disabledBgColor,
            disabledContentColor = disabledContentColor,
        ),
    ) {
        Text(text = label, style = AppTextStyles.b2_semibold_16)
    }
}

@Composable
fun CenterLabelDivider(
    label: String,
    modifier: Modifier = Modifier,
    lineColor: Color = GreyMain100,
    lineThickness: Dp = 1.dp,
    labelPadding: Dp = 12.dp,
    labelColor: Color = GreyMain300,
    textStyle: TextStyle = AppTextStyles.label_semibold_14
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(lineThickness)
                .background(lineColor)
        )

        Text(
            text = label,
            style = textStyle,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = labelPadding)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(lineThickness)
                .background(lineColor)
        )
    }
}

private fun notiStableKey(section: String, item: NotificationItem): String {
    val nid = item.notificationId.ifEmpty { "noNid" }
    val iid = item.invitationId?.toString() ?: "noIid"
    return "$section-$nid-$iid-${item.type.name}"
}

@Preview(showBackground = true, name = "NotificationFeed – Interactive")
@Composable
private fun NotificationFeedInteractive() {
    var state by remember {
        mutableStateOf(
            NotificationFeedState(
                recent = listOf(
                    NotificationItem(
                        notificationId = "r1",
                        title = "아직 열어보지 않은 카드가 있어요",
                        message = "잊기 전에 확인해보세요",
                        timeLabel = "지금",
                        type = NotiType.UnviewedReminder,
                        unread = true,
                    ),
                    NotificationItem(
                        notificationId = "r2",
                        title = "새로운 카드가 생성 완료되었어요",
                        message = "",
                        timeLabel = "1시간 전",
                        type = NotiType.NewCard,
                        unread = true,
                        action = NotiAction.Invite()
                    ),
                ),
                past = listOf(
                    NotificationItem(
                        notificationId = "p1",
                        title = "박동훈 님이 공유 보드 초대를 수락했습니다",
                        message = "이제 함께 보드를 관리할 수 있습니다",
                        timeLabel = "9월 9일",
                        type = NotiType.Invite,
                        unread = false
                    ),
                )
            )
        )
    }

    NotificationScreen(
        state = state,
        onRefresh = {},
        onClickItem = {},
        onAcceptInvite = { item ->
            state = state.copy(
                recent = state.recent.filterNot { it.notificationId == item.notificationId }
            )
        },
        onRejectInvite = { item ->
            state = state.copy(
                recent = state.recent.filterNot { it.notificationId == item.notificationId }
            )
        },
        onShowMore = {},
        onBack = {},
        onAlarmSetting = {}
    )
}
