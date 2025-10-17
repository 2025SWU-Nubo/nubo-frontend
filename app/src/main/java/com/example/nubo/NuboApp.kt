package com.example.nubo

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NuboApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createAppNotificationChannels()
    }

    @SuppressLint("ServiceCast")
    private fun createAppNotificationChannels() {
        // Only on Android O+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val defaultCh = NotificationChannel("default_channel", "기본 알림", NotificationManager.IMPORTANCE_HIGH)
        val reminderCh = NotificationChannel("reminder_channel", "미시청 카드", NotificationManager.IMPORTANCE_HIGH)
        val cardCh = NotificationChannel("card_channel", "카드 생성", NotificationManager.IMPORTANCE_HIGH)
        val boardCh = NotificationChannel("board_channel", "보드 알림", NotificationManager.IMPORTANCE_HIGH)

//        카드 업로드 진행 중은 알림 채널에 취소
//        val uploadProgress = NotificationChannel(
//            "card_upload_progress_v2",
//            "카드 업로드 진행",
//            NotificationManager.IMPORTANCE_DEFAULT
//        )

        // Idempotent: creating with the same ID is safe
        nm.createNotificationChannel(defaultCh)
        nm.createNotificationChannel(reminderCh)
        nm.createNotificationChannel(cardCh)
        nm.createNotificationChannel(boardCh)
//        nm.createNotificationChannel(uploadProgress)
    }
}


