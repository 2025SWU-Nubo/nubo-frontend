package com.example.nubo.ui.screen.recommendCard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.RecommendCardDetailResponse
import com.example.nubo.data.model.SaveRecommendationCardRequest
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.card.RecommendCardDetailItem
import dagger.hilt.android.lifecycle.HiltViewModel
import formatIsoDateToDisplayLegacy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

// UI state for recommend card detail
sealed interface RecommendDetailUiState {
    data object Loading : RecommendDetailUiState
    data class Success(val item: RecommendCardDetailItem) : RecommendDetailUiState
    data class Error(val message: String) : RecommendDetailUiState
}

// UI state for save action
sealed interface SaveUiState {
    data object Idle : SaveUiState
    data object Saving : SaveUiState
    data class Success(val cardId: Int) : SaveUiState
    data class Error(val message: String) : SaveUiState
}


@HiltViewModel
class RecommendCardDetailViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val recommendationCardId: Int = checkNotNull(savedStateHandle["cardId"])

    private val _uiState = MutableStateFlow<RecommendDetailUiState>(RecommendDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val saveState = _saveState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = RecommendDetailUiState.Loading

            val token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                _uiState.value = RecommendDetailUiState.Error(
                    "인증 토큰이 없습니다. 다시 로그인해주세요."
                )
                return@launch
            }

            // Repository already returns Result<RecommendCardDetailResponse>
            cardRepository.getRecommendCardDetail(token, recommendationCardId)
                .onSuccess { res ->
                    // map to UI model
                    _uiState.value = RecommendDetailUiState.Success(res.toUi())
                }
                .onFailure { e ->
                    _uiState.value = RecommendDetailUiState.Error(e.humanMessage())
                }
        }
    }


    fun saveToMyCards(boardIds: List<Int>? = null) {
        viewModelScope.launch {
            val token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                _saveState.value = SaveUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            _saveState.value = SaveUiState.Saving

            // Use repository Result API
            cardRepository.saveRecommendationCard(
                token = token,
                recommendationCardId = recommendationCardId,
                boardIds = boardIds
            ).onSuccess { res ->
                _saveState.value = SaveUiState.Success(res.cardId)
            }.onFailure { e ->
                _saveState.value = SaveUiState.Error(e.humanMessage())
            }
        }
    }


    fun resetSaveState() {
        _saveState.value = SaveUiState.Idle
    }


    private fun Throwable.humanMessage(): String = when (this) {
        is HttpException -> "서버 오류(${code()})가 발생했습니다"
        else -> message ?: "알 수 없는 오류가 발생했습니다."
    }

    // Map recommend detail response → CardDetailItem (reuse existing UI model)
    private fun RecommendCardDetailResponse.toUi(): RecommendCardDetailItem {
        return RecommendCardDetailItem(
            recommendationCardId = recommendationCardId,   // 아직 내 카드 id 는 아님
            title = title,
            summary = summary,
            tags = tags,
            videoUrl = videoUrl,
            videoThumbnailUrl = videoThumbnailUrl,
            videoPlatform = videoPlatform,
            aiCategoryName = aiCategoryName,
            createdAt = formatIsoDateToDisplayLegacy(createdAt),
            updatedAt = formatIsoDateToDisplayLegacy(updatedAt),
            username = username,
            matchPercent = matchPercent
        )
    }
}
