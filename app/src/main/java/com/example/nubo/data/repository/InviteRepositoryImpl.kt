package com.example.nubo.data.repository

import com.example.nubo.data.mapper.toDomain
import com.example.nubo.data.network.UserService
import com.example.nubo.domain.model.InviteUser
import com.example.nubo.domain.repository.InviteRepository
import javax.inject.Inject

class InviteRepositoryImpl @Inject constructor(
    private val userService: UserService
) : InviteRepository {

    override suspend fun searchByEmail(keyword: String): List<InviteUser> {
        if (keyword.isBlank()) return emptyList()
        return userService.searchUsers(keyword).map { it.toDomain() }
    }
}
