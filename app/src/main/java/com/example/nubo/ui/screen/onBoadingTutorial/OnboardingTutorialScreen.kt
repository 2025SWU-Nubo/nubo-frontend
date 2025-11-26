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
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.layout.ContentScale
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.launch

// 온보딩 전체 페이지 수  인트로 4개 스텝 아웃트로
private const val TOTAL_PAGES = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState { TOTAL_PAGES }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        scope.launch {
            if (pagerState.currentPage > 0) {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            } else {
                onFinish()
            }
        }
    }

    // 페이지별 배경 리소스 선택 / choose background per page
    val bgResId = when (pagerState.currentPage) {
        0 -> null                                   // 인트로는 배경 없음
        TOTAL_PAGES - 1 -> R.drawable.outro_bg      // 마지막 페이지
        else -> R.drawable.onboading_step_bg        // 스텝 공통 배경
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1) ⭐ 항상 배경을 먼저 그리기
        bgResId?.let { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 2) 그 위에 Scaffold + 페이지 내용 올리기
        Scaffold(
            containerColor = Color.Transparent,   // Scaffold 자체 배경 제거
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
                    onSkipClick = { onFinish() }
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
                    2 -> OnboardingStep2Page()   // ← 여기 안의 YouTube 아이콘 그대로 유지
                    3 -> OnboardingStep3Page()   // ← 화살표 아이콘도 그대로
                    4 -> OnboardingStep4Page()
                    5 -> OnboardingOutroPage(stepNumber = 5)
                }
            }
        }
    }
}


@Composable
private fun OnboardingTopBar(
    currentPage: Int,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)      // 상단 바만 흰색 배경 고정
            .drawBehind {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
                    color = Color.Black,
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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
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
