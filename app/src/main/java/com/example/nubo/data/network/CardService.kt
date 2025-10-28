package com.example.nubo.data.network

import com.example.nubo.data.model.CardDeleteRequest
import com.example.nubo.data.model.CardDeleteResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardFavoriteRequest
import com.example.nubo.data.model.CardFavoriteResponse
import com.example.nubo.data.model.CardRestoreRequest
import com.example.nubo.data.model.CardRestoreResponse
import com.example.nubo.data.model.EditSummaryAiRequest
import com.example.nubo.data.model.EditSummaryRequest
import com.example.nubo.data.model.EditSummaryResponse
import com.example.nubo.data.model.PagedResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

enum class CardSort { LATEST, OLDEST,ALPHABET }

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

    @Headers("Accept: application/json")
    @GET("/api/home/boards/{boardId}/unviewed-cards")
    fun getUnviewedCardsByBoard(
        @Header("Authorization") token: String,
        @Path("boardId") boardId: Long,
        @Query("limit") limit: Int = 10
    ): Call<List<CardResponse>>

    @GET("/api/home/boards/all/unviewed-cards")
    fun getUnviewedAllCards(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Query("limit") limit: Int = 20
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

    // 즐겨 찾기 업데이트
    @PATCH("/api/card/{cardId}/favorite")
    fun updateCardFavorite(
        @Header("Authorization") bearer: String,
        @Header("Accept") accept: String = "application/json",
        @Path("cardId") cardId: Int,
        @Body body: CardFavoriteRequest
    ): Call<CardFavoriteResponse>

    // 카드 삭제/제거 API
    @HTTP(method = "DELETE", path = "/api/card", hasBody = true)
    suspend fun deleteCards(
        @Header("Authorization") authHeader: String,
        @Body body: CardDeleteRequest
    ): Response<CardDeleteResponse>

    // 카드 복구 API
    @PATCH("/api/card/restore")
    suspend fun restoreCards(
        @Header("Authorization") authHeader: String,
        @Body body: CardRestoreRequest
    ): CardRestoreResponse

}
