package com.example.nubo.data.model

/**
 * 관심사 설정 응답 바디
 * - completed: 서버 기준 온보딩 완료 여부 (true면 홈 화면 등으로 분기)
 * - selectedCount: 설정된 보드 개수 (건너뛰기/중복호출(idempotent) 시 0일 수 있음)
 */
data class InterestSubmitResponse(
    val completed: Boolean,
    val selectedCount: Int
)
