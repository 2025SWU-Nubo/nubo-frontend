package com.example.nubo.model.card

import com.example.nubo.data.dto.HighlightDto

data class CardDetailItem(
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
    val isFavorite: Boolean
)


