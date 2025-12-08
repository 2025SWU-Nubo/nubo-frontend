package com.example.nubo.data.model

import com.example.nubo.data.dto.HighlightDto

data class RecommendCardDetailResponse(
    val recommendationCardId: Int,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val videoUrl: String,
    val videoThumbnailUrl: String,
    val videoPlatform: String,
    val aiCategoryName: String,
    val createdAt: String,
    val updatedAt: String,
    val username: String,
    val matchPercent: Int
)
