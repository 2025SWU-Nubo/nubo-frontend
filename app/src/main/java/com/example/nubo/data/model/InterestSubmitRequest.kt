package com.example.nubo.data.model

/**
 * 관심사 설정 요청 바디
 * - skip=true  : 건너뛰기 (selectedBoardIds 생략)
 * - skip=false : 보드 선택( selectedBoardIds 필수 )
 */
data class InterestSubmitRequest(
    val skip: Boolean,
    val selectedBoardIds: List<Long>? = null
)
