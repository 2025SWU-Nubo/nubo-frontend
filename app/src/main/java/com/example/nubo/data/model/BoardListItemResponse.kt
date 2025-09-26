package com.example.nubo.data.model

data class BoardListItemResponse(
    val id: Int,
    val name: String,
    val source: String,
    val updatedAt: String,
    val sectionCount: Int,
    val cardCount: Int,
    val videoThumbnailUrl: String?,
    val shared: Boolean,
    val favorite: Boolean
)
