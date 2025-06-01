package com.example.nubo.data.model

data class BoardItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val createdAt: String,
    val isBookmarked: Boolean = false,
    val imageUrl: String = ""
)
