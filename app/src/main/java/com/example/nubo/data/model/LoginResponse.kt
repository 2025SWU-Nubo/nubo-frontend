package com.example.nubo.data.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserInfo,
    val reactivated: Boolean,
    val newUser: Boolean,
    val interestSetupCompleted: Boolean,
    val tutorialCompleted: Boolean
)
