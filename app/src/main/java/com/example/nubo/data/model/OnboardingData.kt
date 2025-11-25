package com.example.nubo.data.model

import com.example.nubo.R

sealed class OnboardingPage {
    data object Intro : OnboardingPage()
    data class Step(
        val stepNumber: Int,
        val title: String,
        val description: String,
        val imageResId: Int
    ) : OnboardingPage()
    data object Outro : OnboardingPage()
}

val onboardingPages: List<OnboardingPage> = listOf(
    OnboardingPage.Intro,
    OnboardingPage.Step(
        stepNumber = 1,
        title = "1 버튼을 누르세요",
        description = "영상 화면에서 ••• 버튼을 눌러주세요",
        imageResId = R.drawable.onboarding_step_1
    ),
    OnboardingPage.Step(
        stepNumber = 2,
        title = "공유 버튼을 누르세요",
        description = "하단 공유 메뉴를 열어주세요",
        imageResId = R.drawable.onboarding_step_2
    ),
    OnboardingPage.Step(
        stepNumber = 3,
        title = "앱 목록을 쓸어넘겨 더보기를 누르세요",
        description = "오른쪽 끝의 ••• 버튼을 눌러주세요",
        imageResId = R.drawable.onboarding_step_3
    ),
    OnboardingPage.Step(
        stepNumber = 4,
        title = "앱 목록에서 Nubo를 선택하세요",
        description = "공유 대상 목록에서 Nubo를 눌러주세요",
        imageResId = R.drawable.onboarding_step_4
    ),
    OnboardingPage.Outro
)

