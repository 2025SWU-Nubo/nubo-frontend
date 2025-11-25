package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R

// 4번 스텝  스크린샷만 중앙에 표시
@Composable
fun OnboardingStep4Page() {
    StepBaseLayout(
        stepNumber = 4,
        title = "앱 목록을 쓸어내려\nNubo를 누르세요"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.onboarding_step_4),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(9f / 16f),
                contentScale = ContentScale.Fit
            )

            // 화살표 오버레이  위치는 dp로 미세 조정
            Image(
                painter = painterResource(id = R.drawable.onboading_focus_dot),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (17).dp, y = (-33).dp)
                    .width(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

