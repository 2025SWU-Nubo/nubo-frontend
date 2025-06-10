package com.example.nubo.data.model

import com.example.nubo.model.card.CardDetailDialogItem

data class CardDetailResponse(
    val id: Int,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val boardSource: String,
    val boardName: String,
    val videoUrl: String,
    val videoThumbnailUrl: String,
    val videoPlatform: String,
    val createdAt: String,
    val updatedAt: String
)

