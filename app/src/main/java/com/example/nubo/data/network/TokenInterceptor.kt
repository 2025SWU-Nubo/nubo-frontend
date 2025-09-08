package com.example.nubo.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모든 요청에 Authorization/우회 헤더를 자동으로 추가하는 인터셉터
 * - AuthRepository가 저장한 SharedPreferences에서 토큰을 읽음
 * - DI 순환을 피하기 위해 Repository를 참조하지 않고 Context로 직접 접근
 */
@Singleton
class TokenInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
): Interceptor{
    override fun intercept(chain: Interceptor.Chain): Response {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        val newReq = chain.request().newBuilder()
            .addHeader("ngrok-skip-browser-warning", "true") // ngrok 경고 우회
            .apply {
                if (!token.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        return chain.proceed(newReq)
    }
}
