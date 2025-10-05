package com.example.nubo.data.network

import com.example.nubo.data.model.NicknameUpdateRequest
import com.example.nubo.data.model.NotificationSetRequest
import com.example.nubo.data.model.ProfileResponse
import com.google.android.gms.common.api.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface ProfileService {
    //프로필 확인
    @GET("/api/user/mypage")
    suspend fun getProfile(): ProfileResponse

    //닉네임 변경
    @PATCH("/api/user/me/nickname")
    suspend fun updateNickname(@Body body: NicknameUpdateRequest):retrofit2.Response<Unit>

    //알림 설정 페이지 (204 No Content)
    // - 요청 바디에서 pushEnabled / remindEnabled 를 선택적으로 보냄
    // - 서버 규칙: pushEnabled=false 이면 remindEnabled 도 자동 false 처리
    @PATCH("/api/user/me/notification")
    suspend fun updateNotification(
        @Body body: NotificationSetRequest
    ):retrofit2.Response<Unit>
}
