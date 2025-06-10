package com.example.nubo.data.network

import com.example.nubo.data.model.BoardResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface BoardService {

    @GET("/api/board")
    suspend fun getMyBoards(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json"
    ): List<BoardResponse>

    @GET("/api/board/{id}")
    suspend fun getBoardDetail(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Path("id") boardId: String
    ): BoardResponse
}
