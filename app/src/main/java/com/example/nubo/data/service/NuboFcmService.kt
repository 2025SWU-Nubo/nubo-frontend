package com.example.nubo.data.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nubo.MainActivity
import com.example.nubo.R
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.NotificationRepository
import com.example.nubo.deeplink.DeepLinkContract
import com.example.nubo.ui.screen.onBoardingLogin.OnBoardingLoginActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit

@AndroidEntryPoint
class NuboFcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var authRepository: AuthRepository
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("FCM_SVC", "onCreate pid=${android.os.Process.myPid()}")
    }

    // FCM 토큰이 갱신될 때 호출됨 (앱 재설치/데이터삭제/프로젝트 변경 등)
    @SuppressLint("HardwareIds")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SVC", "onNewToken=$token")
        if (token.isBlank()) return

        // 로그인 직후 재등록을 위한 최신 토큰 캐시 (로그인 상태가 아닐 수 있으므로)
        getSharedPreferences("push_prefs", MODE_PRIVATE)
            .edit() {
                putString("latest_fcm_token", token)
            }

        // 로그인 이전일 수 있으므로 실패해도 로그만 남기고 종료
        ioScope.launch {
            runCatching { notificationRepository.registerDeviceTokenIfNeeded(token) }
                .onFailure { Log.w("FCM_SVC", "registerDeviceTokenIfNeeded failed (will retry after login)", it) }
        }
    }

    /**
     * 서버가 보낸 푸시 수신 지점
     * - 포그라운드: data-only 또는 notification+data 모두 여기로 진입 → 우리가 직접 표시
     * - 백그라운드/종료: notification-only는 OS가 표시하고 콜백이 안 올 수 있음(정상)
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val noti = message.notification
        Log.d(
            "FCM_SVC",
            "onMessageReceived from=${message.from} data=$data notificationTitle=${noti?.title} body=${noti?.body}"
        )

        // 1) payload 파싱 (camelCase + snake_case를 모두 수용)
        val payload = parsePayload(message)

        // 2) 탭 시 MainActivity로 이동할 Intent 구성 (딥링크/파라미터 포함)
        val contentPI = buildContentPendingIntent(payload)

        // 3) 채널 보장 (Android 8+)
        ensureChannel(payload.channelId, payload.channelName)

        // 4) 알림 빌드/표시
        val notification = NotificationCompat.Builder(this, payload.channelId)
            .setSmallIcon(R.drawable.nubo_symbol) // TODO: 운영 아이콘으로 교체 가능
            .setContentTitle(payload.title)               // 서버 title 우선
            .setContentText(payload.body)                 // 서버 body 우선
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPI)
            .build()

        // Android 13+ 알림 권한 체크 (콜백 자체는 권한과 무관하나, 표시에는 필요)
        val canNotify = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        android.util.Log.d("FCM_SVC", "willNotify=$canNotify ch=${payload.channelId} title=${payload.title}")

        if (canNotify) {
            NotificationManagerCompat.from(this).notify(payload.notificationId, notification)
        } else {
            android.util.Log.w("FCM_SVC", "POST_NOTIFICATIONS not granted; skip notify")
        }
    }

    private fun parsePayload(message: RemoteMessage): Payload {
        val d = message.data
        fun pick(vararg keys: String) = keys.firstNotNullOfOrNull { d[it] }

        val type = (pick("type", "TYPE") ?: "").trim().uppercase()
        val title = pick("title", "TITLE") ?: message.notification?.title ?: /* fallback */ "알림"
        val body  = pick("body", "BODY")  ?: message.notification?.body  ?: /* fallback */ "새 알림이 있습니다."

        val cardIdStr  = pick("card_id", "cardId")         // ← 그대로 String
        val boardIdStr = pick("board_id", "boardId")       // ← 그대로 String
        val boardTitle = pick("board_title", "boardTitle")
        val invitationId = pick("invitation_id", "invitationId")

        val serverChannel = pick("channel_id", "android_channel_id")
        val (target, channelName, defaultChannel) = when (type) {
            "CARD_ADDED" -> Triple(DeepLinkContract.TARGET_CARD_DETAIL, "카드 알림", "card_channel")
            "REMINDER"   -> Triple(DeepLinkContract.TARGET_CARD_UNREAD_LIST, "리마인더", "reminder_channel")
            "BOARD"      -> Triple(DeepLinkContract.TARGET_BOARD_INVITE, "보드 알림", "board_channel")
            else         -> Triple(DeepLinkContract.TARGET_BOARD_INVITE, "일반 알림", "default_channel")
        }
        val channelId = serverChannel ?: defaultChannel

        val notificationId = listOfNotNull(type, cardIdStr, boardIdStr, invitationId)
            .joinToString("#").hashCode()

        return Payload(type, title, body, cardIdStr, boardIdStr, boardTitle, invitationId,
            channelId, channelName, target, notificationId)
    }


    // 알림 탭 시 메인으로 이동(딥링크 파라미터 포함)
    private fun buildContentPendingIntent(p: Payload): PendingIntent {
        val tap = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW

            // 기존 태스크 앞으로 끌고오고(onTop), 기존 인스턴스로 onNewIntent 유도
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            // ---- DeepLink/FCM extras (문자열 우선 + 호환 키 전부) ----
            putExtra(DeepLinkContract.EXTRA_DEEPLINK_TARGET, p.target)
            putExtra(DeepLinkContract.EXTRA_NOTIFICATION_TYPE, p.type)
            putExtra("type", p.type); putExtra("TYPE", p.type)

            p.cardIdStr?.let { s ->
                putExtra("card_id", s); putExtra("cardId", s)     // data 키는 문자열 유지
                s.toLongOrNull()?.let { putExtra(DeepLinkContract.EXTRA_CARD_ID, it) } // 컨트랙트 키는 Long 가능하면 추가
            }
            p.boardIdStr?.let { s ->
                putExtra("board_id", s); putExtra("boardId", s)
                s.toLongOrNull()?.let { putExtra(DeepLinkContract.EXTRA_BOARD_ID, it) }
            }
            p.boardTitle?.let {
                putExtra("board_title", it); putExtra("boardTitle", it)
                putExtra(DeepLinkContract.EXTRA_BOARD_TITLE, it)
            }
            p.invitationId?.let {
                putExtra("invitation_id", it); putExtra("invitationId", it)
                putExtra(DeepLinkContract.EXTRA_INVITE_TOKEN, it)
            }
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // TaskStackBuilder 제거 → 단순/안정
        return PendingIntent.getActivity(this, p.notificationId, tap, flags)
    }

    // 알림 채널 보장 (Android 8+). 이미 존재하면 create가 no-op
    private fun ensureChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // 채널 조회 (기존 생성된 것이 있으면 그걸 씀)
        var ch = nm.getNotificationChannel(id)
        if (ch == null) {
            ch = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Nubo notifications"
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(ch)
        }
        android.util.Log.d("FCM_SVC", "channel id=$id importance=${ch.importance} enabled=${nm.areNotificationsEnabled()}")
    }


    // payload DTO
    private data class Payload(
        val type: String,
        val title: String,
        val body: String,
        val cardIdStr: String?,
        val boardIdStr: String?,
        val boardTitle: String?,
        val invitationId: String?,
        val channelId: String,
        val channelName: String,
        val target: String,
        val notificationId: Int
    )
}
