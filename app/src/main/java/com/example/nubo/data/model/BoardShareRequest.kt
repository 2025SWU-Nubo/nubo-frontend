package com.example.nubo.data.model

// 공유 보드 전환 요청
data class ShareBoardRequest(
    val shared: Boolean
)

// 공유 보드 전환 응답
data class ShareBoardResponse(
    val boardId: Long,
    val shared: Boolean
)
