package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

enum class ServerNotificationType {
    @SerializedName("REMINDER") REMINDER,      // 미시청 카드 리마인드
    @SerializedName("BOARD") BOARD,            // 보드 관련(초대/수락/보드추가)
    @SerializedName("CARD_ADDED") CARD_ADDED   // 카드 생성 완료
}

// 서버에서 내려주는 알림 항목을 담는 DTO임
data class NotificationDto(
    val notificationId: Long,                    // 알림 ID
    val type: ServerNotificationType,// 알림 대분류 타입
    val title: String,               // 제목
    val body: String,                // 본문
    val read: Boolean,               // 읽음 여부
    val createdAt: String,           // 예: 2025-10-01T09:30:00 (타임존 표기 없음)
    val boardId: String?  = null  ,              // 보드 ID(옵션)
    val invitationId: Int?,         // 초대 ID(옵션)
    val cardId: String? = null         // 카드 ID(옵션, 서버가 주면 상세로 이동 가능)
)

// 알림 목록 응답을 담는 DTO임
data class NotificationListResponse(
    val items: List<NotificationDto>// 알림 목록
)

// 디바이스 토큰 등록 요청 본문임
data class RegisterDeviceTokenRequest(
    val token: String        // 앱 버전명/코드
)

// 디바이스 토큰 삭제 요청 본문임
data class DeleteDeviceTokenRequest(
    val deviceToken: String         // 삭제할 FCM 디바이스 토큰
)
