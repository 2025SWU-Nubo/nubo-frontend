package com.example.nubo.data.repository

import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.network.CardService
import com.example.nubo.data.network.RetrofitClient.cardApiService
import retrofit2.Call
import javax.inject.Inject

//hilt di 적용
class CardRepository @Inject constructor(private val apiService: CardService) {

    fun getCards(token: String, sort: String, page: Int?, size: Int?) =
        apiService.getCards(token, "application/json", sort, page, size)

    fun uploadCard(
        token: String,
        request: CardUploadRequest
    ): Call<CardUploadResponse> {
        return cardApiService.uploadCard("Bearer $token", request = request)
    }

    //상세 카드 조회
    fun getCardDetail(token: String, cardId: Int): Call<CardDetailResponse> {
        return apiService.getCardDetail("Bearer $token", cardId = cardId)
    }
}
