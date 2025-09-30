package com.example.nubo.data.repository

import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardFavoriteRequest
import com.example.nubo.data.model.CardFavoriteResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.model.EditSummaryAiRequest
import com.example.nubo.data.model.EditSummaryRequest
import com.example.nubo.data.model.EditSummaryResponse
import com.example.nubo.data.model.PagedResponse
import com.example.nubo.data.network.CardService
import com.example.nubo.data.network.CardSort
import com.example.nubo.domain.model.CardFilter
import retrofit2.Call
import javax.inject.Inject

//hilt di 적용
class CardRepository @Inject constructor(private val apiService: CardService) {

    suspend fun getCards(
        token: String,
        sort: CardSort? = CardSort.LATEST,
        filter: CardFilter? = CardFilter.ALL,
        page: Int = 0,
        size: Int = 20
    ): Result<PagedResponse<CardResponse>> = runCatching {
        apiService.getCards(
            authorization = "Bearer $token",
            sort = sort?.name,          // ensures UPPERCASE
            filter = filter?.name,
            page = page,
            size = size
        )
    }

    fun getUnviewedCardsByBoard(token: String, boardId: Long, limit: Int = 10): Call<List<CardResponse>> {
        return apiService.getUnviewedCardsByBoard("Bearer $token", boardId, limit)
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

    // 카드 즐겨찾기 수정
    fun updateFavorite(token: String,cardId:Int,body: CardFavoriteRequest):Call<CardFavoriteResponse>{
        return apiService.updateCardFavorite("Bearer $token", "application/json", cardId, body)
    }
}
