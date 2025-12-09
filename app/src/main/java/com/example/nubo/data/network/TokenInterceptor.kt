package com.example.nubo.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val access = prefs.getString("access_token", null)

        // 1) 액세스토큰 붙여서 요청
        var request = chain.request().newBuilder()
        if (access != null) {
            request.header("Authorization", "Bearer $access")
        }
        var response = chain.proceed(request.build())

        // 2) 403 Forbidden → 토큰 갱신 로직 실행
        if (response.code == 403) {
            response.close() // 반드시 닫기

            val newAccess = tryRefreshToken(prefs)

            if (newAccess != null) {
                // 리프레시 성공 → 새 토큰으로 다시 호출
                val newReq = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()

                return chain.proceed(newReq)
            } else {
                // 리프레시 실패 → 토큰 삭제하고 로그아웃 플로우로 유도
                prefs.edit().clear().apply()
            }
        }

        return response
    }

    private fun tryRefreshToken(prefs: android.content.SharedPreferences): String? {
        val refresh = prefs.getString("refresh_token", null) ?: return null

        val body = """{"refreshToken":"$refresh"}"""
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = okhttp3.Request.Builder()
            .url("https://janae-nontenantable-endosmotically.ngrok-free.dev/api/auth/refresh")
            .post(body)
            .build()

        val client = OkHttpClient.Builder().build()
        val res = client.newCall(refreshRequest).execute()

        if (!res.isSuccessful) return null

        val json = JSONObject(res.body?.string() ?: return null)
        val newAccess = json.getString("accessToken")
        val newRefresh = json.getString("refreshToken")

        prefs.edit().apply {
            putString("access_token", newAccess)
            putString("refresh_token", newRefresh)
            apply()
        }

        return newAccess
    }
}
