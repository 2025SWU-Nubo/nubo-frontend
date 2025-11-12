package com.example.nubo

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.nubo.push.PushChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NuboApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 앱 시작 시 한 번만 호출하여 0+ 기기에서 알림 채널 보장 생성
        PushChannels.ensure(this)
    }
}


