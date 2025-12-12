package com.example.nubo.data.model

import okio.Source

// 보드 내 아이템 응답
data class BoardItemResponse(
    val id:Long,
    val name: String,
    val boardType:String,
    val source: String,
    val shared: Boolean,
    val favorite: Boolean,
    val owner:Boolean
)

// 즐겨찾기 변경 요청 바디
data class FavoriteRequest(
    val favorite: Boolean
)

// 즐겨찾기 변경 응답
data class FavoriteResponse(
    val boardId: Long,
    val favorite: Boolean
)
