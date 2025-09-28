package com.example.nubo.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nubo.MainActivity
import com.example.nubo.R
import com.example.nubo.deeplink.DeepLinkContract
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NuboFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: send token to backend
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"]
        val cardId = data["cardId"]?.toLongOrNull()
        val boardId = data["boardId"]?.toLongOrNull()
        val boardTitle = data["boardTitle"]
        val inviteToken = data["inviteToken"]

        val target: String
        val title: String
        val body: String
        val channelId: String

        when (type) {
            DeepLinkContract.NOTI_CARD_CREATED -> {
                target = DeepLinkContract.TARGET_CARD_DETAIL
                title = "카드 생성 완료"
                body = "새 카드가 저장되었어요"
                channelId = "noti_card_created"
            }
            DeepLinkContract.NOTI_CARD_UNREAD -> {
                target = DeepLinkContract.TARGET_CARD_UNREAD_LIST
                title = "미시청 카드"
                body = "아직 보지 않은 카드가 있어요"
                channelId = "noti_card_unread"
            }
            DeepLinkContract.NOTI_BOARD_INVITE -> {
                target = DeepLinkContract.TARGET_BOARD_INVITE
                title = "보드 초대"
                body = "공유 보드에 초대되었어요"
                channelId = "noti_board_invite"
            }
            else -> return
        }

        // Build tap intent to MainActivity with deep-link extras
        val tap = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW

            putExtra(DeepLinkContract.EXTRA_DEEPLINK_TARGET, target)
            putExtra(DeepLinkContract.EXTRA_NOTIFICATION_TYPE, type)
            cardId?.let { putExtra(DeepLinkContract.EXTRA_CARD_ID, it) }
            boardId?.let { putExtra(DeepLinkContract.EXTRA_BOARD_ID, it) }
            boardTitle?.let { putExtra(DeepLinkContract.EXTRA_BOARD_TITLE, it) }
            inviteToken?.let { putExtra(DeepLinkContract.EXTRA_INVITE_TOKEN, it) }

            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val requestCode = ((cardId ?: boardId) ?: System.currentTimeMillis()).toInt()
        val piFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT

        val contentPI = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(tap)
            .getPendingIntent(requestCode, piFlags)

        ensureChannel(channelId, title)

        val noti = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.basic_profile_image)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPI)
            .build()

        NotificationManagerCompat.from(this).notify(requestCode, noti)
    }

    private fun ensureChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
