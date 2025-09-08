package com.example.nubo.domain.model

data class InviteUser(
    val id: Long,
    val nickname: String,
    val email: String,
    val profileImageUrl: String? = null
)
