package com.example.nubo.data.network

import com.example.nubo.data.model.BoardItemResponse
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.UpsertBoardRequest
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("/api/board/check-name")
    suspend fun getBoardNameAvailable(
        @Query("name") name: String,
        @Header("Authorization") authHeader: String
    ): JsonObject

    @POST("/api/board")
    suspend fun upsertBoard(
        @Body body:UpsertBoardRequest,
        @Header("Authorization")authHeader: String
    ):BoardItemResponse
}
