package com.example.nubo.data.repository

import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.network.CardService
import retrofit2.Call
import javax.inject.Inject

//hilt di 적용
class CardRepository @Inject constructor(private val apiService: CardService) {

    fun getCards(token: String, sort: String, page: Int?, size: Int?) =
        apiService.getCards(token, "application/json", sort, page, size)

    fun getUnviewedCardsByBoard(token: String, boardId: Long, limit: Int = 10): Call<List<CardResponse>> {
        return apiService.getUnviewedCardsByBoard("Bearer $token", "application/json", boardId, limit)
    }

    fun uploadCard(
        token: String,
        request: CardUploadRequest
    ): Call<CardUploadResponse> {
        return apiService.uploadCard("Bearer $token", request = request)
    }

    //상세 카드 조회
    fun getCardDetail(token: String, cardId: Int): Call<CardDetailResponse> {
        return apiService.getCardDetail("Bearer $token", cardId = cardId)
    }
}
