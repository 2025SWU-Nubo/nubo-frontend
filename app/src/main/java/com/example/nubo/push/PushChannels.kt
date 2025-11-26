package com.example.nubo.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 누보 앱의 모든 알림 채널을 중앙에서 관리하는 싱글톤 객체
 *
 * 알림 채널은 Android 8.0(API 26) 이상에서 필수이며,
 * 사용자가 앱 설정에서 알림 유형별로 제어할 수 있게 해줍니다.
 *
 * 사용법: Application.onCreate()에서 PushChannels.ensure(context) 호출
 */
object PushChannels {
    // ============================================
    // 알림 채널 ID 상수 정의 (서버 명세와 일치)
    // ============================================


    /** FCM 푸시: 카드가 생성되었을 때 (CARD_ADDED 타입) */
    const val CH_CARD_CREATED = "card_channel"

    /** FCM 푸시: 미시청 카드 리마인더 (REMINDER 타입) */
    const val CH_REMINDER = "reminder_channel"

    /** FCM 푸시: 보드 초대 및 관련 알림 (BOARD 타입) */
    const val CH_BOARD_INVITE = "board_channel"

    /** 포그라운드 서비스: 카드 업로드 진행 중 알림 */
    const val CH_UPLOAD_PROGRESS = "card_upload_progress_v2"

    /** FCM 푸시: 타입 미지정 또는 기타 알림 (폴백용) */
    const val CH_DEFAULT = "default_channel"

    // ============================================
    // 채널 생성 메서드
    // ============================================

    /**
     * 앱 시작 시 모든 알림 채널을 시스템에 등록합니다.
     *
     * Android 8.0(API 26) 미만에서는 채널이 필요 없으므로 아무 작업도 하지 않습니다.
     * 이미 생성된 채널에 대해 다시 호출해도 안전합니다(멱등성 보장).
     *
     * @param context Application 또는 Service 컨텍스트
     */
    fun ensure(context: Context) {
        // Android 8.0 미만에서는 알림 채널이 필요 없음
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 각 채널을 생성하여 시스템에 등록
        manager.createNotificationChannel(createCardCreatedChannel())
        manager.createNotificationChannel(createReminderChannel())
        manager.createNotificationChannel(createBoardInviteChannel())
        manager.createNotificationChannel(createUploadProgressChannel())
        manager.createNotificationChannel(createDefaultChannel())
    }

    // ============================================
    // 개별 채널 생성 (private)
    // ============================================

    /**
     * [카드 생성 알림] 채널 생성
     *
     * 중요도: HIGH
     * - 상단 배너(헤드업) 알림 표시됨
     * - 소리와 진동 울림
     * - 보드에 새 카드가 추가되면 즉시 알려야 하는 중요한 알림
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCardCreatedChannel(): NotificationChannel {
        return NotificationChannel(
            CH_CARD_CREATED,
            "카드 생성 알림",
            NotificationManager.IMPORTANCE_HIGH  // HIGH = 상단 배너 표시
        ).apply {
            description = "보드에 새 카드가 생성되면 즉시 알려드립니다."
            enableVibration(true)
            setShowBadge(true)
        }
    }

    /**
     * [미시청 카드 리마인더] 채널 생성
     *
     * 중요도: DEFAULT
     * - 알림 서랍에만 표시 (상단 배너 없음)
     * - 소리는 울리지만 화면을 방해하지 않음
     * - 급하지 않은 추천/리마인더 알림에 적합
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createReminderChannel(): NotificationChannel {
        return NotificationChannel(
            CH_REMINDER,
            "미시청 카드 추천",
            NotificationManager.IMPORTANCE_HIGH  // DEFAULT = 배너 없이 조용히 알림
        ).apply {
            description = "아직 보지 않은 카드를 정기적으로 추천해드립니다."
            enableVibration(false)  // 리마인더는 진동 없이
            setShowBadge(false)
        }
    }

    /**
     * [보드 초대/결과] 채널 생성
     *
     * 중요도: HIGH
     * - 상단 배너(헤드업) 알림 표시됨
     * - 소리와 진동 울림
     * - 사용자 액션이 필요한 초대장이므로 눈에 띄어야 함
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createBoardInviteChannel(): NotificationChannel {
        return NotificationChannel(
            CH_BOARD_INVITE,
            "보드 초대 알림",
            NotificationManager.IMPORTANCE_HIGH  // HIGH = 상단 배너 표시
        ).apply {
            description = "보드 초대와 수락/거절 결과를 알려드립니다."
            enableVibration(true)
            setShowBadge(true)
        }
    }

    /**
     * [카드 업로드 진행] 채널 생성
     *
     * 중요도: LOW
     * - 소리/진동 없음
     * - 포그라운드 서비스용 진행 상태 알림
     * - 사용자를 방해하지 않고 조용히 표시
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createUploadProgressChannel(): NotificationChannel {
        return NotificationChannel(
            CH_UPLOAD_PROGRESS,
            "카드 업로드 진행",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "영상 업로드 진행 상황을 표시합니다."
            enableVibration(false)
            setShowBadge(false)  // 진행 알림은 뱃지 불필요
        }
    }

    /**
     * [기본 알림] 채널 생성
     *
     * 중요도: DEFAULT
     * - 타입이 지정되지 않은 FCM 푸시용 폴백 채널
     * - 일반적인 알림 동작 (소리 O, 배너 X)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDefaultChannel(): NotificationChannel {
        return NotificationChannel(
            CH_DEFAULT,
            "기본 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "분류되지 않은 일반 알림입니다."
            enableVibration(true)
            setShowBadge(true)
        }
    }


    // ============================================
    // 헬퍼 메서드
    // ============================================

    /**
     * FCM 푸시 메시지 타입에 따라 적절한 채널 ID를 반환합니다.
     *
     * @param type FCM data payload의 "type" 필드 (예: "CARD_ADDED", "REMINDER", "BOARD")
     * @return 해당하는 채널 ID 문자열
     */
    fun getChannelIdForType(type: String): String {
        return when (type.uppercase().trim()) {
            "CARD_ADDED" -> CH_CARD_CREATED
            "REMINDER" -> CH_REMINDER
            "BOARD" -> CH_BOARD_INVITE
            else -> CH_DEFAULT
        }
    }

    /**
     * 채널별 사용자 친화적인 이름을 반환합니다.
     *
     * @param channelId 채널 ID 상수
     * @return 사용자에게 보여줄 채널 이름
     */
    fun getChannelName(channelId: String): String {
        return when (channelId) {
            CH_CARD_CREATED -> "카드 생성 알림"
            CH_REMINDER -> "미시청 카드 추천"
            CH_BOARD_INVITE -> "보드 초대 알림"
            CH_UPLOAD_PROGRESS -> "카드 업로드 진행"
            CH_DEFAULT -> "기본 알림"
            else -> "알림"
        }
    }

}
