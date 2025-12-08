package com.example.nubo.data.repository

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.nubo.data.model.LoginRequest
import com.example.nubo.data.model.LoginResponse
import com.example.nubo.data.model.RefreshTokenRequest
import com.example.nubo.data.model.RefreshTokenResponse
import com.example.nubo.data.model.TokenCheckRequest
import com.example.nubo.data.model.TokenValidationResponse
import com.example.nubo.data.model.UserInfo
import com.example.nubo.data.network.AuthService
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AuthRepository"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_INFO = "user_info"

        private const val KEY_INTEREST_COMPLETED = "interest_completed"
        private const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
    }

    private val _accessTokenFlow = MutableStateFlow<String?>(null)
    val accessTokenFlow: StateFlow<String?> = _accessTokenFlow

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == KEY_ACCESS_TOKEN) {
            _accessTokenFlow.value = prefs.getString(KEY_ACCESS_TOKEN, null)
        }
    }


    init {
        // set initial value & register listener
        _accessTokenFlow.value = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
    }


    // --- API calls ---
    fun loginWithGoogle(request: LoginRequest): Call<LoginResponse> {
        return authService.loginWithGoogle(request)
    }

    //서버에 토큰 유효성 검증 요청
    fun checkToken(): Call<TokenValidationResponse>? {
        val token = getAccessToken()
        return if (token != null) {
            val request = TokenCheckRequest(accessToken = token)
            authService.checkToken(request)
        } else {
            null
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): RefreshTokenResponse {
        Log.d(TAG, "refreshAccessToken() call start")

        val request = RefreshTokenRequest(refreshToken)
        val response = authService.refreshToken(request).execute()

        if (!response.isSuccessful) {
            Log.e(TAG, "refresh failed: code=${response.code()}")
            throw Exception("Refresh failed")
        }

        val body = response.body() ?: throw Exception("Empty refresh body")

        // 서버가 새 토큰 내려줌 → 저장
        saveAccessToken(body.accessToken)
        saveRefreshToken(body.refreshToken)
        saveUserInfo(body.user)

        saveOnboardingFlags(
            interestCompleted = body.interestSetupCompleted,
            tutorialCompleted = body.tutorialCompleted
        )

        Log.d(TAG, "refresh success → new access token saved")
        return body
    }

    // --- Token & user info persistence ---
    fun saveAccessToken(token: String) {
        Log.d(TAG, "saveAccessToken() length=${token.length} preview=${token.take(10)}...")
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
            apply()
        }
    }

    fun saveUserId(userId: Int) {
        Log.d(TAG, "saveUserId() id=$userId")
        sharedPreferences.edit().apply {
            putInt(KEY_USER_ID, userId)
            apply()
        }
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? =
        sharedPreferences.getString(KEY_REFRESH_TOKEN, null)


    fun getUserId(): Int? {
        val v = sharedPreferences.getInt(KEY_USER_ID, Int.MIN_VALUE)
        return if (v == Int.MIN_VALUE) null else v
    }

    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    fun logout() {
        sharedPreferences.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            apply()
        }
    }

    // 모든 인증 관련 데이터 삭제
    fun clearAuthData() {
        Log.d(TAG, "clearAuthData() remove all auth related prefs")
        sharedPreferences.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_INFO)
            remove(KEY_INTEREST_COMPLETED)
            remove(KEY_TUTORIAL_COMPLETED)
            apply()
        }
    }

    // UserInfo 저장
    fun saveUserInfo(userInfo: UserInfo) {
        val json = Gson().toJson(userInfo)
        Log.d(TAG, "saveUserInfo() userId=${userInfo.id}")
        sharedPreferences.edit().apply {
            putString(KEY_USER_INFO, json)
            apply()
        }
    }

    // UserInfo 불러오기
    fun getUserInfo(): UserInfo? {
        val json = sharedPreferences.getString(KEY_USER_INFO, null)
        return json?.let { Gson().fromJson(it, UserInfo::class.java) }
    }

    fun saveOnboardingFlags(
        interestCompleted: Boolean,
        tutorialCompleted: Boolean
    ) {
        Log.d(
            TAG,
            "saveOnboardingFlags() interest=$interestCompleted tutorial=$tutorialCompleted"
        )
        sharedPreferences.edit().apply {
            putBoolean(KEY_INTEREST_COMPLETED, interestCompleted)
            putBoolean(KEY_TUTORIAL_COMPLETED, tutorialCompleted)
            apply()
        }

        // Log stored values right after save
        Log.d(
            TAG,
            "saved flags now interest=${getInterestCompleted()} tutorial=${getTutorialCompleted()}"
        )
    }

    // Update only interest flag (after interest onboarding finished)
    fun setInterestCompleted(completed: Boolean) {
        Log.d(TAG, "setInterestCompleted() completed=$completed")
        sharedPreferences.edit().apply {
            putBoolean(KEY_INTEREST_COMPLETED, completed)
            apply()
        }
        Log.d(TAG, "stored interestCompleted=${getInterestCompleted()}")
    }

    // Update only tutorial flag (after tutorial finished)
    fun setTutorialCompleted(completed: Boolean) {
        Log.d(TAG, "setTutorialCompleted() completed=$completed")
        sharedPreferences.edit().apply {
            putBoolean(KEY_TUTORIAL_COMPLETED, completed)
            apply()
        }
        Log.d(TAG, "stored tutorialCompleted=${getTutorialCompleted()}")
    }

    // Nullable: null means "unknown (never set yet)"
    fun getInterestCompleted(): Boolean? {
        return if (sharedPreferences.contains(KEY_INTEREST_COMPLETED)) {
            val v = sharedPreferences.getBoolean(KEY_INTEREST_COMPLETED, false)
            Log.d(TAG, "getInterestCompleted() exists=true value=$v")
            v
        } else {
            Log.d(TAG, "getInterestCompleted() exists=false value=null")
            null
        }
    }

    fun getTutorialCompleted(): Boolean? {
        return if (sharedPreferences.contains(KEY_TUTORIAL_COMPLETED)) {
            val v = sharedPreferences.getBoolean(KEY_TUTORIAL_COMPLETED, false)
            Log.d(TAG, "getTutorialCompleted() exists=true value=$v")
            v
        } else {
            Log.d(TAG, "getTutorialCompleted() exists=false value=null")
            null
        }
    }



}

