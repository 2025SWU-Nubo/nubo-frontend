package com.example.nubo.data.model

data class LoginResponse(
    val accessToken: String,
    val user: UserInfo,
    val reactivated: Boolean,
    val isNewUser: Boolean
)
