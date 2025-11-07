package com.example.nubo.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.nubo.data.model.AppNotification
import com.example.nubo.data.dto.DeleteDeviceTokenRequest
import com.example.nubo.data.dto.NotificationDto
import com.example.nubo.data.dto.RegisterDeviceTokenRequest
import com.example.nubo.data.network.NotificationService
import com.example.nubo.ui.screen.notification.NotificationFeedState
import com.example.nubo.ui.screen.notification.toFeedState
import com.example.nubo.utils.toAppModels
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import androidx.core.content.edit


/**
 * 푸시 토큰 등록/삭제 + 알림 목록/읽음 처리 레포지토리
 * - registerDeviceTokenIfNeeded(): TTL + force 기반 업서트
 * - deleteRegisteredTokenIfAny(): 서버/로컬 상태 정리
 */
class NotificationRepository @Inject constructor(
    private val api: NotificationService,
    @ApplicationContext private val context: Context,
){
    private val prefs = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_REGISTERED_TOKEN = "registered_fcm_token"
        private const val KEY_REGISTERED_AT = "registered_at_epoch"
    }

    @Volatile private var lastRegistered: String? = null

    // NotificationRepository.kt
    sealed class RegisterOutcome {
        object SkippedAlready : RegisterOutcome()
        object SkippedBlank : RegisterOutcome()
        object Success : RegisterOutcome()
        data class Failure(val error: Throwable) : RegisterOutcome()
    }

    /**
     * FCM 토큰 업서트(등록/갱신)
     * - 동일 토큰을 너무 자주 보내지 않도록 TTL을 둠
     * - force=true면 TTL/중복 무시하고 무조건 서버 호출
     */
    suspend fun registerDeviceTokenIfNeeded(
        fcmToken: String,
        force: Boolean = false,
        ttlHours: Int = 24
    ): RegisterOutcome {
        if (fcmToken.isBlank()) {
            android.util.Log.w("FCM_REG", "skip: blank token")
            return RegisterOutcome.SkippedBlank
        }

        val now = System.currentTimeMillis()
        val lastInMem = lastRegistered
        val lastInPrefs = prefs.getString(KEY_REGISTERED_TOKEN, null)
        val lastAt = prefs.getLong(KEY_REGISTERED_AT, 0L)
        val expired = (now - lastAt) > ttlHours * 60L * 60L * 1000L

        // 이미 등록 + TTL 유효 + force 아님 → 스킵
        if (!force && !expired && (fcmToken == lastInMem || fcmToken == lastInPrefs)) {
            android.util.Log.d("FCM_REG",
                "skip: already registered (mem=${lastInMem != null}, prefs=${lastInPrefs != null}), expired=$expired"
            )
            return RegisterOutcome.SkippedAlready
        }

        android.util.Log.d("FCM_REG", "try register (force=$force, expired=$expired, ${fcmToken.take(12)}...)")
        return runCatching {
            val res = api.registerDeviceToken(RegisterDeviceTokenRequest(fcmToken))
            if (res.isSuccessful) {
                lastRegistered = fcmToken
                prefs.edit {
                    putString(KEY_REGISTERED_TOKEN, fcmToken)
                    putLong(KEY_REGISTERED_AT, now)
                }
                android.util.Log.d("FCM_REG", "success 200 (${fcmToken.take(12)}...)")
                RegisterOutcome.Success
            } else {
                android.util.Log.w("FCM_REG", "fail HTTP ${res.code()}")
                RegisterOutcome.Failure(IllegalStateException("HTTP ${res.code()}"))
            }
        }.getOrElse { e ->
            android.util.Log.w("FCM_REG","fail: ${e.message}")
            RegisterOutcome.Failure(e)
        }
    }

    /** 특정 토큰을 서버에서 제거(로그아웃/계정전환) + 로컬 상태 초기화 */
    suspend fun deleteDeviceToken(fcmToken: String) {
        runCatching {
            api.deleteDeviceToken(DeleteDeviceTokenRequest(deviceToken = fcmToken))
            prefs.edit() {
                remove(KEY_REGISTERED_TOKEN)
                remove(KEY_REGISTERED_AT)
            }
            lastRegistered = null
        }.onFailure { e ->
            android.util.Log.e("FCM","deleteDeviceToken failed", e)
        }
    }

    /** 저장된 토큰이 있으면 서버에 삭제 요청하고 로컬 플래그 초기화 */
    suspend fun deleteRegisteredTokenIfAny() {
        val reg = prefs.getString(KEY_REGISTERED_TOKEN, null)
        val cachedLatest = prefs.getString("latest_fcm_token", null)

        if (!reg.isNullOrBlank()) {
            runCatching { api.deleteDeviceToken(DeleteDeviceTokenRequest(reg)) }
                .onSuccess {
                    prefs.edit { remove(KEY_REGISTERED_TOKEN); remove(KEY_REGISTERED_AT) }
                    lastRegistered = null
                    android.util.Log.d("FCM_REG", "deleted registered token (${reg.take(12)}...)")
                }
                .onFailure { android.util.Log.w("FCM_REG", "delete registered failed: ${it.message}") }
        }

        if (!cachedLatest.isNullOrBlank() && cachedLatest != reg) {
            runCatching { api.deleteDeviceToken(DeleteDeviceTokenRequest(cachedLatest)) }
                .onSuccess { android.util.Log.d("FCM_REG", "deleted cached latest (${cachedLatest.take(12)}...)") }
                .onFailure { android.util.Log.w("FCM_REG", "delete cached latest failed: ${it.message}") }
        }
    }

    suspend fun acceptInvitation(token: String, invitationId: Int) {
        val res = api.acceptInvitation("Bearer $token", invitationId)
        if (!res.isSuccessful) throw retrofit2.HttpException(res)
    }

    suspend fun rejectInvitation(token: String,invitationId: Int) {
        val res = api.rejectInvitation("Bearer $token", invitationId)
        if (!res.isSuccessful) throw retrofit2.HttpException(res)
    }


    // ------- 알림 목록/읽음 처리 -------


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
