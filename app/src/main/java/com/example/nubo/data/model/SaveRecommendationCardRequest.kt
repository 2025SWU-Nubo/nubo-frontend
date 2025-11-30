package com.example.nubo.data.model

data class SaveRecommendationCardRequest(
    val recommendationCardId: Int,
    val boardIds: List<Int>? = null // 없으면 AI 보드로 자동 저장
)
