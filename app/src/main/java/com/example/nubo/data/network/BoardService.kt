package com.example.nubo.data.network

import com.example.nubo.data.model.*
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface BoardService {

    // 홈_미시청 보드 이름 조회
    @GET("/api/home/boards")
    suspend fun getHomeBoards(
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("sort") sort: String? = null
    ): List<HomeBoardResponse>

    // 나의 보드 조회
    @GET("/api/board")
    suspend fun getMyBoards(
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("sort") sort: String? = null,
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<BoardListItemResponse>

    // 보드 상세 조회
    @GET("/api/board/{id}")
    suspend fun getBoardDetail(
        @Header("Accept") acceptHeader: String = "application/json",
        @Path("id") boardId: Int,
        @Query("sort") sort: String = "LATEST",
        @Query("filter") filter: String = "ALL",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): BoardResponse

    // 보드 이름 중복 체크
    @GET("/api/board/check-name")
    suspend fun getBoardNameAvailable(
        @Query("name") name: String
    ): JsonObject

    // 보드 생성/수정
    @POST("/api/board")
    suspend fun upsertBoard(
        @Body body: UpsertBoardRequest
    ): BoardItemResponse

    // 즐겨찾기 설정
    @PATCH("/api/board/{boardId}/favorite")
    suspend fun setFavorite(
        @Header("Accept") acceptHeader: String = "application/json",
        @Path("boardId") boardId: Long,
        @Body body: FavoriteRequest
    ): FavoriteResponse

    // 보드/섹션 이름 변경
    @PATCH("api/board/{boardId}/name")
    suspend fun renameBoardOrSection(
        @Header("Accept") accept: String = "application/json",
        @Path("boardId") boardId: Long,
        @Body body: BoardRenameRequest
    )

    // 보드 검색
    @GET("/api/board/search")
    suspend fun searchBoards(
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("keyword") keyword: String,
        @Query("sort") sort: String = "LATEST"
    ): List<BoardSearchItemResponse>

    // 카드 검색
    @GET("/api/card/search")
    suspend fun searchCards(
        @Header("Accept") acceptHeader: String = "application/json",
        @Query("keyword") keyword: String,
        @Query("sort") sort: String = "LATEST"
    ): List<CardSearchItemResponse>

    // 최근 본 보드 조회
    @GET("/api/home/boards/recent")
    suspend fun getRecentBoard(
        @Header("Accept") acceptHeader: String = "application/json"
    ): List<RecentBoardResponse>

    // 보드 + 섹션 트리 조회
    @GET("api/board/with-sections")
    suspend fun getBoardsWithSections(
        @Header("Accept") accept: String = "application/json"
    ): Response<List<BoardWithSectionsResponse>>

    // 일괄 복제
    @POST("/api/board/{sourceBoardId}/bulk-copy")
    suspend fun bulkCopy(
        @Path("sourceBoardId") sourceBoardId: Long,
        @Body body: BulkCopyRequest
    ): Response<BulkCopyResponse>

    @POST("api/board/bulk-copy")
    suspend fun bulkCopyFromRoot(
        @Body body: BulkCopyRequest
    ): Response<Void>

    @POST("api/board/bulk-move")
    suspend fun bulkMoveFromRoot(
        @Body body: BulkMoveRequest
    ): Response<Void>

    // 일괄 이동
    @POST("/api/board/{sourceBoardId}/bulk-move")
    suspend fun bulkMove(
        @Path("sourceBoardId") sourceBoardId: Long,
        @Body body: BulkMoveRequest
    ): Response<BulkMoveResponse>

    // 보드(섹션) 삭제
    @HTTP(method = "DELETE", path = "/api/board", hasBody = true)
    suspend fun deleteBoards(
        @Body body: BoardDeleteRequest
    ): Response<List<BoardDeleteResponse>>

    // 보드 복구
    @PATCH("/api/board/restore")
    suspend fun restoreBoards(
        @Body body: BoardRestoreRequest
    ): Response<BoardRestoreResponse>

    // 기본 보드 목록
    @GET("/api/board/defaults")
    suspend fun getDefaultBoards(
        @Header("Accept") accept: String = "application/json"
    ): List<DefaultBoardItemResponse>

    // 보드 멤버 조회
    @GET("api/board/{boardId}/members")
    suspend fun getBoardMembers(
        @Path("boardId") boardId: Long
    ): Response<BoardMembersResponse>

    // 초대 취소
    @DELETE("api/board/{boardId}/invitation/{invitationId}")
    suspend fun cancelInvitation(
        @Path("boardId") boardId: Long,
        @Path("invitationId") invitationId: Long
    ): Response<Unit>

    // 개인 보드 → 공유 보드 전환
    @PATCH("api/board/{boardId}/share")
    suspend fun updateBoardShare(
        @Path("boardId") boardId: Long,
        @Body body: ShareBoardRequest
    ): Response<ShareBoardResponse>

    // 보드 멤버 초대
    @POST("api/board/{boardId}/invitation")
    suspend fun inviteMembers(
        @Path("boardId") boardId: Long,
        @Body body: InviteMembersRequest
    ): Response<List<InvitationDto>>
}
