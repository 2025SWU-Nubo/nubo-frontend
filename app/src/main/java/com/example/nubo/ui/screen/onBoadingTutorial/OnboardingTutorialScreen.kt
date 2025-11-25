// TutorialScreen.kt
package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.launch

// 온보딩 전체 페이지 수  인트로 4개 스텝 아웃트로
private const val TOTAL_PAGES = 6

@Composable
fun TutorialScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState { TOTAL_PAGES }
    val scope = rememberCoroutineScope()

    // 뒤로가기 동작 커스터마이징
    BackHandler(enabled = true) {
        scope.launch {
            if (pagerState.currentPage > 0) {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            } else {
                onFinish()
            }
        }
    }

    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentPage = pagerState.currentPage,
                onBackClick = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onSkipClick = {
                    onFinish()
                }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                lastPageIndex = TOTAL_PAGES - 1,
                onNextClick = {
                    scope.launch {
                        if (pagerState.currentPage == TOTAL_PAGES - 1) {
                            onFinish()
                        } else {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            when (pageIndex) {
                0 -> OnboardingIntroPage()
                1 -> OnboardingStep1Page()
                2 -> OnboardingStep2Page()
                3 -> OnboardingStep3Page()
                4 -> OnboardingStep4Page()
                5 -> OnboardingOutroPage(stepNumber = 5)
            }
        }
    }
}

// 상단 앱바  뒤로가기와 건너뛰기 버튼만 있는 형태
@Composable
private fun OnboardingTopBar(
    currentPage: Int,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // 하단 1dp 구분선 그리기
                val y = size.height
                drawLine(
                    color = Grey50,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage == 0) {
                // 인트로에서는 뒤로가기 숨김
                Spacer(modifier = Modifier.width(48.dp))
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "이전"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onSkipClick) {
                Text(
                    text = "건너뛰기",
                    color = Grey200,
                    style = AppTextStyles.b2_semibold_16
                )
            }
        }
    }
}

// 하단 버튼 영역  마지막 페이지만 색상과 텍스트 변경
@Composable
private fun OnboardingBottomBar(
    currentPage: Int,
    lastPageIndex: Int,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val buttonText =
            if (currentPage == lastPageIndex) "모두 이해했어요" else "다음"

        Button(
            onClick = onNextClick,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentPage == lastPageIndex) PurpleMain500 else Purple100,
                contentColor = if (currentPage == lastPageIndex) Color.White else PurpleMain500
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = buttonText,
                style = AppTextStyles.b2_medium_16
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TutorialScreenPreview() {
    MaterialTheme {
        TutorialScreen(onFinish = {})
    }
}
