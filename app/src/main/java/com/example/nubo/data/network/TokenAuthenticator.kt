package com.example.nubo.data.network

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        // 무한 루프 방지
        if (response.request.header("X-Refresh-Attempt") == "true") {
            clearTokens()
            return null
        }

        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", null) ?: return null

        // refresh 도 원본 요청의 host/scheme 를 그대로 사용
        val refreshUrl = response.request.url.newBuilder()
            .encodedPath("/api/auth/refresh")
            .query(null)
            .fragment(null)
            .build()

        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .post(
                """{"refreshToken":"$refreshToken"}"""
                    .toRequestBody("application/json".toMediaType())
            )
            .header("X-Refresh-Attempt", "true") // refresh 호출 자체도 재진입 방지
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val refreshResponse = try {
            client.newCall(refreshRequest).execute()
        } catch (e: IOException) {
            // 네트워크 문제는 로그아웃 처리하면 안 됨
            Log.e("TOKEN_AUTHENTICATOR", "refresh network error", e)
            return null
        } catch (e: Exception) {
            Log.e("TOKEN_AUTHENTICATOR", "refresh error", e)
            return null
        }

        refreshResponse.use { rr ->
            if (!rr.isSuccessful) {
                // refreshToken 무효 가능성이 큰 케이스만 clear
                if (rr.code == 401 || rr.code == 403) {
                    clearTokens()
                }
                return null
            }

            val bodyString = rr.body?.string() ?: return null
            val json = JSONObject(bodyString)

            val newAccess = json.getString("accessToken")
            val newRefresh = json.getString("refreshToken")

            prefs.edit().apply {
                putString("access_token", newAccess)
                putString("refresh_token", newRefresh)
                apply()
            }

            // 원래 요청 다시 시도
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .header("X-Refresh-Attempt", "true")
                .build()
        }
    }

    private fun clearTokens() {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
