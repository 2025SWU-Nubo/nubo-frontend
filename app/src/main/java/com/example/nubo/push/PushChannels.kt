package com.example.nubo.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build


object PushChannels {
    const val CH_CREATED = "nubo_card_created"          // 카드 생성 알림용 채널 ID
    const val CH_UNREAD  = "nubo_unread_recommend"      // 미시청 추천 알림용 채널 ID
    const val CH_INVITE  = "nubo_board_invite"          // 초대 및 결과 알림용 채널 ID

    // 앱/서비스 시작 시 반드시 호출되어야 함
    fun ensure(context: Context) {
        // 안드로이드 8.0 이상에서만 채널 생성이 필요함
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // 시스템에서 NotificationManager를 가져옴
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 카드 생성 알림 채널을 생성함 (중요도 보통)
        val created = NotificationChannel(
            CH_CREATED,
            "카드 생성 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            // 뱃지와 진동을 허용함
            enableVibration(true)
            setShowBadge(true)
            description = "보드에 새 카드가 생성되면 알려줍니다."
        }

        // 미시청 추천 알림 채널을 생성함 (중요도 보통)
        val unread = NotificationChannel(
            CH_UNREAD,
            "미시청 카드 추천",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            // 뱃지와 진동을 허용함
            enableVibration(true)
            setShowBadge(true)
            description = "아직 보지 않은 카드를 추천해드립니다."
        }

        // 초대/결과 알림 채널을 생성함 (중요도 높음)
        val invite = NotificationChannel(
            CH_INVITE,
            "보드 초대/결과 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            // 뱃지와 진동을 허용함
            enableVibration(true)
            setShowBadge(true)
            description = "보드 초대와 초대 결과를 알려드립니다."
        }

        // 위에서 정의한 세 채널을 시스템에 등록함
        mgr.createNotificationChannel(created)
        mgr.createNotificationChannel(unread)
        mgr.createNotificationChannel(invite)
    }
}
