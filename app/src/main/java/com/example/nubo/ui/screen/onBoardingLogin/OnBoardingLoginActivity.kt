package com.example.nubo.ui.screen.onBoardingLogin

import android.content.Intent
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
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.components.toast.AppToastOverlay
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.MainActivity
import com.example.nubo.R
import com.example.nubo.ui.component.dialog.NotificationPermissionDialog
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.cacheToStore
import com.example.nubo.utils.rememberNotificationSettingsLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

@AndroidEntryPoint
class OnBoardingLoginActivity : ComponentActivity() {

    private val viewModel: OnBoardingViewModel by viewModels()



    // Google 로그인 런처
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it)
            viewModel.handleSignInResult(task) { baseIntent ->
                // 메인으로 이동할 Intent를 보강(백스택 정리 + 현재 온보딩이 가진 extras 복사)
                val main = baseIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    this@OnBoardingLoginActivity.intent?.extras?.let { putExtras(it) }
                    setClass(this@OnBoardingLoginActivity, MainActivity::class.java)
                }
                startActivity(main)
                finish()
            }
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.toast("알림 권한이 허용되었어요.", AppToastType.ALARM_ALLOWED)
            } else {
                viewModel.toast("알림 권한이 거부되었어요.", AppToastType.ALARM_DENIED)
            }
            viewModel.onLoginNotificationPermissionHandled()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let { cacheToStore(it) }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            var notificationsEnabled by remember {
                mutableStateOf(
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                )
            }

            val settingsLauncher = rememberNotificationSettingsLauncher(
                onReturn = {
                    notificationsEnabled =
                        NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
            )

            // 전역 토스트 호스트 생성
            val toastHost = rememberAppToastHostState()

            // 온보딩 전체를 CompositionLocal 로 감싸기
            CompositionLocalProvider(LocalAppToastHostState provides toastHost) {

                val uiState = viewModel.uiState.collectAsState().value
                val askPermission by viewModel.shouldRequestNotificationPermission.collectAsState()

                // 자동 토큰 검증
                LaunchedEffect(Unit) {
                    viewModel.onStartButtonClicked()
                }

                // 토스트 이벤트 수신
                LaunchedEffect(Unit) {
                    viewModel.toastEvents.collectLatest { ev ->
                        toastHost.show(
                            title = AnnotatedString(ev.message),
                            layout = ev.layout,
                            type = ev.type,
                            durationMillis = ev.durationMillis
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.ensurePushTokenRegistered()
                }

                Box(Modifier.fillMaxSize()) {
                    // 알림 권한 다이얼로그 등 기존 코드 그대로

                    OnBoardingScreen(
                        uiState = uiState,
                        onStartClick = { viewModel.onStartButtonClicked() },
                        onGoogleLoginClick = {
                            googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                        },
                        onAccountSwitchConfirmed = {
                            viewModel.confirmAccountSwitch { baseIntent ->
                                val main = baseIntent.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    this@OnBoardingLoginActivity.intent?.extras?.let { putExtras(it) }
                                    setClass(this@OnBoardingLoginActivity, MainActivity::class.java)
                                }
                                startActivity(main)
                                finish()
                            }
                        }
                    )

                    // 알림 권한 안내 다이얼로그
                    NotificationPermissionDialog(
                        visible = askPermission,
                        onAllow = {
                            // "알림 켜기" 버튼
                            if (Build.VERSION.SDK_INT >= 33) {
                                // 시스템 권한 다이얼로그 호출
                                requestNotificationPermissionLauncher.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                // 13 미만은 바로 다음 단계로 진행
                                viewModel.onLoginNotificationPermissionHandled()
                            }
                        },
                        onLater = {
                            // "나중에" 선택  바로 다음 단계로 진행
                            viewModel.toast("알림은 나중에 설정에서도 바꿀 수 있어요")
                            viewModel.onLoginNotificationPermissionHandled()
                        },
                        onDismiss = {
                            // 바깥 터치로 닫았을 때도 흐름은 계속 가야 함
                            viewModel.onLoginNotificationPermissionHandled()
                        }
                    )

                    // 전역 토스트 오버레이
                    AppToastOverlay(hostState = toastHost)
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 실행 중 새 인텐트로 들어와도 FCM/딥링크 캐시
        cacheToStore(intent)
    }
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
