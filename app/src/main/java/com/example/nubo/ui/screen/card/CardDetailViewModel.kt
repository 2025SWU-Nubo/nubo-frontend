package com.example.nubo.ui.screen.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.model.card.CardDetailItem
import dagger.hilt.android.lifecycle.HiltViewModel
import formatIsoDateToDisplayLegacy
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.await

// UI state sealed class
sealed interface CardDetailUiState {
    data object Loading : CardDetailUiState
    data class Success(val item: CardDetailItem) : CardDetailUiState
    data class Error(val message: String) : CardDetailUiState
}


@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: CardRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Read required nav arg
    private val cardId: Int = checkNotNull(savedStateHandle["cardId"])

    private val _uiState = MutableStateFlow<CardDetailUiState>(CardDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    // Load card detail by id
     private fun load() {
        viewModelScope.launch {
            _uiState.value = CardDetailUiState.Loading

            val token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                _uiState.value = CardDetailUiState.Error("인증 토큰이 없습니다. 다시 로그인해주세요.")
                return@launch
            }
            runCatching {
                // ⚠️ Replace with your actual API call
                val res: CardDetailResponse = repository.getCardDetail(token, cardId).await()
                res.toUi()
            }.onSuccess { ui ->
                _uiState.value = CardDetailUiState.Success(ui)
            }.onFailure { e ->
                val msg = when (e) {
                is HttpException -> when (e.code()) {
                    401, 403 -> "세션이 만료되었어요. 다시 로그인해주세요."
                    404 -> "해당 카드를 찾을 수 없습니다."
                    else -> "서버 오류(${e.code()})가 발생했습니다."
                }
                else -> e.message ?: "알 수 없는 오류가 발생했습니다."
            }
                _uiState.value = CardDetailUiState.Error(msg)
            }
        }
    }


    // 요약 노트 수정 요약 노트 리프레쉬 함수
    fun refresh() { load() }
}


// Mapper from API model to UI model
private fun CardDetailResponse.toUi(): CardDetailItem {
    // NOTE: Adjust fields to match your real response model
    return CardDetailItem(
        id = cardId,
        imageUrl = videoThumbnailUrl.orEmpty(),
        videoUrl = videoUrl.orEmpty(),
        title = title ?: "제목 없음",
        category = boardName ?: "카테고리 없음",
        boardSource = boardSource.orEmpty(),
        description = summary ?: "설명 없음",
        date = formatIsoDateToDisplayLegacy(createdAt), // 기존 유틸 재사용
        videoPlatform = videoPlatform ?: "알 수 없음"
    )
}
