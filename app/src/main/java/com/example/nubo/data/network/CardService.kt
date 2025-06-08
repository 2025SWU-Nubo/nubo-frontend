package com.example.nubo.data.network

import com.example.nubo.data.model.CardItemDto
import retrofit2.http.GET
import retrofit2.http.Path

interface CardService {
    //나의 카드
    @GET("/api/cards")
    suspend fun getMyCards(): List<CardItemDto>

    //보드 상세 카드
    @GET("/api/board/{id}/cards")
    suspend fun getBoardCards(
        @Path("id") boardId: String
    ): List<CardItemDto>
}
