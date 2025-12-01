package com.example.nubo.data.repository

import android.content.Context
import com.example.nubo.data.model.NicknameUpdateRequest
import com.example.nubo.data.model.NotificationSetRequest
import com.example.nubo.data.model.ProfileResponse
import com.example.nubo.data.network.ProfileService
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import javax.inject.Inject
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.Response

sealed interface AuthEvent {
    data object LoginRequired : AuthEvent
}

private const val PREFS_NAME = "notification_prefs"
private const val KEY_PUSH = "push_enabled"
private const val KEY_REMIND = "remind_enabled"

class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileService: ProfileService,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
) {
    // ───────── 인증 이벤트 채널(뷰모델이 구독) ─────────
    private val _authEvents = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    // ───────── 내부 헬퍼: 저장된 토큰 → Bearer 문자열 생성 ─────────
    private fun bearerOrNull(): String? =
        authRepository.getAccessToken()?.let { "Bearer $it" }

    // ───────── 내부 헬퍼: Response<T> → Result<T> (+401/403 처리) ─────────
    private suspend fun <T> asResult(resp: Response<T>): Result<T> {
        if (resp.isSuccessful) return Result.success(resp.body() as T)
        if (resp.code() == 401 || resp.code() == 403) {
            // 토큰 없음/만료 → 로그인 필요 이벤트 발행
            _authEvents.emit(AuthEvent.LoginRequired)
            return Result.failure(IllegalStateException("Unauthorized"))
        }
        return Result.failure(HttpException(resp))
    }
    private suspend fun asResultUnit(resp: Response<Unit>): Result<Unit> {
        if (resp.isSuccessful) return Result.success(Unit)
        if (resp.code() == 401 || resp.code() == 403) {
            _authEvents.emit(AuthEvent.LoginRequired)
            return Result.failure(IllegalStateException("Unauthorized"))
        }
        return Result.failure(HttpException(resp))
    }


    // 프로필 확인
    suspend fun fetchProfile(): Result<ProfileResponse> = runCatching {
        profileService.getProfile()
    }
    // 넥네임 변경
    suspend fun updateNickname(nickname: String): Result<Unit> = runCatching {
        val res = profileService.updateNickname(NicknameUpdateRequest(nickname))
        if (!res.isSuccessful) throw HttpException(res)
    }

    // 알림 설정 PATCH (추가)
    suspend fun updateNotification(
        body: NotificationSetRequest
    ): Result<Unit> = runCatching {
        // 한글 주석: 알림 설정 변경 요청 (204 No Content 기대)
        val res = profileService.updateNotification(body)
        if (!res.isSuccessful) throw HttpException(res)
    }

    // 로컬에서 마지막 알림 설정 읽기 (기본값: push=true, remind=false)
    fun readNotificationPrefs(): Pair<Boolean, Boolean> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val push = sp.getBoolean(KEY_PUSH, true)
        val remind = sp.getBoolean(KEY_REMIND, false)
        return push to remind
    }

    // 로컬에 알림 설정 저장 (KTX 확장 사용)
    fun saveNotificationPrefs(push: Boolean, remind: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_PUSH, push)
            putBoolean(KEY_REMIND, remind)
        }
    }

    suspend fun withdraw(): Result<Unit> {
        // 1) 현재 기기 FCM 매핑을 먼저 제거(실패해도 계속)
        runCatching { notificationRepository.deleteRegisteredTokenIfAny() }

        // 2) 토큰이 없으면 로그인 필요 신호
        val bearer = bearerOrNull() ?: run {
            _authEvents.emit(AuthEvent.LoginRequired)
            return Result.failure(IllegalStateException("Missing token"))
        }

        // 2-1) 실제 탈퇴 API 호출 후 결과 변환(401/403 처리 포함)
        val result = asResult(profileService.deleteMe(bearer))

        // 3) 성공이면 로컬 인증 데이터 정리
        if (result.isSuccess) {
            authRepository.clearAuthData()
        }

        return result
    }

    /*// 안 읽은 알림 존재 여부 조회
    suspend fun hasUnreadNotification(): Boolean {
        return runCatching {
            // 1. 액세스 토큰 가져오기
            val token = authRepository.getAccessToken() ?: return false

            // 2. NotificationRepository 가 요구하는 token 그대로 전달
            notificationRepository.hasUnreadNotification(token)
        }.getOrElse {
            false
        }
    }*/
}
