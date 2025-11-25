package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nubo.data.model.OnboardingPage
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Purple300
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun OnboardingStepPage(
    step: OnboardingPage.Step
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 페이지 번호
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
                text = step.stepNumber.toString(),
                style = AppTextStyles.b1_bold_18,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = step.title,
            style = AppTextStyles.b1_bold_18,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 튜토리얼 이미지
        Image(
            painter = painterResource(id = step.imageResId),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(16.dp))

    }
}
