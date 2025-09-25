package com.example.nubo.data.network

import com.example.nubo.data.model.NicknameUpdateRequest
import com.example.nubo.data.model.ProfileResponse
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
}
