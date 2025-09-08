package com.example.nubo.data.dto

import com.google.gson.annotations.SerializedName

data class UserSearchDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("email") val email: String,
    @SerializedName("profileImageUrl") val profileImageUrl: String?
)
