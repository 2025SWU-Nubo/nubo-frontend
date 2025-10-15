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
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
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
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.buildAppNotificationSettingsIntent
import com.example.nubo.utils.openAppNotificationSettings
import com.example.nubo.utils.rememberNotificationSettingsLauncher
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain100


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
    var addBottomSpacer by remember { mutableStateOf(false) }

    // --- 스크롤 애니메이션을 위한 상태/스코프 ---
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
        val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()


        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(bottom = 0.dp)
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
                            // 1) 클릭 시점의 "기존 마지막 인덱스"를 계산
                            //    헤더(1) + (배너가 있으면 2개 더) + 현재 노출 중인 최근 아이템 개수
                            val bannerExtra = if (!notificationsEnabled) 2 else 0
                            val firstNewRecentIndex = 1 + bannerExtra + expandedRecentCount

                            // 2) 모든 최근 아이템을 보이도록 확장
                            expandedRecentCount = state.recent.size
                            addBottomSpacer = true
                            onShowMore(NotiSection.Recent)

                            // 3) 새로 드러나는 '첫 아이템' 위치로 부드럽게 스크롤
                            scope.launch {
                                // 3) 리컴포지션으로 리스트가 확장 반영될 시간을 한 프레임 정도 줌
                                delay(18)

                                // 4) 방금 드러난 첫 아이템의 현재 위치를 조회
                                val layout = listState.layoutInfo
                                val target = layout.visibleItemsInfo.firstOrNull { it.index == firstNewRecentIndex }

                                if (target != null) {
                                    // viewport의 시작(top)으로부터 목표 아이템 top까지의 픽셀 거리 계산
                                    val distancePx = (target.offset - layout.viewportStartOffset).toFloat()

                                    // 5) 지정한 duration으로 천천히 스크롤 (원하면 600~900ms 사이로 조절)
                                    listState.animateScrollBy(
                                        value = distancePx,
                                        animationSpec = tween(
                                            durationMillis = 900,              // ← 여기로 속도 조절
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                } else {
                                    // 혹시 가시 영역에 바로 안 잡히면 fallback
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
                            // 1) 지난 섹션 시작 인덱스 및 새로 드러날 첫 인덱스 계산
                            val bannerExtra = if (!notificationsEnabled) 2 else 0
                            val recentMoreBtnExtra =
                                if (state.recent.size >= 3 && expandedRecentCount < state.recent.size) 1 else 0
                            // 구성: [타이틀 1] + [배너 0/2] + [최근N] + [최근더보기 0/1] + [Spacer 1] + [지난 타이틀 1]
                            val pastSectionStartIndex =
                                1 + bannerExtra + expandedRecentCount + recentMoreBtnExtra + 1 + 1
                            val firstNewPastIndex = pastSectionStartIndex + expandedPastCount

                            // 2) 목록 확장
                            expandedPastCount = state.past.size
                            addBottomSpacer = true
                            onShowMore(NotiSection.Past)

                            scope.launch {
                                // 3) 확장 반영 대기
                                delay(18)

                                // 4) 목표 아이템 위치 파악
                                val layout = listState.layoutInfo
                                val target = layout.visibleItemsInfo.firstOrNull { it.index == firstNewPastIndex }

                                if (target != null) {
                                    val distancePx = (target.offset - layout.viewportStartOffset).toFloat()

                                    // 5) 천천히 스크롤 (duration으로 속도 조절)
                                    listState.animateScrollBy(
                                        value = distancePx,
                                        animationSpec = tween(
                                            durationMillis = 900,              // ← 필요시 700~1000으로 맞춤
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

            // ===== 하단 여분(내비와 겹치지 않게) =====
            if (addBottomSpacer) {
                item {
                    // Add nav bar height + extra 16dp breathing room
                    Spacer(Modifier.height(navPadding + 16.dp))
                }
            }

            item { CenterLabelDivider(label = "7일 전 알림까지 확인할 수 있어요",
                lineColor = Grey50,
                labelColor = Grey200) }

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
                .background(Purple50, shape = RoundedCornerShape(72.dp)) // pill
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
        /* 미시청 카드 리마인드 */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            /* 알림 타입 */
            val subtitle = when (item.type) {
                NotiType.UnviewedReminder -> "미시청 카드 리마인드"
                NotiType.NewCard -> "추가하기"
                NotiType.Invite -> "공유하기"
                NotiType.System -> "시스템"
            }

            /* 알림 소제목 */
            Text(subtitle, style = AppTextStyles.b2_regular_16, modifier = Modifier.weight(1f), color = GreyMain300)
            /* 알림 시간 */
            Text(item.timeLabel, style = AppTextStyles.b2_regular_16, color = GreyMain300)
        }
        // 알림 내용
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

        // 액션 버튼(더보기, 공유하기_초대 수락,거절)
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
            is NotiAction.ShowMore, null -> {
                // ShowMore는 카드 외부에서 처리, null은 버튼 없음
            }
        }
    }
}

// ===== Custom Buttons =====
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
    lineColor: Color = GreyMain100, // = Grey10 정도
    lineThickness: Dp = 1.dp,
    labelPadding: Dp = 12.dp,
    labelColor: Color = GreyMain300, // = GreyMain300 정도
    textStyle: TextStyle = AppTextStyles.label_semibold_14 // 프로젝트 스타일에 맞게
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(lineThickness)
                .background(lineColor)
        )

        // Center label
        Text(
            text = label,
            style = textStyle,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = labelPadding)
        )

        // Right line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(lineThickness)
                .background(lineColor)
        )
    }
}


// LazyColumn 아이템 고유키 생성 (섹션별 네임스페이스 + 복합키)
//  - notificationId가 비거나 중복일 수 있으므로 invitationId, type까지 섞음
private fun notiStableKey(section: String, item: NotificationItem): String {
    val nid = item.notificationId.ifEmpty { "noNid" }
    val iid = item.invitationId?.toString() ?: "noIid"
    return "$section-$nid-$iid-${item.type.name}"
}

@Preview(showBackground = true, name = "NotificationFeed – Interactive")
@Composable
private fun NotificationFeedInteractive() {
    // PreviewParameter 제거하고 직접 초기화
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
                    NotificationItem(
                        notificationId = "r3",
                        title = "김친구 님이 '디자인' 보드를 공유하고 싶어해요",
                        message = "",
                        timeLabel = "1일 전",
                        type = NotiType.Invite,
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
                    NotificationItem(
                        notificationId = "p2",
                        title = "스케치 공유 보드가 내 보드에 추가되었습니다",
                        message = "",
                        timeLabel = "9월 1일",
                        type = NotiType.Invite,
                        unread = false
                    )
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
