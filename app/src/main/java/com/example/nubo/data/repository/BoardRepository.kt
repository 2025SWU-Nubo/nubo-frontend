package com.example.nubo.data.repository

import com.example.nubo.data.mapper.toDomain
import com.example.nubo.data.model.BoardItemResponse
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.PageState
import com.example.nubo.data.model.PagedResult
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.model.UpsertBoardRequest
import com.example.nubo.data.network.BoardService
import com.example.nubo.domain.model.BoardCardFilter
import com.example.nubo.domain.model.BoardCardSort
import com.example.nubo.domain.model.BoardSummary
import com.google.gson.JsonObject
import retrofit2.HttpException
import javax.inject.Inject

class BoardRepository @Inject constructor(
    private val boardService: BoardService
) {
    // 최근 본 보드 조회
    suspend fun getRecentBoards(token: String): Result<List<RecentBoardResponse>> = runCatching {
        boardService.getRecentBoard(
            authHeader = "Bearer $token",
            acceptHeader = "application/json"
        )
    }

    // 보드 목록 조회 신포맷
    suspend fun getMyBoards(
        token: String,
        sort: BoardCardSort? = BoardCardSort.LATEST,
        filter: BoardCardFilter? = BoardCardFilter.ALL,
        page: Int = 0,
        size: Int = 20
    ): Result<PagedResult<BoardSummary>> = runCatching {
        val res = boardService.getMyBoards(
            authHeader = "Bearer $token",
            acceptHeader = "application/json",
            sort = sort?.name,        // 대문자 그대로 전달
            filter = filter?.name,
            page = page,
            size = size
        )
        PagedResult(
            items = res.content.map { it.toDomain() },
            pageState = PageState(
                page = res.number,
                size = res.size,
                totalPages = res.totalPages,
                totalElements = res.totalElements,
                isFirst = res.first,
                isLast = res.last,
                numberOfElements = res.numberOfElements,
                sorted = res.sort?.sorted == true
            )
        )
    }

    // 보드 생성 전 보드 이름 중복 여부 확인
    suspend fun isBoardNameAvailable(
        token: String,
        name: String
    ): Result<Boolean> = runCatching {
        // Call API and parse {"available": true/false}
        val json: JsonObject = boardService.getBoardNameAvailable(
            name = name,
            authHeader = token
        )
        json.get("available")?.asBoolean == true
    }.recoverCatching { e ->
        if (e is HttpException && e.code() == 409) false else throw e
    }

    // 보드 생성
    suspend fun createBoard(
        token: String,
        name: String,
        shared: Boolean,
        favorite: Boolean = false,
        source: String= "USER",
        memberEmails: List<String>? = null
    ): Result<BoardItemResponse> = runCatching{
        val body = UpsertBoardRequest(
            name = name,
            boardType = "BOARD",
            source = source,
            shared = shared,
            favorite = favorite,
            memberEmails = memberEmails?.takeIf { shared && it.isNotEmpty() }
        )
        boardService.upsertBoard(
            body = body,
            authHeader = token
        )
    }

    // 보드 상세 화면 조회
    suspend fun getBoardDetail(
        token: String,
        boardId: Int,
        favoriteOnly: Boolean,
        page: Int,
        size: Int,
        sort : String
    ): Result<BoardResponse> = runCatching {
        boardService.getBoardDetail(
            authHeader = "Bearer $token",
            acceptHeader = "application/json",
            boardId = boardId,
            sort = sort,                           // 정렬은 최신순 고정
            filter = if (favoriteOnly) "FAVORITE" else "ALL",
            page = page,
            size = size
        )
    }
}
