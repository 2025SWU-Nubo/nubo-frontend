package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nubo.R
import com.example.nubo.data.model.OnboardingPage
import com.example.nubo.data.model.onboardingPages
import com.example.nubo.ui.screen.interest.OnBoardingInterestViewModel
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import com.example.nubo.ui.theme.GreyMain300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onFinish: () -> Unit,
) {
    // Pager 상태를 기억하는 부분
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()

    // 안드로이드 시스템 뒤로가기 동작 커스터마이징
    BackHandler(enabled = true) {
        scope.launch {
            if (pagerState.currentPage > 0) {
                // 온보딩 중간 페이지에서는 이전 페이지로 이동
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            } else {
                // 인트로 페이지에서는 온보딩 종료
                onFinish()
            }
        }
    }

    // 현재 페이지에 해당하는 배경 리소스 선택
    val currentPage = pagerState.currentPage
    val current = onboardingPages.getOrNull(currentPage)
    val bgResId = when (current) { // 인트로 배경 PNG
        is OnboardingPage.Step -> R.drawable.onboading_step_bg     // 2~5 공통 배경 PNG
        is OnboardingPage.Outro -> R.drawable.outro_bg   // 아웃트로 배경 PNG
        else -> null
    }

    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentPage = pagerState.currentPage,
                lastPageIndex = onboardingPages.lastIndex,
                onBackClick = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onSkipClick = {
                    // 건너뛰기 선택 시 온보딩 즉시 종료
                    onFinish()
                }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                lastPageIndex = onboardingPages.lastIndex,
                onNextClick = {
                    scope.launch {
                        if (pagerState.currentPage == onboardingPages.lastIndex) {
                            // 마지막 페이지에서 버튼을 누르면 온보딩 종료
                            onFinish()
                        } else {
                            // 나머지 페이지에서는 다음 페이지로 이동
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
            when (val page = onboardingPages[pageIndex]) {
                is OnboardingPage.Intro -> OnboardingIntroPage()
                is OnboardingPage.Step -> OnboardingStepPage(page)
                is OnboardingPage.Outro -> OnboardingOutroPage(pageIndex)
            }
        }
    }
}

// 온보딩 상단 툴바
@Composable
private fun OnboardingTopBar(
    currentPage: Int,
    lastPageIndex: Int,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Draw bottom divider line
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 인트로 페이지(0)에서는 뒤로가기 아이콘을 숨김
            if (currentPage == 0) {
                Spacer(modifier = Modifier.width(48.dp))
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = "이전")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 항상 우측에 건너뛰기 버튼 표시
            TextButton(onClick = onSkipClick) {
                Text("건너뛰기", color = Grey200, style = AppTextStyles.b2_semibold_16)
            }

        }

    }

}

// 온보딩 하단 페이지 인디케이터와 CTA 버튼
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
//        OnboardingPageIndicator(
//            currentPage = currentPage,
//            totalPages = lastPageIndex + 1
//        )



        val buttonText =
            if (currentPage == lastPageIndex) "모두 이해했어요" else "다음"

        Button(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentPage == lastPageIndex) PurpleMain500 else Purple100  ,
                contentColor = if (currentPage == lastPageIndex) Color.White else PurpleMain500  ,
            ),
            contentPadding = PaddingValues(0.dp),
            onClick = onNextClick
        ) {
            Text(text = buttonText)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 페이지 인디케이터 전체
//@Composable
//private fun OnboardingPageIndicator(
//    currentPage: Int,
//    totalPages: Int
//) {
//    Row(
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        repeat(totalPages) { index ->
//            val isSelected = index == currentPage
//            OnboardingDot(isSelected = isSelected)
//        }
//    }
//}


//// 인디케이터 하나
//@Composable
//private fun OnboardingDot(
//    isSelected: Boolean
//) {
//    val size = if (isSelected) 10.dp else 6.dp
//    val color = if (isSelected) PurpleMain500 else Grey200
//
//    Box(
//        modifier = Modifier
//            .size(size)
//            .background(color = color, shape = CircleShape)
//    )
//}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TutorialScreenPreview() {
    // TODO: Replace MaterialTheme with your app theme if you have one
    MaterialTheme {
        TutorialScreen(
            onFinish = { /* no-op for preview */ }
        )
    }
}

