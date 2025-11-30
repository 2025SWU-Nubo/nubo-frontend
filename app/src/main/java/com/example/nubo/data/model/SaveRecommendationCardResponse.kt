package com.example.nubo.data.model

data class SaveRecommendationCardResponse(
    val cardId: Int,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val videoId: String,
    val videoThumbnailUrl: String,
    val boardIds: List<Long>,
    val favorite: Boolean
)
