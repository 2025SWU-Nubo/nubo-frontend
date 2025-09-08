package com.example.nubo.data.network

import com.example.nubo.data.dto.UserSearchDto
import retrofit2.http.GET
import retrofit2.http.Query

interface UserService {
    // GET /api/user/search?email=keyword
    @GET("/api/user/search")
    suspend fun searchUsers(@Query("email") emailKeyword: String): List<UserSearchDto>
}

