@file:Suppress("UnusedMaterial3ScaffoldPaddingParameter")
package com.example.nubo.ui.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500

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

    val totalUnviewedRecent = remember(state.recent) { state.recent.count { it.type == NotiType.UnviewedReminder } }
    val totalUnviewedPast   = remember(state.past)   { state.past.count   { it.type == NotiType.UnviewedReminder } }

    var visibleUnviewedRecent by remember(totalUnviewedRecent) {
        mutableStateOf(if (totalUnviewedRecent > 0) 1 else 0)
    }
    var visibleUnviewedPast by remember(totalUnviewedPast) {
        mutableStateOf(if (totalUnviewedPast > 0) 1 else 0)
    }

    var expandedRecentCount by remember { mutableStateOf(1) }
    var expandedPastCount by remember {mutableStateOf(1)}

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0),
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
                }
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {

            // ===== 알림 섹션 =====
            item { SectionHeader("알림") }
            val visibleRecentItems = state.recent.take(expandedRecentCount)

            itemsIndexed(visibleRecentItems, key = { _, it -> it.id }) { _, item ->
                NotiCard(
                    item = item,
                    tinted = true,
                    onClick = { onClickItem(item) },
                    onAcceptInvite = { onAcceptInvite(item) },
                    onRejectInvite = { onRejectInvite(item) }
                )
            }

            // N 건 더보기 텍스트 버튼
            if (state.recent.size > expandedRecentCount) {
                item{
                    TextButton(
                        onClick = {
                            expandedRecentCount = minOf(
                                expandedRecentCount + 2,
                                state.recent.size
                            )
                            onShowMore(NotiSection.Recent)
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
            item { SectionSub("지난 알림") }

            val visiblePastItems = state.past.take(expandedPastCount)
            itemsIndexed(visiblePastItems, key = { _, it -> it.id }) { _, item ->
                NotiCard(
                    item = item,
                    tinted = false,
                    onClick = { onClickItem(item) },
                    onAcceptInvite = { onAcceptInvite(item) },
                    onRejectInvite = { onRejectInvite(item) }
                )
            }

            // "N건 더보기" 텍스트 버튼
            if (state.past.size > expandedPastCount) {
                item {
                    TextButton(
                        onClick = {
                            expandedPastCount = minOf(
                                expandedPastCount + 2,
                                state.past.size
                            )
                            onShowMore(NotiSection.Past)
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
        }
    }
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
    onClick: () -> Unit,
    onAcceptInvite: () -> Unit,
    onRejectInvite: () -> Unit,
) {
    val container = if (tinted) Purple50 else Color.Transparent

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
//        Spacer(Modifier.height(8.dp))
        // 알림 내용
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                item.title,
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
                        modifier = Modifier.height(40.dp)
                    )
                    NuboPrimaryButton(
                        label = action.rejectLabel,
                        onClick = onRejectInvite,
                        modifier = Modifier.height(40.dp),
                        bgColor = Grey30,
                        contentColor = Grey500
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

// English comments only.
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
    shape: androidx.compose.foundation.shape.CornerBasedShape = MaterialTheme.shapes.small,
) {
    var pressed by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        shape = shape,
        modifier = modifier,
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
fun NuboGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = label, style = AppTextStyles.b2_bold_16, color = contentColor)
    }
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
                        id = "r1",
                        title = "아직 열어보지 않은 카드가 있어요",
                        message = "잊기 전에 확인해보세요",
                        timeLabel = "지금",
                        type = NotiType.UnviewedReminder,
                        unread = true,
                    ),
                    NotificationItem(
                        id = "r2",
                        title = "새로운 카드가 생성 완료되었어요",
                        message = "",
                        timeLabel = "1시간 전",
                        type = NotiType.NewCard,
                        unread = true,
                        action = NotiAction.Invite()
                    ),
                    NotificationItem(
                        id = "r3",
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
                        id = "p1",
                        title = "박동훈 님이 공유 보드 초대를 수락했습니다",
                        message = "이제 함께 보드를 관리할 수 있습니다",
                        timeLabel = "9월 9일",
                        type = NotiType.Invite,
                        unread = false
                    ),
                    NotificationItem(
                        id = "p2",
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
                recent = state.recent.filterNot { it.id == item.id }
            )
        },
        onRejectInvite = { item ->
            state = state.copy(
                recent = state.recent.filterNot { it.id == item.id }
            )
        },
        onShowMore = {},
        onBack = {},
        onAlarmSetting = {}
    )
}
