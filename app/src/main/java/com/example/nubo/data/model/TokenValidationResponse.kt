package com.example.nubo.data.model

data class TokenValidationResponse(
    val valid : Boolean,
    val expired: Boolean,
    val user: UserInfo
)
