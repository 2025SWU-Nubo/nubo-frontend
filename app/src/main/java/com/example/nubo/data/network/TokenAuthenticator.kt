package com.example.nubo.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton
import com.example.nubo.data.model.RefreshTokenRequest
import com.example.nubo.data.network.AuthService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit

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

        val refreshRequest = Request.Builder()
            .url("https://janae-nontenantable-endosmotically.ngrok-free.dev/api/auth/refresh")
            .post(
                """{"refreshToken":"$refreshToken"}"""
                    .toRequestBody("application/json".toMediaType())
            )
            .build()

        val client = OkHttpClient.Builder()
            .build()

        val refreshResponse = client.newCall(refreshRequest).execute()

        if (!refreshResponse.isSuccessful) {
            clearTokens()
            return null
        }

        val bodyString = refreshResponse.body?.string() ?: return null
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

    private fun clearTokens() {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
