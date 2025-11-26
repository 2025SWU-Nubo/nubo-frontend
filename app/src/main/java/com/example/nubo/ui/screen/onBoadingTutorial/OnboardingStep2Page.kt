package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles

// 2번 스텝  스크린샷 상단 좌측에 YouTube 안내 표시
@Composable
fun OnboardingStep2Page() {
    StepBaseLayout(
        stepNumber = 2,
        title = "공유 버튼을 누르세요."
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
                // 실제 영상 스크린샷
                Image(
                    painter = painterResource(id = R.drawable.onboarding_step_2),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(9f / 16f),
                    contentScale = ContentScale.Fit
                )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
