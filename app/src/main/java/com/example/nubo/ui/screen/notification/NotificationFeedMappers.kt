package com.example.nubo.ui.screen.notification

import com.example.nubo.data.model.AppNotification
import com.example.nubo.data.model.AppNotificationType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

private val KST: ZoneId = ZoneId.of("Asia/Seoul")

// 상대 시각 라벨 생성: 지금/분 전/시간 전/월 일
private fun Instant.toRelativeLabel(now: Instant, zone: ZoneId = KST): String {
    val diff = Duration.between(this, now)
    val minutes = diff.toMinutes()
    val hours = diff.toHours()

    return when {
        minutes < 1   -> "지금"
        minutes < 60  -> "${minutes}분 전"
        hours < 24    -> "${hours}시간 전"
        else -> {
            val ld = this.atZone(zone).toLocalDate()
            "${ld.monthValue}월 ${ld.dayOfMonth}일"
        }
    }
}

// App → UI 아이템 변환
private fun AppNotification.toUiItem(now: Instant): NotificationItem {
    val type = when (this.type) {
        AppNotificationType.UNREAD_RECOMMEND -> NotiType.UnviewedReminder
        AppNotificationType.CARD_CREATED     -> NotiType.NewCard
        AppNotificationType.BOARD_INVITE     -> NotiType.Invite
        AppNotificationType.INVITE_RESULT,
        AppNotificationType.BOARD_ADDED,
        AppNotificationType.UNKNOWN          -> NotiType.System
    }

    val action = when (this.type) {
        AppNotificationType.BOARD_INVITE -> NotiAction.Invite()
        else -> null
    }

    return NotificationItem(
        notificationId = this.notificationId,
        title = this.title,
        message = this.body,
        timeLabel = this.createdAt.toRelativeLabel(now),
        type = type,
        unread = !this.read,
        action = action,
        boardId = this.boardId,
        cardId = this.cardId,
        invitationId = this.invitationId
    )
}

// 미시청 리마인드 2건 이상이면 1건만 남기고 나머지는 제목에 (+N건 더보기)
private fun foldUnreadRemindersApp(section: List<AppNotification>): List<AppNotification> {
    val recs = section.filter { it.type == AppNotificationType.UNREAD_RECOMMEND }
    if (recs.size <= 1) return section

    val latest = recs.maxBy { it.createdAt } // 가장 최신 1건
    val folded = latest.copy(title = "${latest.title} (+${recs.size - 1}건 더보기)")
    return section.filter { it.type != AppNotificationType.UNREAD_RECOMMEND } + folded
}

/** AppNotification 리스트를 화면 섹션 상태로 변환 */
fun List<AppNotification>.toFeedState(
    now: Instant = Instant.now(),
    zone: ZoneId = KST
): NotificationFeedState {
    val sorted = this.sortedByDescending { it.createdAt }

    val d2 = now.minus(Duration.ofDays(2))
    val d7 = now.minus(Duration.ofDays(7))

    // 0~2일 / 3~7일 분리
    val recentApp = sorted.filter { it.createdAt.isAfter(d2) }
    val pastApp   = sorted.filter { it.createdAt <= d2 && it.createdAt.isAfter(d7) }

    // 미시청 접기 규칙은 App 단계에서 적용
    val recentFolded = foldUnreadRemindersApp(recentApp)
    val pastFolded   = foldUnreadRemindersApp(pastApp)

    // UI 아이템으로 변환
    val recentUi = recentFolded.map { it.toUiItem(now) }
    val pastUi   = pastFolded.map { it.toUiItem(now) }

    return NotificationFeedState(
        recent = recentUi,
        past   = pastUi,
        loading = false
    )
}
