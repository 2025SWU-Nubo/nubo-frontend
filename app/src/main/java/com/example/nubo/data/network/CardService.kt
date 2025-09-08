package com.example.nubo.data.network

import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.model.CardDetailResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CardService {
    @GET("api/card")
    fun getCards(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Query("sort") sort: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): Call<List<CardResponse>>


    @POST("api/card")
    fun uploadCard(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Body request: CardUploadRequest
    ): Call<CardUploadResponse>

    @GET("api/card/{cardId}")
    fun getCardDetail(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("cardId") cardId: Int
    ): Call<CardDetailResponse>

    @GET("/api/home/boards/{boardId}/unviewed-cards")
    fun getUnviewedCardsByBoard(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("boardId") boardId: Long,
        @Query("limit") limit: Int = 10
    ): Call<List<CardResponse>>

}
