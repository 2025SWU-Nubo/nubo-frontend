package com.example.nubo.ui.screen.onBoarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nubo.MainActivity
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.utils.AuthManager
import kotlinx.coroutines.delay
import com.example.nubo.ui.screen.login.LoginActivity
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.nubo.R
import com.example.nubo.ui.theme.PurpleMain500


class OnBoardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnboardingScreen(
                onStartClicked = {
                    if (AuthManager.isUserLoggedIn(this)) {
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish() }
            )
        }
    }
}

@Composable
fun OnboardingScreen(onStartClicked: () -> Unit) {
    var showDetail by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3000) // 3초 후 텍스트 + 버튼 표시
        showDetail = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // 메인 로고 텍스트
            Image(
                painter = painterResource(id = R.drawable.nubo_logo),
                contentDescription = "Nubo 로고",
                modifier = Modifier
                    .width(210.dp)
                    .height(90.dp),
                contentScale = ContentScale.Fit
            )

            // AnimatedVisibility로 텍스트 & 버튼 서서히 등장
            AnimatedVisibility(visible = showDetail) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(
                        text = "한 곳에 모이는 학습 지식저장소",
                        style = AppTextStyles.b3_medium_14,
                        color = Color.Black
                    )
                }
            }
        }

        // 시작하기 버튼은 하단에 고정
        AnimatedVisibility(
            visible = showDetail,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        ) {
            Button(
                onClick = onStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain500)
            ) {
                Text(text = "시작하기", color = Color.White, style = AppTextStyles.b1_bold_18)
            }
        }
    }
}

