package com.example.nubo.push
//
//import android.Manifest
//import android.R.id.message
//import android.app.PendingIntent
//import android.content.Intent
//import androidx.annotation.RequiresPermission
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.example.nubo.MainActivity
//import com.example.nubo.R
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//import dagger.hilt.android.AndroidEntryPoint
//
//@AndroidEntryPoint
//class AppFirebaseMessagingService : FirebaseMessagingService() {
//
//    // 새로운 토큰이 발급될 때 자동으로 호출됨 (앱 재설치, wipe data 등)
//    override fun onNewToken(token: String) {
//        android.util.Log.d("FCM_NEW_TOKEN", "새 토큰 발급됨: $token")
//
//        // 1️⃣ 로컬 SharedPreferences 에 최신 토큰 캐싱
//        getSharedPreferences("push_prefs", MODE_PRIVATE)
//            .edit { putString("latest_fcm_token", token) } // apply() 자동 호출됨
//
//        // 2️⃣ 백그라운드에서 안전하게 서버 업서트
//        TokenUploadWork.enqueue(applicationContext, token, force = true)
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    override fun onMessageReceived(message: RemoteMessage) {
//
//        // 알림 채널이 항상 존재하도록 보장
//        PushChannels.ensure(this)
//
//        // 사용자 알림 권한이 꺼져있으면 표시하지 않음
//        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
//            android.util.Log.w("FCM_NOTIFY", "알림 권한이 꺼져 있어 표시되지 않음")
//            return
//        }
//        val data = message.data
//        val type = data["type"] ?: "default"
//
//        // 타입별로 사용할 채널을 결정함
//        val channel = when (type) {
//            "card_created"      -> PushChannels.CH_CREATED   // 카드 생성 알림 채널
//            "unread_recommend"  -> PushChannels.CH_UNREAD    // 미시청 추천 알림 채널
//            "board_invite",
//            "invite_result"     -> PushChannels.CH_INVITE    // 초대/결과 알림 채널
//            else -> return                                     // 정의되지 않은 타입은 무시
//        }
//
//        // 알림 제목과 본문은 서버 템플릿 그대로 사용함
//        val title = data["title"] ?: "Nubo"
//        val body  = data["body"]  ?: ""
//
//        // 메인 액티비티로 이동하는 인텐트를 구성함 (기존 딥링크 유틸이 처리할 수 있도록 데이터 전부 전달)
//        val intent = Intent(this, MainActivity::class.java).apply {
//            // 이미 실행 중인 액티비티를 재사용하고, 백 스택을 정리함
//            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
//
//            // 필수: type과 notificationId를 전달하여 라우팅/읽음처리를 가능하게 함
//            putExtra("type", type)
//            putExtra("notificationId", data["notificationId"])
//
//            // 선택: 카드/보드/초대 식별자 등 모든 페이로드를 그대로 전달함
//            data.forEach { (k, v) -> putExtra(k, v) }
//
//            // 초대/결과는 알림 페이지로 보내야 하므로 힌트 route를 추가함 (기존 유틸이 활용 가능)
//            if (type == "board_invite" || type == "invite_result") {
//                putExtra("route", "notifications")
//            }
//        }
//
//        // PendingIntent를 생성하여 알림 탭 시 위 인텐트를 실행하도록 함
//        val pi = PendingIntent.getActivity(
//            this,
//            (data["notificationId"] ?: "$type:$title").hashCode(), // 고유성을 보장하기 위해 키를 해시함
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // 시스템 알림을 생성하고 표시함
//        val n = NotificationCompat.Builder(this, channel)
//            .setSmallIcon(R.drawable.nubo_symbol) // 상태바용 단색 아이콘 필요
//            .setContentTitle(title)                // 알림 제목을 지정함
//            .setContentText(body)                  // 알림 본문을 지정함
//            .setAutoCancel(true)                   // 탭 후 자동으로 알림을 제거함
//            .setContentIntent(pi)                  // 탭 시 실행할 인텐트를 지정함
//            .build()
//
//        // 알림을 실제로 표시함 (ID는 현재 시간 기반으로 유니크하게 생성)
//        NotificationManagerCompat.from(this)
//            .notify(System.currentTimeMillis().toInt(), n)
//    }
//
//    /**
//     * 서비스가 처음 시작될 때 채널이 존재하지 않으면 생성
//     * (안드로이드 13+에서는 필요)
//     */
//    override fun onCreate() {
//        super.onCreate()
//        PushChannels.ensure(this)
//    }
//}
//
