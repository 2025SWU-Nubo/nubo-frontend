package com.example.nubo.ui.screen.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.dto.HighlightDto
import com.example.nubo.data.model.EditSummaryRequest
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.await

sealed interface EditCardUiState {
    data object Loading : EditCardUiState
    data class Ready(
        val summary: String,
        val highlights: List<HighlightDto>
    ) : EditCardUiState
    data class Error(val message: String) : EditCardUiState
    data object Saving : EditCardUiState
    data object Saved : EditCardUiState
}

@HiltViewModel
class EditCardViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Int = checkNotNull(savedStateHandle["cardId"])

    private val _uiState = MutableStateFlow<EditCardUiState>(EditCardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init { bootstrap() }

    // Load initial summary/highlights to edit
    private fun bootstrap() = viewModelScope.launch {
        val token = authRepository.getAccessToken()
        if (token.isNullOrEmpty()) {
            _uiState.value = EditCardUiState.Error("인증 토큰이 없습니다. 다시 로그인해주세요.")
            return@launch
        }
        runCatching {
            // Reuse existing detail API to pre-fill editing fields
            val detail = cardRepository.getCardDetail(token, cardId).await()
            EditCardUiState.Ready(
                summary = detail.summary.orEmpty(),
                highlights = detail.highlights?: emptyList() // <- 서버가 제공한다면
            )
        }.onSuccess { _uiState.value = it }
            .onFailure { _uiState.value = EditCardUiState.Error(it.humanMessage()) }
    }

    // Update local summary text
    fun updateSummary(newText: String) {
        val s = _uiState.value
        if (s is EditCardUiState.Ready) {
            _uiState.value = s.copy(summary = newText)
        }
    }

    // Toggle highlight by selection range (add if absent, remove if exact match exists)
    fun toggleHighlight(start: Int, end: Int) {
        val s = _uiState.value
        if (s is EditCardUiState.Ready) {
            if (start == end) return // ignore empty selection

            val normalizedStart = minOf(start, end)
            val normalizedEnd = maxOf(start, end)

            val existing = s.highlights.any { it.rangeStart == normalizedStart && it.rangeEnd == normalizedEnd }
            val next = if (existing) {
                s.highlights.filterNot { it.rangeStart == normalizedStart && it.rangeEnd == normalizedEnd }
            } else {
                s.highlights + HighlightDto(normalizedStart, normalizedEnd)
            }
            _uiState.value = s.copy(highlights = next)
        }
    }

    // Save to server once (summary + highlights)
    fun save(onSuccess: () -> Unit = {}) = viewModelScope.launch {
        val token = authRepository.getAccessToken()
        val s = _uiState.value
        if (token.isNullOrEmpty() || s !is EditCardUiState.Ready) return@launch

        _uiState.value = EditCardUiState.Saving
        runCatching {
            val body = EditSummaryRequest(
                summary = s.summary,
                highlights = s.highlights
            )
            cardRepository.updateCardSummary(token, cardId, body).await()
        }.onSuccess { resp ->
            // Server returns canonicalized summary/highlights; adopt them
            _uiState.value = EditCardUiState.Ready(
                summary = resp.summary,
                highlights = resp.highlights
            )
            _uiState.value = EditCardUiState.Saved
            onSuccess()
        }.onFailure {
            _uiState.value = EditCardUiState.Error(it.humanMessage())
        }
    }
}

// --- small helper for human-readable errors
private fun Throwable.humanMessage(): String = when (this) {
    is HttpException -> "서버 오류(${code()})가 발생했습니다."
    else -> message ?: "알 수 없는 오류가 발생했습니다."
}

