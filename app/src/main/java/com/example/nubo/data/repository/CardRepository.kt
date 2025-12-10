package com.example.nubo.data.repository

import com.example.nubo.data.model.*
import com.example.nubo.data.network.CardService
import com.example.nubo.data.network.CardSort
import com.example.nubo.domain.model.CardFilter
import retrofit2.Call
import javax.inject.Inject

class CardRepository @Inject constructor(
    private val apiService: CardService
) {

    // 카드 목록 조회
    suspend fun getCards(
        sort: CardSort? = CardSort.LATEST,
        filter: CardFilter?,
        page: Int = 0,
        size: Int = 20
    ): Result<PagedResponse<CardResponse>> = runCatching {
        apiService.getCards(
            accept = "application/json",
            sort = sort?.name,
            filter = filter?.name,
            page = page,
            size = size
        )
    }

    // 미시청 전체 카드 조회
    fun getUnviewedAllCards(limit: Int = 20): Call<List<CardResponse>> {
        return apiService.getUnviewedAllCards(
            accept = "application/json",
            limit = limit
        )
    }

    fun getUnviewedCardsByBoard(boardId: Long, limit: Int = 10): Call<List<CardResponse>> {
        return apiService.getUnviewedCardsByBoard(
            boardId = boardId,
            limit = limit
        )
    }

    // 카드 업로드
    fun uploadCard(request: CardUploadRequest): Call<CardUploadResponse> {
        return apiService.uploadCard(
            accept = "application/json",
            request = request
        )
    }

    // 상세 카드 조회
    fun getCardDetail(cardId: Int): Call<CardDetailResponse> {
        return apiService.getCardDetail(
            accept = "application/json",
            cardId = cardId
        )
    }

    // 요약 노트 수정
    fun updateCardSummary(cardId: Int, body: EditSummaryRequest): Call<EditSummaryResponse> {
        return apiService.updateCardSummary(
            accept = "application/json",
            cardId = cardId,
            body = body
        )
    }

    // AI 요약 노트 수정
    fun updateSummaryWithAi(cardId: Int, body: EditSummaryAiRequest): Call<EditSummaryResponse> {
        return apiService.updateCardSummaryWithAi(
            accept = "application/json",
            cardId = cardId,
            body = body
        )
    }

    // 즐겨찾기 수정
    fun updateFavorite(cardId: Int, body: CardFavoriteRequest): Call<CardFavoriteResponse> {
        return apiService.updateCardFavorite(
            accept = "application/json",
            cardId = cardId,
            body = body
        )
    }

    // 추천 카드 목록
    suspend fun getRecommendCards(): Result<RecommendCardResponse> = runCatching {
        apiService.recommendCards(
            accept = "application/json"
        )
    }

    // 추천 카드 상세
    suspend fun getRecommendCardDetail(
        recommendationCardId: Int
    ): Result<RecommendCardDetailResponse> = runCatching {
        apiService.getRecommendationCardDetail(
            accept = "application/json",
            id = recommendationCardId
        )
    }

    // 추천 카드 저장
    suspend fun saveRecommendationCard(
        recommendationCardId: Int,
        boardIds: List<Int>? = null
    ): Result<SaveRecommendationCardResponse> = runCatching {
        apiService.saveRecommendationCard(
            accept = "application/json",
            body = SaveRecommendationCardRequest(
                recommendationCardId = recommendationCardId,
                boardIds = boardIds
            )
        )
    }
}
