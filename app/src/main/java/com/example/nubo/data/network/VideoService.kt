package com.example.nubo.data.network

import com.example.nubo.data.model.BoardWithSectionsResponse
import com.example.nubo.data.model.ValidateLinkResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface VideoService {

    // 영상 링크 유효성 검사 api
    @GET("/api/videos/validate-link")
    suspend fun validateLink(
        @Query("url") url: String
    ): Response<ValidateLinkResponse>

    // 보드+섹션 트리 조회
    @GET("api/board/with-sections")
    suspend fun getBoardsWithSections(
        @Header("Authorization") authorization: String,           // "Bearer {token}"
        @Header("Accept") accept: String = "application/json"
    ): Response<List<BoardWithSectionsResponse>>
}
