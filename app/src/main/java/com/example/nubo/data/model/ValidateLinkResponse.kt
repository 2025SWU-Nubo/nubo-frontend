package com.example.nubo.data.model

data class ValidateLinkResponse(
    val valid: Boolean,         // 유효성 여부
    val platform: String?       // "YOUTUBE" 등 (실패 시 null)
)
