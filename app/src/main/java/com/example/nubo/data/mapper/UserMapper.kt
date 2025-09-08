package com.example.nubo.data.mapper

import com.example.nubo.data.dto.UserSearchDto
import com.example.nubo.domain.model.InviteUser

// Map network DTO to domain model
fun UserSearchDto.toDomain(): InviteUser = InviteUser(
    id = id,
    nickname = nickname,
    email = email,
    profileImageUrl = profileImageUrl
)
