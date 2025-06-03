package com.example.nubo.data.network

import com.example.nubo.data.model.CardItemDto
import retrofit2.http.GET

interface CardService {
    @GET("/api/cards")
    suspend fun getMyCards(): List<CardItemDto>
}
