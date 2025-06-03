package com.example.nubo.data.model

data class BoardResponse(
    val id: Int,                // 서버 Board ID
    val name: String,
    val source: String,
    val sectionCount: Int,
    val cardCount: Int,
    val updatedAt: String
)
