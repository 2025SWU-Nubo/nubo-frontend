package com.example.nubo.data.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import com.example.nubo.R

sealed class OnboardingPage {
    data object Intro : OnboardingPage()
    data class Step(
        val stepNumber: Int,
        val title: String,
        val description: String,
        @DrawableRes val imageResId: Int,
        val overlays: List<StepOverlay> = emptyList()
    ) : OnboardingPage()
    data object Outro : OnboardingPage()
}

data class StepOverlay(
    @DrawableRes val imageResId: Int,
    val alignment: Alignment = Alignment.TopStart, // base image anchor
    val offsetXFraction: Float = 0f,               // 0f ~ 1f (relative to base width)
    val offsetYFraction: Float = 0f,               // 0f ~ 1f (relative to base height)
    val widthFraction: Float = 0.2f                // overlay width ratio to base width
)

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
        title = "앱 목록을 쓸어넘겨\n더보기를 •••버튼을 누르세요.",
        description = "오른쪽 끝의 ••• 버튼을 눌러주세요",
        imageResId = R.drawable.onboarding_step_3,
        overlays = listOf(
            StepOverlay(
                imageResId = R.drawable.onboading_arrow,
                alignment = Alignment.BottomStart,   // top-left as anchor
                offsetXFraction = 0.2f,          // base width * 0.65 to the right
                offsetYFraction = 1f,          // base height * 0.30 down
                widthFraction = 0.20f             // overlay width = base width * 0.20
            )
        )
    ),
    OnboardingPage.Step(
        stepNumber = 4,
        title = "앱 목록을 쓸어내려\nNubo를 누르세요",
        description = "공유 대상 목록에서 Nubo를 눌러주세요",
        imageResId = R.drawable.onboarding_step_4
    ),
    OnboardingPage.Outro
)

