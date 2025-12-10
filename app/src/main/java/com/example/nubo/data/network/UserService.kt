package com.example.nubo.data.network

import com.example.nubo.data.dto.UserSearchDto
import com.example.nubo.data.model.InterestSubmitRequest
import com.example.nubo.data.model.InterestSubmitResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface UserService {
    // GET /api/user/search?email=keyword
    @GET("/api/user/search")
    suspend fun searchUsers(@Query("email") emailKeyword: String): List<UserSearchDto>

    // 관심사 설정: POST /api/interests
    @POST("/api/interest")
    suspend fun submitInterests(
        @Body body: InterestSubmitRequest
    ): InterestSubmitResponse

    // 로그인한 내 계정의 튜토리얼 시청 여부를 true 로 변경
    @PATCH("/api/user/me/tutorial-completed")
    suspend fun markTutorialCompleted(): Response<Unit>
}

