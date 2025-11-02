package com.example.nubo.data.network

import com.example.nubo.data.model.BoardDeleteRequest
import com.example.nubo.data.model.BoardDeleteResponse
import com.example.nubo.data.model.BoardItemResponse
import com.example.nubo.data.model.BoardListItemResponse
import com.example.nubo.data.model.BoardRenameRequest
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.BoardRestoreRequest
import com.example.nubo.data.model.BoardRestoreResponse
import com.example.nubo.data.model.BoardSearchItemResponse
import com.example.nubo.data.model.BoardWithSectionsResponse
import com.example.nubo.data.model.BulkCopyRequest
import com.example.nubo.data.model.BulkCopyResponse
import com.example.nubo.data.model.BulkMoveRequest
import com.example.nubo.data.model.BulkMoveResponse
import com.example.nubo.data.model.CardSearchItemResponse
import com.example.nubo.data.model.FavoriteRequest
import com.example.nubo.data.model.FavoriteResponse
import com.example.nubo.data.model.HomeBoardResponse
import com.example.nubo.data.model.PagedResponse
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.model.UpsertBoardRequest
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BoardService {

    // 홈_미시청 보드 이름 조회
    @GET("/api/home/boards")
    suspend fun getHomeBoards(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("sort") sort: String? = null // LATEST | OLDEST | ALPHABET
    ): List<HomeBoardResponse>

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

    // 섹션/보드 공통 이름 변경 API
    @PATCH("api/board/{boardId}/name")
    suspend fun renameBoardOrSection(
        @Header("Authorization") authHeader: String,
        @Header("Accept") accept: String = "application/json",
        @Path("boardId") boardId: Long,
        @Body body: BoardRenameRequest
    )

    // 보드 검색
    @GET("/api/board/search")
    suspend fun searchBoards(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("keyword") keyword: String,
        @Query("sort") sort: String = "LATEST" // API 명세에 따라 LATEST, OLDEST, ALPHABET 사용 가능
    ): List<BoardSearchItemResponse> // API 명세에 페이징 정보가 없으므로 List 형태로 받음

    // 카드 검색
    @GET("/api/card/search")
    suspend fun searchCards(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("keyword") keyword: String,
        @Query("sort") sort: String = "LATEST"
    ): List<CardSearchItemResponse>

    //홈_최근 본 보드 조회
    @GET("/api/home/boards/recent")
    suspend fun getRecentBoard(
        @Header("Authorization") authHeader: String,
        @Header("Accept") acceptHeader: String = "application/json"
    ):List<RecentBoardResponse>

    // 보드+섹션 트리 조회
    @GET("api/board/with-sections")
    suspend fun getBoardsWithSections(
        @Header("Authorization") authorization: String,           // "Bearer {token}"
        @Header("Accept") accept: String = "application/json"
    ): Response<List<BoardWithSectionsResponse>>

    // 섹션 및 카드 일괄 복제 API
    @POST("/api/board/{sourceBoardId}/bulk-copy")
    suspend fun bulkCopy(
        @Header("Authorization") authHeader: String,
        @Path("sourceBoardId") sourceBoardId: Long,
        @Body body: BulkCopyRequest
    ): Response<BulkCopyResponse> // Response는 Unit 또는 실제 응답 클래스로 변경 가능

    // '나의 카드' 탭(루트)에서 호출하는 API
    @POST("api/board/bulk-copy")
    suspend fun bulkCopyFromRoot(
        @Header("Authorization") authHeader: String,
        @Body body: BulkCopyRequest
    ): Response<Void>
    @POST("api/board/bulk-move")
    suspend fun bulkMoveFromRoot(
        @Header("Authorization") authHeader: String,
        @Body body: BulkMoveRequest
    ): Response<Void>

    // 섹션 및 카드 일괄 이동 API
    @POST("/api/board/{sourceBoardId}/bulk-move")
    suspend fun bulkMove(
        @Header("Authorization") authHeader: String,
        @Path("sourceBoardId") sourceBoardId: Long,
        @Body body: BulkMoveRequest
    ): Response<BulkMoveResponse>

    // 보드(섹션 삭제)
    @HTTP(method = "DELETE", path = "/api/board", hasBody = true)
    suspend fun deleteBoards(
        @Header("Authorization") authHeader: String,
        @Body body: BoardDeleteRequest
    ): Response<List<BoardDeleteResponse>> // 응답 형식이 List라고 가정

    // 새로운 보드 복구 API 엔드포인트
    @PATCH("/api/board/restore")
    suspend fun restoreBoards(
        @Header("Authorization") authHeader: String,
        @Body body: BoardRestoreRequest
    ): Response<BoardRestoreResponse>
}
