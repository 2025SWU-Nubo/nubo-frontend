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

