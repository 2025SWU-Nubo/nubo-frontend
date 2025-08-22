package com.example.nubo.ui.component.sheet

interface InviteRepository {
    suspend fun searchByEmail(keyword: String): List<InviteUserItem>
}
