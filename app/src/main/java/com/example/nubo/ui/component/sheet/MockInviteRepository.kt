package com.example.nubo.ui.component.sheet

import kotlinx.coroutines.delay

class MockInviteRepository : InviteRepository {
    private val seed = listOf(
        InviteUserItem("1", "NUBO", "nubo@gmail.com"),
        InviteUserItem("2", "NUBO", "nubo1@gmail.com"),
        InviteUserItem("3", "NUBO", "nubo12@gmail.com")
    )

    override suspend fun searchByEmail(keyword: String): List<InviteUserItem> {
        delay(250) // fake network delay
        return seed.filter { it.email.contains(keyword, ignoreCase = true) }
    }
}
