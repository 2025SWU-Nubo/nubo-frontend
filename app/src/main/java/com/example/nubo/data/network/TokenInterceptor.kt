package com.example.nubo.data.network

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    companion object {
        // Only one thread can refresh at a time
        @Volatile
        private var isRefreshing = false
        private val refreshLock = Object()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val access = prefs.getString("access_token", null)

        // 1) 원본 요청 로그
        Log.d("TOKEN_INTERCEPTOR", "➡️ API CALL: ${chain.request().url}")

        // 2) 액세스 토큰 붙이기
        val requestBuilder = chain.request().newBuilder()
        if (!access.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $access")
        }

        var response = chain.proceed(requestBuilder.build())

        // 3) 응답 로그
        Log.d(
            "TOKEN_INTERCEPTOR",
            "⬅️ RESPONSE ${response.code} for ${chain.request().url}"
        )

        // 4) 401/403 아니면 그냥 반환
        if (response.code != 401 && response.code != 403) {
            return response
        }

        Log.d("TOKEN_INTERCEPTOR", "🔄 Refresh needed (code=${response.code})")
        response.close() // 메모리 누수 방지

        val newAccess: String?

        synchronized(refreshLock) {
            if (isRefreshing) {
                // 이미 다른 쓰레드가 리프레시 중이면 기다렸다가 저장된 새 토큰으로 재시도
                Log.d("TOKEN_INTERCEPTOR", "⏳ Already refreshing → waiting briefly...")
                // 아주 짧게 기다렸다가(폴링 느낌) 새 토큰이 생겼는지 확인
                Thread.sleep(300)

                val tokenAfterWait = prefs.getString("access_token", null)

                return if (!tokenAfterWait.isNullOrBlank()) {
                    Log.d("TOKEN_INTERCEPTOR", "🔁 Using refreshed access token after wait")
                    retryRequest(chain, tokenAfterWait)
                } else {
                    Log.e(
                        "TOKEN_INTERCEPTOR",
                        "❌ No token after waiting for refresh → returning original 401/403"
                    )
                    // 여기서는 더 할 수 있는 게 없으니 실패 응답 그대로 반환
                    // (ViewModel 쪽에서 handleTokenExpired() 같은 걸로 처리)
                    chain.proceed(chain.request())
                }
            }

            // 내가 리프레시 담당자
            isRefreshing = true
        }

        // 5) 실제 리프레시 호출
        newAccess = tryRefreshToken(prefs, chain.request())

        synchronized(refreshLock) {
            isRefreshing = false
        }

        return if (newAccess != null) {
            Log.d("TOKEN_INTERCEPTOR", "✅ Refresh success → retrying original request")
            retryRequest(chain, newAccess)
        } else {
            // refresh 실패는 2가지가 있음
            // 1) refreshToken 자체가 만료/무효 → 토큰 clear 해야 함
            // 2) 네트워크/서버 장애(ngrok 오프라인 같은) → 토큰 clear 하면 안 됨
            Log.e("TOKEN_INTERCEPTOR", "❌ Refresh failed")

            // tryRefreshToken 안에서 "무효 refresh"일 때만 clearAuthDataNeeded=true 로 처리
            val shouldClear = prefs.getBoolean("refresh_clear_needed", false)
            prefs.edit().remove("refresh_clear_needed").apply()

            if (shouldClear) {
                Log.e("TOKEN_INTERCEPTOR", "❌ Refresh invalid → clearing tokens")
                prefs.edit().clear().apply()
            } else {
                Log.e("TOKEN_INTERCEPTOR", "⚠️ Refresh failed but not clearing tokens (network/server issue)")
            }

            // 실패했으니 원래 요청을 또 보내봤자 의미가 없음 → 그냥 401/403 상황으로 돌려보냄
            chain.proceed(chain.request())
        }
    }

    /** 새 accessToken으로 원래 요청 재시도 */
    private fun retryRequest(chain: Interceptor.Chain, newToken: String): Response {
        val newReq = chain.request().newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header("X-REFRESH-RETRY", "true") // 디버깅용
            .build()

        return chain.proceed(newReq)
    }

    /** Refresh API 실제 호출 */
    private fun tryRefreshToken(
        prefs: android.content.SharedPreferences,
        originalRequest: Request
    ): String? {
        val refresh = prefs.getString("refresh_token", null) ?: return null

        Log.d("TOKEN_INTERCEPTOR", "👉 Calling /auth/refresh with refreshToken")

        val body = """{"refreshToken":"$refresh"}"""
            .toRequestBody("application/json".toMediaType())

        // refresh 도 원본 요청의 host/scheme 를 그대로 사용
        val refreshUrl = originalRequest.url.newBuilder()
            .encodedPath("/api/auth/refresh")
            .query(null)
            .fragment(null)
            .build()

        val request = Request.Builder()
            .url(refreshUrl)
            .post(body)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        return try {
            val response = client.newCall(request).execute()
            Log.d("TOKEN_INTERCEPTOR", "refresh response code=${response.code}")

            if (!response.isSuccessful) {
                // 401/403 은 refreshToken 무효 가능성이 높으니 토큰 clear 필요 플래그
                if (response.code == 401 || response.code == 403) {
                    prefs.edit().putBoolean("refresh_clear_needed", true).apply()
                }

                Log.e("TOKEN_INTERCEPTOR", "refresh not successful, body=${response.body?.string()}")
                return null
            }

            val jsonString = response.body?.string() ?: return null
            val json = JSONObject(jsonString)

            val newAccess = json.getString("accessToken")
            val newRefresh = json.getString("refreshToken")

            Log.d(
                "TOKEN_INTERCEPTOR",
                "refresh success, newAccess=${newAccess.take(15)}..., newRefresh=${newRefresh.take(15)}..."
            )

            prefs.edit().apply {
                putString("access_token", newAccess)
                putString("refresh_token", newRefresh)
                apply()
            }

            newAccess
        } catch (e: IOException) {
            // 네트워크 장애는 clear 하면 안 됨
            Log.e("TOKEN_INTERCEPTOR", "refresh network error", e)
            null
        } catch (e: Exception) {
            Log.e("TOKEN_INTERCEPTOR", "refresh error", e)
            null
        }
    }
}
