package com.example.nubo.data.network

import com.example.nubo.data.model.BoardItemResponse
import com.example.nubo.data.model.BoardListItemResponse
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.PagedResponse
import com.example.nubo.data.model.RecentBoardResponse
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
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("sort") sort: String? = null,     // LATEST | OLDEST | ALPHABET
        @Query("filter") filter: String? = null, // ALL | FAVORITE | SHARED
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<BoardListItemResponse>

    @GET("/api/board/{id}")
    suspend fun getBoardDetail(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Path("id") boardId: Int
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

    //홈_최근 본 보드 조회
    @GET("/api/home/boards/recent")
    suspend fun getRecentBoard(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json"
    ):List<RecentBoardResponse>
}
