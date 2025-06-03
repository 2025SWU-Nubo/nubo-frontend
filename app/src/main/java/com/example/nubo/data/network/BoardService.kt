package com.example.nubo.data.network

import com.example.nubo.data.model.BoardResponse
import retrofit2.http.GET

interface BoardService {
    @GET("/api/boards")
    suspend fun getMyBoards(): List<BoardResponse>
}
