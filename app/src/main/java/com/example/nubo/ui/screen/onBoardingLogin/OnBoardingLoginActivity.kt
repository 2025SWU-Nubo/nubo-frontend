package com.example.nubo.ui.screen.onBoardingLogin

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.components.toast.AppToastOverlay
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

@AndroidEntryPoint
class OnBoardingLoginActivity : ComponentActivity() {

    private val viewModel: OnBoardingViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it)
            viewModel.handleSignInResult(task) { intent ->
                startActivity(intent)
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // 시스템 Toast 제거 → ViewModel 이벤트로
                viewModel.toast("알림 권한이 허용되었어요.⏰", AppToastType.POSITIVE)
                Log.d("NotificationPermission", "알림 권한이 허용되었습니다.")
            } else {
                viewModel.toast("알림 권한이 거부되어, 업로드 완료를 토스트로 안내할게요.", AppToastType.NEGATIVE)
                Log.d("NotificationPermission", "알림 권한이 거부되었습니다.")
            }
            viewModel.onLoginNotificationPermissionHandled()
        }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState = viewModel.uiState.collectAsState().value
            val askPermission by viewModel.shouldRequestNotificationPermission.collectAsState()

            // AppToast 호스트 준비
            val toastHost = rememberAppToastHostState()

            // ViewModel의 SharedFlow 수신 → 커스텀 토스트 표시
            LaunchedEffect(Unit) {
                viewModel.toastEvents.collectLatest{ ev ->
                    toastHost.show(
                        title = AnnotatedString(ev.message),
                        layout = ev.layout,
                        type = ev.type,
                        durationMillis = ev.durationMillis
                        // 아이콘 쓰려면 iconRes = ev.iconRes
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (askPermission) {
                    NotificationPermissionDialog(
                        onConfirm = {
                            requestNotificationPermissionLauncher.launch(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        },
                        onDismiss = { viewModel.onLoginNotificationPermissionHandled() }
                    )
                }

                OnBoardingScreen(
                    uiState = uiState,
                    onStartClick = { viewModel.onStartButtonClicked() },
                    onGoogleLoginClick = {
                        googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                    },
                    onAccountSwitchConfirmed = {
                        viewModel.confirmAccountSwitch { intent ->
                            startActivity(intent)
                            finish()
                        }
                    }
                )

                // 커스텀 토스트 오버레이 부착 (하단)
                AppToastOverlay(hostState = toastHost)
                // 또는 전체화면 배치: AppToastHost(hostState = toastHost)
            }
        }
    }
}


@Composable
fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "알림 권한 설정",
                style = AppTextStyles.b1_bold_18
            )
        },
        text = {
            Column {
                Text(
                    text = "카드 업로드 완료 알림을 받으시겠습니까?",
                    style = AppTextStyles.b2_medium_16
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 허용 시: 업로드 완료를 알림으로 안내\n• 거부 시: 업로드 완료를 토스트로 안내",
                    style = AppTextStyles.b3_medium_14,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain500)
            ) {
                Text("허용", color = Color.White, style = AppTextStyles.b3_medium_14)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("나중에", color = Color.White, style = AppTextStyles.b3_medium_14)
            }
        }
    )
}

@Composable
fun OnBoardingScreen(
    uiState: OnBoardingUiState,
    onStartClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onAccountSwitchConfirmed: () -> Unit
) {
    var showDetail by remember { mutableStateOf(false) }

    val logoWidth by animateDpAsState(
        targetValue = if (uiState.logoShrinked) 120.dp else 190.dp,
        label = "Logo Width"
    )
    val logoHeight by animateDpAsState(
        targetValue = if (uiState.logoShrinked) 50.dp else 80.dp,
        label = "Logo Height"
    )

    LaunchedEffect(Unit) {
        delay(700)
        showDetail = true
    }

    if (uiState.existingUser != null && uiState.loginResponseUser != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("계정 변경 안내") },
            text = {
                Text("기존 사용자(${uiState.existingUser.id})에서 새로운 계정(${uiState.loginResponseUser.id})으로 전환됩니다.")
            },
            confirmButton = {
                Button(onClick = onAccountSwitchConfirmed) {
                    Text("확인")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.nubo_logo),
                contentDescription = "Nubo Logo",
                modifier = Modifier
                    .width(logoWidth)
                    .height(logoHeight),
                contentScale = ContentScale.Fit
            )

            AnimatedVisibility(visible = uiState.logoShrinked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("간편하게 로그인하고", style = AppTextStyles.title_regular_24)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("다양한 서비스를 이용하세요.", style = AppTextStyles.title_bold_24)
                }
            }

            AnimatedVisibility(visible = showDetail && !uiState.logoShrinked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "한 곳에 모이는 학습 지식저장소",
                        style = AppTextStyles.b2_medium_16,
                        color = Color.Black
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showDetail && !uiState.showLoginButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        ) {
            Button(
                onClick = { onStartClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain500)
            ) {
                Text(text = "시작하기", color = Color.White, style = AppTextStyles.b1_bold_18)
            }
        }

        AnimatedVisibility(
            visible = uiState.showLoginButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        ) {
            Button(
                onClick = { if (!uiState.isLoading) onGoogleLoginClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.btn_google_logo),
                    contentDescription = "Google Logo",
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google로 로그인하기", color = Color.Black, style = AppTextStyles.b3_medium_14)
            }
        }
    }
}
