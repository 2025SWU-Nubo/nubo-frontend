package com.example.nubo.data.model

data class TokenValidationResponse(
    val valid : Boolean,
    val expired: Boolean,
    val interestSetupCompleted: Boolean,
    val tutorialCompleted: Boolean
)
