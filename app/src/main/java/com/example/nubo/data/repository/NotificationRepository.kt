package com.example.nubo.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.nubo.data.model.AppNotification
import com.example.nubo.data.model.DeleteDeviceTokenRequest
import com.example.nubo.data.model.NotificationDto
import com.example.nubo.data.model.NotificationListResponse
import com.example.nubo.data.model.RegisterDeviceTokenRequest
import com.example.nubo.data.network.NotificationService
import com.example.nubo.ui.screen.notification.NotificationFeedState
import com.example.nubo.ui.screen.notification.toFeedState
import com.example.nubo.utils.toAppModels
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import androidx.core.content.edit

class NotificationRepository @Inject constructor(
    private val api: NotificationService,
    @ApplicationContext private val context: Context,
){
    private val prefs = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_REGISTERED_TOKEN = "registered_fcm_token"
    }

    @Volatile private var lastRegistered: String? = null

    // NotificationRepository.kt
    sealed class RegisterOutcome {
        object SkippedAlready : RegisterOutcome()
        object SkippedBlank : RegisterOutcome()
        object Success : RegisterOutcome()
        data class Failure(val error: Throwable) : RegisterOutcome()
    }

    suspend fun registerDeviceTokenIfNeeded(fcmToken: String): RegisterOutcome {
        if (fcmToken.isBlank()) {
            android.util.Log.w("FCM_REG", "skip: blank token")
            return RegisterOutcome.SkippedBlank
        }

        val lastInMem = lastRegistered
        val lastInPrefs = prefs.getString(KEY_REGISTERED_TOKEN, null)

        if (fcmToken == lastInMem || fcmToken == lastInPrefs) {
            android.util.Log.d("FCM_REG", "skip: already registered (mem=${lastInMem != null}, prefs=${lastInPrefs != null})")
            return RegisterOutcome.SkippedAlready
        }

        android.util.Log.d("FCM_REG", "try register (${fcmToken.take(12)}...)")
        return runCatching {
            api.registerDeviceToken(RegisterDeviceTokenRequest(fcmToken))
            lastRegistered = fcmToken
            prefs.edit { putString(KEY_REGISTERED_TOKEN, fcmToken) }
            android.util.Log.d("FCM_REG", "success 200 (${fcmToken.take(12)}...)")
            RegisterOutcome.Success
        }.getOrElse { e ->
            android.util.Log.w("FCM_REG","fail: ${e.message}")
            RegisterOutcome.Failure(e)
        }
    }

    suspend fun deleteDeviceToken(fcmToken: String) {
        runCatching {
            api.deleteDeviceToken(DeleteDeviceTokenRequest(deviceToken = fcmToken))
            prefs.edit() { remove(KEY_REGISTERED_TOKEN) }
            lastRegistered = null
        }.onFailure { e ->
            android.util.Log.e("FCM","deleteDeviceToken failed", e)
        }
    }

    // 최근 7일 알림을 서버에서 가져와 화면에서 바로 쓰는 FeedState까지 만들어 반환함
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadFeed(
        now: Instant = Instant.now(),
        defaultZone: ZoneId = ZoneId.of("Asia/Seoul")
    ): NotificationFeedState {
        // 서버는 배열(JSON 리스트)로 내려줌
        val serverList: List<NotificationDto> = api.getNotifications()

        // DTO -> App 모델로 변환 (타임존 기본값은 KST)
        val appList: List<AppNotification> = serverList.toAppModels(defaultZone)

        // 0~2일/3~7일 섹션 분리 + 미시청 추천 접기 규칙 적용
        return appList.toFeedState(now)
    }

    // 알림 목록을 서버에서 받아 앱 모델로 변환해 반환함
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getNotifications(
        defaultZone: ZoneId = ZoneId.of("Asia/Seoul")
    ): List<AppNotification> {
        // 서버는 배열(JSON 리스트)로 내려줌
        val serverList: List<NotificationDto> = api.getNotifications()
        // DTO → App 모델 변환(타임존 기본값은 KST)
        return serverList.toAppModels(defaultZone)
    }

    // 특정 알림 읽음 처리
    suspend fun markRead(id: String) {
        api.markRead(id)
    }

    // 모든 알림 일괄 읽음 처리
    suspend fun markAllRead() {
        api.markAllRead()
    }
}
