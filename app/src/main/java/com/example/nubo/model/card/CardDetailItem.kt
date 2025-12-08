package com.example.nubo.model.card

import com.example.nubo.data.dto.HighlightDto

data class CardDetailItem(
    val cardId: Int,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val highlights: List<HighlightDto>? = null,
    val boardName: String,
    val videoUrl: String,
    val videoThumbnailUrl: String,
    val videoPlatform: String,
    val createdAt: String,
    val updatedAt: String,
    val isFavorite: Boolean,
    // 레벨업 및 열매 관련 정보
    val stage: Int,
    val stageUp: Boolean,
    val berryGained: Boolean
)

data class  RecommendCardDetailItem(
    val recommendationCardId : Int,
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
    val matchPercent : Int
)


