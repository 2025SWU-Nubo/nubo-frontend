package com.example.nubo.data.repository

import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.model.EditSummaryAiRequest
import com.example.nubo.data.model.EditSummaryRequest
import com.example.nubo.data.model.EditSummaryResponse
import com.example.nubo.data.network.CardService
import com.example.nubo.data.network.CardSort
import retrofit2.Call
import javax.inject.Inject

//hilt di 적용
class CardRepository @Inject constructor(private val apiService: CardService) {

    fun getCards(token: String, sort: CardSort?, page: Int?, size: Int?) =
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

    // 요약 노트 수정
    fun updateCardSummary(token: String, cardId: Int,body: EditSummaryRequest):Call<EditSummaryResponse>{
        return apiService.updateCardSummary("Bearer $token", "application/json", cardId, body)
    }

    // ai 요약 노트 수정
    fun updateSummaryWithAi(token: String,cardId: Int,body: EditSummaryAiRequest):Call<EditSummaryResponse>{
        return apiService.updateCardSummaryWithAi("Bearer $token", "application/json", cardId, body)
    }
}
