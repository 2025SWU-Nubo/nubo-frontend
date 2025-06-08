package com.example.nubo.data.model

data class CardUploadResponse(
    val id: Long,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val videoId: String,
    val videoThumbnailUrl: String,
    val boardId: Long,
    val favorite: Boolean
)
