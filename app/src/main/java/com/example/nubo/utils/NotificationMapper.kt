package com.example.nubo.utils

import com.example.nubo.data.model.AppNotification
import com.example.nubo.data.model.AppNotificationType
import com.example.nubo.data.model.NotificationDto
import com.example.nubo.data.model.ServerNotificationType
import com.example.nubo.ui.screen.notification.NotificationFeedState
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

private val KST: ZoneId = ZoneId.of("Asia/Seoul")

/**
 * 다양한 서버 포맷(createdAt)을 안전하게 Instant로 변환
 * 허용 예:
 *  - 2025-10-01T00:30:00Z
 *  - 2025-10-01T09:30:00+09:00
 *  - 2025-10-01T09:30:00           (타임존 없음 → KST 가정)
 *  - 2025-10-01T09:30:00+0900     (콜론 없는 오프셋)
 *  - 2025-10-01T09:30:00.123+09:00 (소수 초 유무)
 */
private fun parseInstantFlexible(raw: String, zone: ZoneId = KST): Instant {
    val s = raw.trim()

    // 1) 표준 Instant (…Z)
    runCatching { return Instant.parse(s) }

    // 2) 오프셋 포함 ISO (자동 포맷 추론)
    runCatching { return OffsetDateTime.parse(s).toInstant() }

    // 3) 로컬 ISO (타임존 없음 → zone 가정)
    runCatching { return LocalDateTime.parse(s).atZone(zone).toInstant() }

    // 4) +0900 같이 콜론 없는 오프셋 보정 후 재시도
    val fixedOffset = s.replace(Regex("""([+-]\d{2})(\d{2})$"""), "$1:$2") // +0900 → +09:00
    runCatching { return OffsetDateTime.parse(fixedOffset).toInstant() }

    // 5) 소수초 제거 후 재시도
    val dropMillis = s.replace(Regex("""\.\d{1,9}"""), "")
    runCatching { return OffsetDateTime.parse(dropMillis).toInstant() }
    runCatching { return LocalDateTime.parse(dropMillis).atZone(zone).toInstant() }

    // 6) 최후 보루: 지금 시각
    return Instant.now()
}

private fun toAppType(dto: NotificationDto): AppNotificationType {
    return when (dto.type) {
        ServerNotificationType.CARD_ADDED -> AppNotificationType.CARD_CREATED
        ServerNotificationType.REMINDER   -> AppNotificationType.UNREAD_RECOMMEND
        ServerNotificationType.BOARD      -> {
            // 초대 ID가 있으면 초대 알림으로 분류
            if (dto.invitationId != null) return AppNotificationType.BOARD_INVITE

            val title = dto.title.lowercase(Locale.KOREAN)
            val body  = dto.body.lowercase(Locale.KOREAN)
            val all   = "$title $body"

            val isAccept = Regex("수락|승인|허용").containsMatchIn(all)
            val isAdded  = Regex("보드\\s*추가|내\\s*보드에\\s*추가").containsMatchIn(all)

            when {
                isAccept -> AppNotificationType.INVITE_RESULT
                isAdded  -> AppNotificationType.BOARD_ADDED
                else     -> AppNotificationType.UNKNOWN
            }
        }
    }
}

// 단일 DTO → App 모델
fun NotificationDto.toAppModel(defaultZone: ZoneId = KST): AppNotification {
    return AppNotification(
        notificationId = this.notificationId.toString(),
        type           = toAppType(this),
        createdAt      = parseInstantFlexible(this.createdAt, defaultZone),
        read           = this.read,
        title          = this.title,
        body           = this.body,
        cardId         = this.cardId?.toString(),
        boardId        = this.boardId?.toString(),
        invitationId   = this.invitationId?.toString()
    )
}

// 리스트 변환
fun List<NotificationDto>.toAppModels(defaultZone: ZoneId = KST): List<AppNotification> =
    this.map { it.toAppModel(defaultZone) }
