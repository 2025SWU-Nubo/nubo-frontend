package com.example.nubo.ui.screen.onBoardingLogin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.ui.unit.dp
import com.example.nubo.MainActivity
import com.example.nubo.R
import com.example.nubo.data.model.LoginRequest
import com.example.nubo.data.model.LoginResponse
import com.example.nubo.data.network.RetrofitClient
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState = viewModel.uiState.collectAsState().value

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
        }
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
        delay(1500)
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
