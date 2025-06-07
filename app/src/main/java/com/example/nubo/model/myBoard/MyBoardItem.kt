package com.example.nubo.model.myBoard

data class BoardItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val createdAt: String,
    val isBookmarked: Boolean = false,
    val imageUrl: String = ""
)
