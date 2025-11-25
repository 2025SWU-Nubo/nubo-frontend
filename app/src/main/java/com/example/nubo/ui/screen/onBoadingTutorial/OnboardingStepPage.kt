package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.example.nubo.data.model.OnboardingPage
import com.example.nubo.data.model.StepOverlay
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500

// 온보딩 스텝 페이지 (2~5번 공통)
@Composable
fun OnboardingStepPage(
    step: OnboardingPage.Step
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 페이지 번호 배지
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

        // 타이틀
        Text(
            text = step.title,
            style = AppTextStyles.title_bold_24,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 튜토리얼 이미지 + 오버레이 영역 =====
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),                 // 남은 세로 공간 전체 사용
            contentAlignment = Alignment.Center
        ) {
            val baseWidth = maxWidth   // 기준 이미지 너비
            val baseHeight = baseWidth        // 정사각형 기준 (필요하면 조정 가능)

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 기본 튜토리얼 이미지
                Image(
                    painter = painterResource(id = step.imageResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 오버레이 이미지들
                step.overlays.forEach { overlay ->
                    StepOverlayImage(
                        overlay = overlay,
                        baseWidth = baseWidth,
                        baseHeight = baseHeight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // (필요하면 description 같은 텍스트 추가)
        // Text(...)
    }
}

/**
 * 스텝 이미지 위에 올리는 오버레이 이미지
 *
 * This is a BoxScope extension so that we can use Modifier.align(...)
 */
@Composable
private fun BoxScope.StepOverlayImage(
    overlay: StepOverlay,
    baseWidth: Dp,
    baseHeight: Dp
) {
    val overlayWidth = baseWidth * overlay.widthFraction

    Image(
        painter = painterResource(id = overlay.imageResId),
        contentDescription = null,
        modifier = Modifier
            // 기준 이미지(Box) 안에서 정렬
            .align(overlay.alignment)
            // 기준 이미지 크기 대비 상대 위치 이동
            .offset(
                x = baseWidth * overlay.offsetXFraction,
                y = baseHeight * overlay.offsetYFraction
            )
            // 오버레이 너비를 기준 이미지 비율로 설정
            .width(overlayWidth)
    )
}
