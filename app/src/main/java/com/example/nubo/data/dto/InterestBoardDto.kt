package com.example.nubo.data.dto

/**
 * 서버에서 내려주는 관심 보드 항목
 * - id: 서버에서 관리하는 고유 식별자 (선택 결과 전송 시 사용)
 * - name: 보드 표시 이름 (UI에 그대로 노출)
 * - thumbnailUrl: 보드 대표 이미지 (없을 수 있어 nullable)
 */
data class InterestBoardDto(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null
)
