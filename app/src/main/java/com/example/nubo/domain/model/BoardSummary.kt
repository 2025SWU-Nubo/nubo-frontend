package com.example.nubo.domain.model

//ui 매퍼
data class BoardSummary(
    val id: Int,
    val name: String,
    val source: String,
    val sectionCount: Int,
    val cardCount: Int,
    val updatedAt: String,
    val thumbnail: String?,
    val shared: Boolean,
    val favorite: Boolean
)
