package com.example.nubo.data.model

// 알림 설정 페이지
data class NotificationSetRequest(

    val pushEnabled: Boolean? = null,
    val remindEnabled: Boolean? = null
)
