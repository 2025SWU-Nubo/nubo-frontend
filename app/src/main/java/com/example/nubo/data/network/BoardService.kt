package com.example.nubo.data.network

import com.example.nubo.data.model.BoardItemResponse
import com.example.nubo.data.model.BoardListItemResponse
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.FavoriteRequest
import com.example.nubo.data.model.FavoriteResponse
import com.example.nubo.data.model.PagedResponse
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.model.UpsertBoardRequest
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BoardService {

    // 나의 보드 조회
    @GET("/api/board")
    suspend fun getMyBoards(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("sort") sort: String? = null,     // LATEST | OLDEST | ALPHABET
        @Query("filter") filter: String? = null, // ALL | FAVORITE | SHARED
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<BoardListItemResponse>

    // 보드 디테일 화면 조회
    // 보드 상세: 최신순만, 필터는 ALL/FAVORITE만 사용
    @GET("/api/board/{id}")
    suspend fun getBoardDetail(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Path("id") boardId: Int,
        @Query("sort") sort: String = "LATEST",
        @Query("filter") filter: String = "ALL",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
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

    // 보드 즐겨찾기 변경 (BoardItemResponse)
    @PATCH("/api/board/{boardId}/favorite")
    suspend fun setFavorite(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Path("boardId") boardId: Long,
        @Body body: FavoriteRequest // 요청 바디
    ): FavoriteResponse // 응답 모델

    //홈_최근 본 보드 조회
    @GET("/api/home/boards/recent")
    suspend fun getRecentBoard(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json"
    ):List<RecentBoardResponse>
}
