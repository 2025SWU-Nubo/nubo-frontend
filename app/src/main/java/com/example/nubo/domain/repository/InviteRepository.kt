package com.example.nubo.domain.repository

import com.example.nubo.domain.model.InviteUser

interface InviteRepository {
    suspend fun searchByEmail(keyword: String): List<InviteUser>
}
