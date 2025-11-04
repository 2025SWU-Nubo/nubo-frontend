package com.example.nubo.data.network

import com.example.nubo.data.dto.UserSearchDto
import com.example.nubo.data.model.InterestSubmitRequest
import com.example.nubo.data.model.InterestSubmitResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface UserService {
    // GET /api/user/search?email=keyword
    @GET("/api/user/search")
    suspend fun searchUsers(@Query("email") emailKeyword: String): List<UserSearchDto>

    // 관심사 설정: POST /api/interests
    @POST("/api/interests")
    suspend fun submitInterests(
        @Header("Authorization") auth: String,
        @Header("Accept") accept: String = "application/json",
        @Body body: InterestSubmitRequest
    ): InterestSubmitResponse
}

