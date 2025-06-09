package com.example.nubo.data.model

// 보드
data class BoardResponse(
    val id: Int,                // 서버 Board ID
    val name: String,
    val source: String,
    val sectionCount: Int,
    val cardCount: Int,
    val updatedAt: String,
    val thumbnailUrl: String?,
    val sections: List<SectionDto>,
    val cards: List<CardItemDto>
)
