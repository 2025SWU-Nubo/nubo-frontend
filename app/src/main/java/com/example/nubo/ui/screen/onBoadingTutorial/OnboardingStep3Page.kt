package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.example.nubo.R

// 3번 스텝  스크린샷 위에 화살표 오버레이
@Composable
fun OnboardingStep3Page() {
    StepBaseLayout(
        stepNumber = 3,
        title = "앱 목록을 쓸어넘겨\n더보기를 ••• 를 누르세요"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(9f / 16f)
            ) {
                // 기본 스크린샷
                Image(
                    painter = painterResource(id = R.drawable.onboarding_step_3),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 화살표 오버레이  위치는 dp로 미세 조정
                Image(
                    painter = painterResource(id = R.drawable.onboading_arrow),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-15).dp, y = (-25).dp)
                        .width(200.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

