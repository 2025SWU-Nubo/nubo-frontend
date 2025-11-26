package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles

// 1번 스텝  스크린샷만 중앙에 표시
@Composable
fun OnboardingStep1Page() {
    StepBaseLayout(
        stepNumber = 1,
        title = "버튼을 누르세요.",
        showShareIcon = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // 스크린샷과 오버레이를 감싸는 컨테이너
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(9f / 16f) // 세로 긴 화면 비율
            ) {
                Image(
                    painter = painterResource(id = R.drawable.onboarding_step_1),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth() ,
                    contentScale = ContentScale.Fit
                )
                // 상단 좌측 YouTube 안내 영역
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        // 스크린샷 바깥 쪽으로 살짝 올려서 위치 맞추기
                        .offset(x = (0).dp, y = (-90).dp)
                ) {
                    // 텍스트와 아이콘을 한 덩어리로 배치
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YouTube일 때",
                            style = AppTextStyles.label_SemiBold_12
                        )
                        Image(
                            painter = painterResource(id = R.drawable.youtube_share),
                            contentDescription = null,
                            modifier = Modifier
                                .height(36.dp)  // 아이콘 크기
//                                .offset( y = (-10).dp)
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.sub_arrow),
                        contentDescription = null,
                        modifier = Modifier
                            .height(50.dp)  // 아이콘 크기
                            .offset( x = (32).dp, y=(32).dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

