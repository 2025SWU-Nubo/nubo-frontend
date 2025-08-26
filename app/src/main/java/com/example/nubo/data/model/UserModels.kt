package com.example.nubo.data.model

data class UserItem(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null
)

// Repository contract
interface InviteRepository {
    // Search by email keyword (e.g., prefix match)
    suspend fun searchByEmail(keyword: String): List<UserItem>
}

// Mock repository for demo/testing
class MockInviteRepository : InviteRepository {
    private val seed = listOf(
        UserItem("1", "NUBO", "nubo@gmail.com"),
        UserItem("2", "NUBO", "nubo1@gmail.com"),
        UserItem("3", "NUBO", "nubo12@gmail.com"),
        UserItem("4", "Alex", "alex@example.com"),
        UserItem("5", "Jin", "jin@mail.com"),
    )
    override suspend fun searchByEmail(keyword: String): List<UserItem> {
        // Simulate network delay
        kotlinx.coroutines.delay(250)
        val key = keyword.trim().lowercase()
        if (key.isBlank()) return emptyList()
        return seed.filter { it.email.lowercase().contains(key) }
    }
}
