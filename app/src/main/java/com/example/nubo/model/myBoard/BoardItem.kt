package com.example.nubo.model.myBoard

data class BoardItem(
    val id: Int,                       // 앱 내부 순번 ID
    val serverBoardId: Int,            // 서버에서 받은 ID
    val title: String,
    val subtitle: String,
    val createdAt: String,
    val isBookmarked: Boolean = false,
    val imageUrl: String = "",
    val source: String = ""
)
