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
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.animateContentSize



// ===== Models =====
enum class NotiSection { Recent, Past }

// ===== Screen =====
@OptIn( ExperimentalMaterial3Api::class )
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
    onMarkAllRead: () -> Unit,
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

    // all read 상태
    val allRead = state.recent.none { it.unread } && state.past.none { it.unread }
    val hasAnyNoti = state.recent.isNotEmpty() || state.past.isNotEmpty()

    // 채널별 그룹
    val recentGroups = remember(state.recent) { state.recent.groupBy { it.type } }
    val pastGroups = remember(state.past) { state.past.groupBy { it.type } }

    // 채널별 더보기 펼침 상태
    var expandedRecentTypes by remember(state.recent) { mutableStateOf(setOf<NotiType>()) }
    var expandedPastTypes by remember(state.past) { mutableStateOf(setOf<NotiType>()) }

    // --- 스크롤 애니메이션을 위한 상태/스코프 ---
    val listState = rememberLazyListState()

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
        },) { inner ->

        // 네비게이션 인셋 계산
        val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // 리스트 스크롤 가능 여부
        val isScrollable by remember {
            derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
        }

        // 푸터 실제 높이를 픽셀로 보관 (오버레이일 때 리스트 하단 패딩 확보용)
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
                    bottom = navPadding + 4.dp + if (!isScrollable) footerHeightDp else 0.dp
                )
            ) {
                // ===== 알림 섹션 =====
                item {
                    SectionHeader(
                        title = "알림",
                        showMarkAll = hasAnyNoti,
                        allRead = allRead,
                        onClickMarkAll = onMarkAllRead
                    )
                }

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

                // ===== 최근 알림 리스트 (채널별) =====
                val recentEntries = recentGroups.entries.toList()

                recentEntries.forEachIndexed { groupIndex, (type, itemsOfType) ->
                    item(key = "recent-${type.name}") {

                        val expanded = expandedRecentTypes.contains(type)
                        val previewPerChannel = 1

                        // 펼치기 전에는 1개만, 펼치면 전체
                        val visibleItems =
                            if (expanded || itemsOfType.size <= previewPerChannel)
                                itemsOfType
                            else
                                itemsOfType.take(previewPerChannel)

                        val hasMore = !expanded && itemsOfType.size >= 3
                        val remainCount = (itemsOfType.size - previewPerChannel).coerceAtLeast(0)

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            visibleItems.forEachIndexed { index, item ->
                                val loading = item.notificationId in state.actionLoadingIds

                                // “알림” 섹션에서 완전 첫 번째 카드인지 여부
                                val isFirstNotification = (groupIndex == 0 && index == 0)

                                // 첫 카드면 top 24 / bottom 12, 나머지는 top 12 / bottom 12
                                val topPadding = if (isFirstNotification) 16.dp else 12.dp
                                val bottomPadding = 12.dp

                                // 이 카드에 붙을 "더보기" 개수 (접힌 상태에서 마지막 visible 카드에만)
                                val showMoreForThisCard =
                                    if (hasMore && !expanded && index == visibleItems.lastIndex && remainCount > 0) {
                                        remainCount
                                    } else {
                                        null
                                    }

                                NotiCard(
                                    item = item,
                                    tinted = true,                 // 최근 알림은 틴트 대상
                                    loading = loading,
                                    onClick = { onClickItem(item) },
                                    onAcceptInvite = { onAcceptInvite(item) },
                                    onRejectInvite = { onRejectInvite(item) },
                                    topPadding = topPadding,
                                    bottomPadding = bottomPadding,
                                    showMoreCount = showMoreForThisCard,
                                    // ▶ 이 카드의 더보기 클릭 시: 타입 확장 + 콜백 호출
                                    onClickShowMore = if (showMoreForThisCard != null) {
                                        {
                                            expandedRecentTypes = expandedRecentTypes + type
                                            onShowMore(NotiSection.Recent)
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }



                // ===== 지난 알림 섹션 =====
                item { Spacer(Modifier.height(12.dp)) }

                if (state.past.isNotEmpty()) {
                    item { SectionSub("지난 알림") }
                }

                // ===== 지난 알림 리스트 (채널별) =====
                val pastEntries = pastGroups.entries.toList()

                pastEntries.forEachIndexed { groupIndex, (type, itemsOfType) ->
                    item(key = "past-${type.name}") {

                        val expanded = expandedPastTypes.contains(type)
                        val previewPerChannel = 1

                        val visibleItems =
                            if (expanded || itemsOfType.size <= previewPerChannel)
                                itemsOfType
                            else
                                itemsOfType.take(previewPerChannel)

                        val hasMore = !expanded && itemsOfType.size >= 3
                        val remainCount = (itemsOfType.size - previewPerChannel).coerceAtLeast(0)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                        ) {
                            visibleItems.forEachIndexed { index, item ->
                                val loading = item.notificationId in state.actionLoadingIds

                                val isFirstNotification = (groupIndex == 0 && index == 0)
                                val topPadding = if (isFirstNotification) 24.dp else 12.dp
                                val bottomPadding = 12.dp

                                val showMoreForThisCard =
                                    if (hasMore && !expanded && index == visibleItems.lastIndex && remainCount > 0) {
                                        remainCount
                                    } else {
                                        null
                                    }

                                NotiCard(
                                    item = item,
                                    tinted = false,               // 지난 알림은 틴트 없음
                                    loading = loading,
                                    onClick = { onClickItem(item) },
                                    onAcceptInvite = { onAcceptInvite(item) },
                                    onRejectInvite = { onRejectInvite(item) },
                                    topPadding = topPadding,
                                    bottomPadding = bottomPadding,
                                    showMoreCount = showMoreForThisCard,
                                    onClickShowMore = if (showMoreForThisCard != null) {
                                        {
                                            expandedPastTypes = expandedPastTypes + type
                                            onShowMore(NotiSection.Past)
                                        }
                                    } else null
                                )
                            }
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
private fun SectionHeader(
    title: String,
    showMarkAll: Boolean,
    allRead: Boolean,
    onClickMarkAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTextStyles.title_semibold_24,
            modifier = Modifier.weight(1f)
        )

        if (showMarkAll) {
            TextButton(
                onClick = onClickMarkAll,
                enabled = !allRead,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "모두 읽음",
                    style = AppTextStyles.b2_semibold_16,
                    color = if (allRead) GreyMain300 else PurpleMain500
                )
            }
        }
    }
}

@Composable
private fun SectionSub(title: String) {
    Text(
        text = title,
        style = AppTextStyles.b1_semibold_18,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun NotiCard(
    item: NotificationItem,
    tinted: Boolean,              // 최근 알림이면 true, 지난 알림이면 false
    loading: Boolean,
    onClick: () -> Unit,
    onAcceptInvite: () -> Unit,
    onRejectInvite: () -> Unit,
    // 카드 별로 상단/하단 패딩을 외부에서 제어하기 위한 파라미터
    topPadding: Dp,
    bottomPadding: Dp,
    // 이 카드에 연결된 "n건 더보기" 개수 (없으면 null)
    showMoreCount: Int? = null,
    // "더보기" 클릭 시 호출될 콜백 (null이면 버튼을 그려도 아무 일 하지 않음)
    onClickShowMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // 이 알림이 안 읽은 상태이고 tint 대상이면 보라색, 아니면 흰색 배경
    val targetBg = if (tinted && item.unread) Purple50 else Color.White
    val container by animateColorAsState(
        targetValue = targetBg,
        label = "notiBg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            // 카드 전체 배경
            .background(container)
            // 알림 자체 패딩: 좌우 32, 상단/하단은 파라미터로 조절
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = topPadding,
                bottom = bottomPadding
            )
    ) {
        // ─ 상단 서브타이틀 + 시간 ─
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val subtitle = when (item.type) {
                NotiType.UnviewedReminder -> "미시청 카드 리마인드"
                NotiType.NewCard -> "추가하기"
                NotiType.Invite -> "공유하기"
                NotiType.System -> "공유하기"
            }
            Text(
                text = subtitle,
                style = AppTextStyles.b2_regular_16,
                modifier = Modifier.weight(1f),
                color = GreyMain300
            )
            Text(
                text = item.timeLabel,
                style = AppTextStyles.b2_regular_16,
                color = GreyMain300
            )
        }

        // 텍스트 간 간격 8
        Spacer(Modifier.height(8.dp))

        // ─ 본문 메시지 ─
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Text(
                text = item.message,
                style = AppTextStyles.b1_medium_18,
                color = Color.Black,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ─ 액션 버튼 (초대 수락/거절 등) ─
        when (val action = item.action) {
            is NotiAction.Invite -> {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
            is NotiAction.ShowMore, null -> { /* 아무것도 안 그림 */ }
        }

        // ─ "n건 더보기" 텍스트 (있을 때만, 카드 배경 안에서) ─
        if (showMoreCount != null && showMoreCount > 0) {

            TextButton(
                onClick = { onClickShowMore?.invoke() }, // ▶ 여기서 확장 로직 호출
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "${showMoreCount}건 더보기",
                    style = AppTextStyles.b2_semibold_16,
                    color = PurpleMain500
                )
            }
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
            modifier = Modifier
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                        message = "아직 열어보지 않은 카드가 있어요\n잊기 전에 확인해보세요",
                        timeLabel = "지금",
                        type = NotiType.UnviewedReminder,
                        unread = true,
                    ),
                    NotificationItem(
                        notificationId = "r2",
                        title = "아직 열어보지 않은 카드가 있어요",
                        message = "오늘 저장한 카드 중 아직 안 본 카드가 있어요",
                        timeLabel = "1시간 전",
                        type = NotiType.UnviewedReminder,
                        unread = true,
                    ),
                    NotificationItem(
                        notificationId = "r3",
                        title = "아직 열어보지 않은 카드가 있어요",
                        message = "하루 전에 저장한 카드도 한 번 더 확인해보세요",
                        timeLabel = "하루 전",
                        type = NotiType.UnviewedReminder,
                        unread = false,
                    ),
                    NotificationItem(
                        notificationId = "r2",
                        title = "새로운 카드가 생성 완료되었어요",
                        message = "새로운 카드가 생성 완료되었어요",
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
                        message = "박동훈 님이 공유 보드 초대를 수락했습니다\n이제 함께 보드를 관리할 수 있습니다",
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
        onAlarmSetting = {},
        onMarkAllRead = {}
    )
}
