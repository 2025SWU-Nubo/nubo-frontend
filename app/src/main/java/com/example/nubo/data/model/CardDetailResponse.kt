package com.example.nubo.data.model

data class CardDetailResponse(
    val id: Int,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val boardSource: String,
    val boardName: String,
    val videoUrl: String,
    val videoThumbnailUrl: String,
    val createdAt: String,
    val updatedAt: String
)
