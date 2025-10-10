package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.launch

// 알림 설정 화면

@Composable
fun NotificationSetScreen(
    navController: NavController? = null,   // 실제 화면에선 NavController 주입
    onBack: () -> Unit = { navController?.popBackStack() },
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // VM 상태 바인딩/스낵바/코루틴
    val ui = viewModel.uiState.collectAsState().value
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = { NotiTopBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbar) } // ← 실패 시 안내 토스트
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
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

            // 미시청 카드 리마인드 알림
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "미시청 카드 리마인드 알림",
                        style = AppTextStyles.b1_semibold_18,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "잊혀진 카드에 대한 리마인드를 제공합니다.",
                        style = AppTextStyles.b3_regular_14,
                        color = Grey500
                    )
                }
                Switch(
                    checked = ui.remindEnabled,
                    enabled = ui.pushEnabled, // 전체 알림 꺼져 있으면 비활성화
                    onCheckedChange = { checked ->
                        scope.launch {
                            viewModel.toggleRemind(checked) { msg ->
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
    }
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

// 프리뷰

@Preview(showBackground = true, name = "알림 설정 기본")
@Composable
private fun PreviewNotification() {
    NotificationSetScreen(navController = null)
}
