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


private const val PREFS_NAME = "notification_prefs"
private const val KEY_PUSH = "push_enabled"
private const val KEY_REMIND = "remind_enabled"

class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileService: ProfileService
) {
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
}
