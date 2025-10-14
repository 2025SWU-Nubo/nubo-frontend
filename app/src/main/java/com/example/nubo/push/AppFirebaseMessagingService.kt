package com.example.nubo.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nubo.MainActivity
import com.example.nubo.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(msg: RemoteMessage) {
        // 서버에서 보낸 data-only 페이로드를 꺼냄
        val d = msg.data

        // type 필드는 반드시 있어야 하며, 알 수 없는 타입이면 무시함
        val type = d["type"] ?: return

        // 타입별로 사용할 채널을 결정함
        val channel = when (type) {
            "card_created"      -> PushChannels.CH_CREATED   // 카드 생성 알림 채널
            "unread_recommend"  -> PushChannels.CH_UNREAD    // 미시청 추천 알림 채널
            "board_invite",
            "invite_result"     -> PushChannels.CH_INVITE    // 초대/결과 알림 채널
            else -> return                                     // 정의되지 않은 타입은 무시
        }

        // 알림 제목과 본문은 서버 템플릿 그대로 사용함
        val title = d["title"] ?: "Nubo"
        val body  = d["body"]  ?: ""

        // 메인 액티비티로 이동하는 인텐트를 구성함 (기존 딥링크 유틸이 처리할 수 있도록 데이터 전부 전달)
        val intent = Intent(this, MainActivity::class.java).apply {
            // 이미 실행 중인 액티비티를 재사용하고, 백 스택을 정리함
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // 필수: type과 notificationId를 전달하여 라우팅/읽음처리를 가능하게 함
            putExtra("type", type)
            putExtra("notificationId", d["notificationId"])

            // 선택: 카드/보드/초대 식별자 등 모든 페이로드를 그대로 전달함
            d.forEach { (k, v) -> putExtra(k, v) }

            // 초대/결과는 알림 페이지로 보내야 하므로 힌트 route를 추가함 (기존 유틸이 활용 가능)
            if (type == "board_invite" || type == "invite_result") {
                putExtra("route", "notifications")
            }
        }

        // PendingIntent를 생성하여 알림 탭 시 위 인텐트를 실행하도록 함
        val pi = PendingIntent.getActivity(
            this,
            (d["notificationId"] ?: "$type:$title").hashCode(), // 고유성을 보장하기 위해 키를 해시함
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 시스템 알림을 생성하고 표시함
        val n = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.noti_nubo) // 상태바용 단색 아이콘 필요
            .setContentTitle(title)                // 알림 제목을 지정함
            .setContentText(body)                  // 알림 본문을 지정함
            .setAutoCancel(true)                   // 탭 후 자동으로 알림을 제거함
            .setContentIntent(pi)                  // 탭 시 실행할 인텐트를 지정함
            .build()

        // 알림을 실제로 표시함 (ID는 현재 시간 기반으로 유니크하게 생성)
        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), n)
    }
}

