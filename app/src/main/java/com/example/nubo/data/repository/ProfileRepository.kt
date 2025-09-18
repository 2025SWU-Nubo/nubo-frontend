package com.example.nubo.data.repository

import com.example.nubo.data.model.NicknameUpdateRequest
import com.example.nubo.data.model.ProfileResponse
import com.example.nubo.data.network.ProfileService
import retrofit2.HttpException
import javax.inject.Inject

class ProfileRepository @Inject constructor(
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
}
