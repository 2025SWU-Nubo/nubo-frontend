package com.example.nubo.data.repository

import com.example.nubo.data.mapper.toDomain
import com.example.nubo.data.model.*
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

    // 홈 - 미시청 보드 조회
    suspend fun getHomeBoards(
        sort: String? = "LATEST"
    ): Result<List<HomeBoardResponse>> = runCatching {
        boardService.getHomeBoards(
            acceptHeader = "application/json",
            sort = sort
        )
    }

    // 홈 - 최근 본 보드 조회
    suspend fun getRecentBoards(): Result<List<RecentBoardResponse>> = runCatching {
        boardService.getRecentBoard(
            acceptHeader = "application/json"
        )
    }

    // 보드 목록 조회
    suspend fun getMyBoards(
        sort: BoardCardSort? = BoardCardSort.LATEST,
        filter: BoardCardFilter? = BoardCardFilter.ALL,
        page: Int = 0,
        size: Int = 20
    ): Result<PagedResult<BoardSummary>> = runCatching {

        val res = boardService.getMyBoards(
            acceptHeader = "application/json",
            sort = sort?.name,
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

    // 보드 이름 중복 체크
    suspend fun isBoardNameAvailable(name: String): Result<Boolean> =
        runCatching {
            val json: JsonObject = boardService.getBoardNameAvailable(name = name)
            json.get("available")?.asBoolean == true
        }.recoverCatching { e ->
            if (e is HttpException && e.code() == 409) false else throw e
        }

    // 보드 생성
    suspend fun createBoard(
        name: String,
        shared: Boolean,
        favorite: Boolean = false,
        source: String = "USER",
        memberEmails: List<String>? = null
    ): Result<BoardItemResponse> = runCatching {

        val body = UpsertBoardRequest(
            name = name,
            boardType = "BOARD",
            source = source,
            shared = shared,
            favorite = favorite,
            memberEmails = memberEmails?.takeIf { shared && it.isNotEmpty() }
        )

        boardService.upsertBoard(body)
    }

    // 보드 상세 조회
    suspend fun getBoardDetail(
        boardId: Int,
        favoriteOnly: Boolean,
        page: Int,
        size: Int,
        sort: String
    ): Result<BoardResponse> = runCatching {

        boardService.getBoardDetail(
            acceptHeader = "application/json",
            boardId = boardId,
            sort = sort,
            filter = if (favoriteOnly) "FAVORITE" else "ALL",
            page = page,
            size = size
        )
    }
}
