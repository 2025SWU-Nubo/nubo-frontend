package com.example.nubo.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.nubo.data.model.LoginRequest
import com.example.nubo.data.model.LoginResponse
import com.example.nubo.data.model.UserInfo
import com.example.nubo.data.network.AuthService
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
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
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
    }

    fun loginWithGoogle(request: LoginRequest): Call<LoginResponse> {
        return authService.loginWithGoogle(request)
    }

    fun saveAccessToken(token: String) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
            apply()
        }
    }

    fun saveUserId(userId: Int) {
        sharedPreferences.edit().apply {
            putInt(KEY_USER_ID, userId)
            apply()
        }
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    fun logout() {
        sharedPreferences.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_ID)
            apply()
        }
    }

    // UserInfo 저장
    fun saveUserInfo(userInfo: UserInfo) {
        val json = Gson().toJson(userInfo)
        sharedPreferences.edit().apply {
            putString("user_info", json)
            apply()
        }
    }

    // UserInfo 불러오기
    fun getUserInfo(): UserInfo? {
        val json = sharedPreferences.getString("user_info", null)
        return json?.let { Gson().fromJson(it, UserInfo::class.java) }
    }

}

