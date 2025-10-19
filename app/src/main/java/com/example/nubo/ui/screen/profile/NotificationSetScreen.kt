package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.components.toast.AppToastHost
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.buildAppNotificationSettingsIntent
import com.example.nubo.utils.rememberNotificationSettingsLauncher
import kotlinx.coroutines.launch
import kotlin.math.max

// 알림 설정 화면

@Composable
fun NotificationSetScreen(
    navController: NavController? = null,   // 실제 화면에선 NavController 주입
    onBack: () -> Unit = { navController?.popBackStack() },
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // 시스템 알림 허용 여부
    var notificationsEnabled by remember {
        mutableStateOf(com.example.nubo.utils.NotificationPermissionHelper.isSystemNotificationEnabled(context))
    }
    var reminderChannelOn by remember {
        mutableStateOf(com.example.nubo.utils.NotificationPermissionHelper.isChannelEnabled(context, "reminder_channel"))
    }


    // 설정으로 나갔다가 돌아오면 최신 상태 반영
    val settingsLauncher = rememberNotificationSettingsLauncher(
        onReturn = {
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            reminderChannelOn =
                com.example.nubo.utils.NotificationPermissionHelper.isChannelEnabled(context, "reminder_channel")
        }
    )

    // 화면 복귀 시 최신값 반영
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled =
                    com.example.nubo.utils.NotificationPermissionHelper.isSystemNotificationEnabled(context)
                reminderChannelOn =
                    com.example.nubo.utils.NotificationPermissionHelper.isChannelEnabled(context, "reminder_channel")
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }


    // VM 상태 바인딩/스낵바/코루틴
    val ui = viewModel.uiState.collectAsState().value
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val toastHost = rememberAppToastHostState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            if (ev is ProfileEvent.ShowToast) {
                val type = when (ev.kind) {
                    ToastKind.POSITIVE -> AppToastType.POSITIVE
                    ToastKind.NEGATIVE -> AppToastType.NEGATIVE
                    ToastKind.NORMAL   -> AppToastType.NORMAL
                }
                toastHost.show(
                    title = AnnotatedString(ev.message),
                    layout = AppToastLayout.TitleOnly,
                    type = type,
                    durationMillis = ev.durationMillis
                )
            }
        }
    }



    Scaffold(
        topBar = { NotiTopBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbar) } // ← 실패 시 안내 토스트
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {

            Column(modifier = Modifier.fillMaxSize()) {
                SystemNotificationBanner(
                    notificationsEnabled = notificationsEnabled,
                    onClickEnable = {
                        com.example.nubo.utils.NotificationPermissionHelper.openAppNotificationSettings(context)
                    }
                )

                Spacer(Modifier.height(3.dp))
                Divider(color = Grey50)
                Spacer(Modifier.height(16.dp))

                // 전체 알림 섹션
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "전체 알림",
                            style = AppTextStyles.b1_semibold_18,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (ui.pushEnabled) "허용" else "미허용",
                            style = AppTextStyles.b1_semibold_18,
                            color = if (ui.pushEnabled) PurpleMain500 else GreyMain300
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        Switch(
                            checked = ui.pushEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.togglePush(checked) { msg ->
                                        scope.launch { snackbar.showSnackbar(msg) }
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = PurpleMain500,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Grey50,
                                uncheckedThumbColor = Grey500
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Grey30,
                    thickness = 5.dp
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("미시청 카드 리마인드 알림", style = AppTextStyles.b1_semibold_18)
                        Spacer(Modifier.height(6.dp))
                        Text("잊혀진 카드에 대한 리마인드를 제공합니다.", style = AppTextStyles.b3_regular_14, color = Grey500)

//                    // 채널 상태 + 설정 이동
//                    Spacer(Modifier.height(8.dp))
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        val label = if (reminderChannelOn) "채널: 허용" else "채널: 차단됨"
//                        val color = if (reminderChannelOn) PurpleMain500 else GreyMain300
//                        Text(label, style = AppTextStyles.b3_regular_14, color = color)
//                        Spacer(Modifier.width(8.dp))
//                        TextButton(
//                            onClick = {
//                                com.example.nubo.utils.NotificationPermissionHelper
//                                    .openChannelSettings(context, "reminder_channel")
//                            },
//                            contentPadding = PaddingValues(0.dp)
//                        ) { Text("채널 설정", style = AppTextStyles.b2_semibold_16, color = PurpleMain500) }
//                    }
                    }

                    // ④ 스위치 활성 조건 = 서버 on && 시스템 on && 채널 on
                    val enabled = ui.pushEnabled && notificationsEnabled && reminderChannelOn
                    Switch(
                        checked = ui.remindEnabled,
                        enabled = enabled,
                        onCheckedChange = { checked ->
                            scope.launch {
                                viewModel.toggleRemind(checked) { msg ->  scope.launch { snackbar.showSnackbar(msg) } }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = PurpleMain500,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Grey50,
                            uncheckedThumbColor = Grey500
                        )
                    )
                }
            }

            AppToastHost(
                hostState = toastHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = rememberImeOrNavBottomPadding(extra = 24.dp))
            )
        }

    }
}


@Composable
private fun SystemNotificationBanner(
    notificationsEnabled: Boolean,
    onClickEnable: () -> Unit,
) {
    if (notificationsEnabled) return
    Box(Modifier.padding(horizontal = 20.dp)) {
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
                Text("알림 켜기", style = AppTextStyles.b2_semibold_16, color = PurpleMain500)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}


// 상단바
@Composable
private fun NotiTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        // 뒤로가기 버튼 박스
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.00f), CircleShape)
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = Grey1000
            )
        }

        // 화면 타이틀
        Text(
            text = "알림 설정",
            style = AppTextStyles.subtitle_semibold_20,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun rememberImeOrNavBottomPadding(extra: Dp = 0.dp): Dp {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomPx = max(imeBottom, navBottom)
    return with(density) { bottomPx.toDp() } + extra
}

// 프리뷰

@Preview(showBackground = true, name = "알림 설정 기본")
@Composable
private fun PreviewNotification() {
    NotificationSetScreen(navController = null)
}
