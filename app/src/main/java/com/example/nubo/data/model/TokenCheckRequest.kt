package com.example.nubo.data.model

data class TokenCheckRequest(
    val accessToken: String
)

data class  RefreshTokenRequest(
    val refreshToken: String
)
