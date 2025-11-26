package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500

/**
 * 스텝 공통 상단 레이아웃
 * - stepNumber: 동그라미 안에 들어갈 숫자
 * - title: 타이틀 텍스트
 * - showShareIcon: 1번 스텝처럼 타이틀 왼쪽에 공유 아이콘을 붙일지 여부
 */
@Composable
fun StepBaseLayout(
    stepNumber: Int,
    title: String,
    showShareIcon: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 스텝 번호 동그라미 배지
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = PurpleMain500.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                style = AppTextStyles.b1_bold_18,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 타이틀 + (옵션) 공유 아이콘
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showShareIcon) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = "공유 버튼 아이콘",
                    modifier = Modifier
                        .size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = title,
                style = AppTextStyles.subtitle_bold_20,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 각 페이지별 본문 영역
        content()
    }
}
