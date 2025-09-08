package com.example.nubo.data.repository

import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.network.BoardService
import javax.inject.Inject

class BoardRepository @Inject constructor(
    private val boardService: BoardService
) {
    suspend fun getMyBoards(token: String): Result<List<BoardResponse>> = runCatching {
        boardService.getMyBoards(
            authHeader = "Bearer $token",
            acceptHeader = "application/json"
        )
    }
}
