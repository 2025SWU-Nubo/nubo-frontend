package com.example.nubo.data.model

import java.time.Instant

// 화면에서 공통으로 쓰기 쉬운 내부 알림 타입을 정의함
enum class AppNotificationType {
    // 카드 생성 완료(카드 상세로 이동하는 용도임)
    CARD_CREATED,
    // 미시청 카드 리마인드(카드 상세로 이동하는 용도임)
    UNREAD_RECOMMEND,
    // 보드 초대(알림 페이지에서 수락/거절하는 용도임)
    BOARD_INVITE,
    // 초대 결과(알림 페이지에서 결과 확인하는 용도임)
    INVITE_RESULT,
    // 보드 추가(선택적으로 보드 상세로 이동할 수 있음)
    BOARD_ADDED,
    // 분류 불가(안정성을 위한 폴백 타입임)
    UNKNOWN
}

// UI에서 바로 사용할 정규화된 알림 모델임
data class AppNotification(
    // 알림 레코드 고유 식별자임
    val notificationId: String,
    // 내부 표준화된 알림 타입임
    val type: AppNotificationType,
    // 생성 시각(Instant)임
    val createdAt: Instant,
    // 읽음 시각이 있을 수 있으므로 nullable로 둠(서버 응답엔 불리언만 있었으나 내부 상태 확장을 고려함)
    val read: Boolean,
    // 화면 표시에 사용할 제목 문자열임
    val title: String,
    // 화면 표시에 사용할 본문 문자열임
    val body: String,
    // 라우팅에 필요한 카드 식별자임
    val cardId: String?,
    // 라우팅에 필요한 보드 식별자임
    val boardId: String?,
    // 초대 수락/거절에 필요한 초대 식별자임
    val invitationId: Int?
)
