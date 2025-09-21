package com.example.nubo.data.repository

import com.example.nubo.data.model.BoardItemResponse
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.model.UpsertBoardRequest
import com.example.nubo.data.network.BoardService
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

    suspend fun getMyBoards(token: String): Result<List<BoardResponse>> = runCatching {
        boardService.getMyBoards(
            authHeader = "Bearer $token",
            acceptHeader = "application/json"
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


}
