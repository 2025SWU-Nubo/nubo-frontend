package com.example.nubo.data.network

import com.example.nubo.data.model.BoardResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface BoardService {
    //나의 보드
    @GET("/api/boards")
    suspend fun getMyBoards(): List<BoardResponse>

    //보드 상세 섹션
    @GET("/api/board/{id}")
    suspend fun getBoardDetail(
        @Path("id") boardId: String
    ): BoardResponse
}
