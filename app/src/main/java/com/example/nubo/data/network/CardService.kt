package com.example.nubo.data.network

import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.EditSummaryAiRequest
import com.example.nubo.data.model.EditSummaryRequest
import com.example.nubo.data.model.EditSummaryResponse
import com.example.nubo.data.model.PagedResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

enum class CardSort { LATEST, POPULAR }

interface CardService {
    @GET("api/card")
    suspend fun getCards(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Query("sort") sort: String? = null,    // LATEST | OLDEST | ALPHABET
        @Query("filter") filter: String? = null,// ALL | FAVORITE | SHARED
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): PagedResponse<CardResponse>

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

    @PATCH("/api/card/{cardId}/summary")
    fun updateCardSummary(
        @Header("Authorization") bearer: String,
        @Header("Accept") accept: String = "application/json",
        @Path("cardId") cardId: Int,
        @Body body: EditSummaryRequest
    ): Call<EditSummaryResponse>

    @PATCH("/api/card/{cardId}/summary/ai")
    fun updateCardSummaryWithAi(
        @Header("Authorization") bearer: String,
        @Header("Accept") accept: String = "application/json",
        @Path("cardId") cardId: Int,
        @Body body: EditSummaryAiRequest
    ): Call<EditSummaryResponse>

}
