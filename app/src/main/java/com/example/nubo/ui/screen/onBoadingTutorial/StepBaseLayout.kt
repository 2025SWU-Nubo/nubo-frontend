package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500

// 스텝 페이지 상단 공통 레이아웃
// - 스텝 번호 배지
// - 타이틀
// - 하단 content 슬롯은 각 페이지에서 구현
@Composable
fun StepBaseLayout(
    stepNumber: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))

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

        Spacer(modifier = Modifier.height(6.dp))

        // 타이틀 텍스트
        Text(
            text = title,
            style = AppTextStyles.title_bold_24,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 각 페이지별 본문 영역
        content()
    }
}

