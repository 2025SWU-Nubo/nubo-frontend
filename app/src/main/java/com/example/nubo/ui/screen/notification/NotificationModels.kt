package com.example.nubo.ui.screen.notification

// 스크린에서 카드 라벨/아이콘/동작을 결정하기 위한 화면 전용 타입
enum class NotiType { UnviewedReminder, NewCard, Invite, System }

// 카드 하단 액션(초대 수락/거절 버튼, 'N건 더보기' 등)
sealed class NotiAction {
    data class Invite(
        val acceptLabel: String = "수락",
        val rejectLabel: String = "거절"
    ) : NotiAction()

    data class ShowMore(val count: Int) : NotiAction()
}

// 리스트에 렌더링할 1개의 알림 아이템
data class NotificationItem(
    val notificationId: String,
    val title: String,
    val message: String,  //body
    val timeLabel: String,
    val type: NotiType,
    val unread: Boolean,
    val action: NotiAction? = null,
    val boardId: String? = null,
    val cardId: String? = null,
    val invitationId: Int? = null
)

// 화면 전체 상태(Recent/Past 섹션 + 로딩)
data class NotificationFeedState(
    val recent: List<NotificationItem> = emptyList(),
    val past: List<NotificationItem> = emptyList(),
    val loading: Boolean = false,
    val actionLoadingIds: Set<String> = emptySet()
)

