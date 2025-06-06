package com.example.nubo.data.model

// 섹션
data class SectionDto(
    val id: Int,
    val name: String,
    val source: String,
    val updatedAt: String,
    val sectionCount: Int,
    val cardCount: Int,
    val shared: Boolean,
    val favorite: Boolean
)
