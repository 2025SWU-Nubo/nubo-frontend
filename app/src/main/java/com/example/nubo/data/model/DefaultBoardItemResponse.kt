package com.example.nubo.data.model

/**
 * 기본 보드 목록 조회 응답 아이템
 * - boardId: 서버 보드 고유 식별자 (Int/Long 타입 권장)
 * - boardName: 보드 이름
 */
data class DefaultBoardItemResponse(
    val boardId: Long,
    val boardName: String
)
