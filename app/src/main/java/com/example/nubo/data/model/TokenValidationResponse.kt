package com.example.nubo.data.model

data class TokenValidationResponse(
    val valid : Boolean,
    val expired: Boolean,
    val interestSetupCompleted: Boolean,
    val tutorialCompleted: Boolean
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserInfo,
    val reactivated: Boolean,
    val newUser: Boolean,
    val interestSetupCompleted: Boolean,
    val tutorialCompleted: Boolean
)

