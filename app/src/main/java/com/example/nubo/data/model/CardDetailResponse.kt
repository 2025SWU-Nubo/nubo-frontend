package com.example.nubo.data.model

import com.example.nubo.data.dto.HighlightDto


data class CardDetailResponse(
    val cardId: Int,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val highlights: List<HighlightDto>? = null,
    val boardSource: String,
    val boardName: String,
    val videoUrl: String,
    val videoThumbnailUrl: String,
    val videoPlatform: String,
    val createdAt: String,
    val updatedAt: String,
    val stage: Int, // 현재 단계 (0~4)
    val berryGained: Boolean, // 열매 획득 여부
    val stageUp: Boolean, // 단계 달성 여부,
    val isFavorite: Boolean
)

