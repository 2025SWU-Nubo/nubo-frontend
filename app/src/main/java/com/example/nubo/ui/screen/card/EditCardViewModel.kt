package com.example.nubo.ui.screen.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.dto.HighlightDto
import com.example.nubo.data.model.EditSummaryAiRequest
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

    // ai 입력바 관련 상태
    private val _showAiBar = MutableStateFlow(false)
    val showAiBar = _showAiBar.asStateFlow()

    private val _aiQuery = MutableStateFlow("")
    val aiQuery = _aiQuery.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast = _toast.asStateFlow()

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

    // 요약 텍스트 로컬 반영
    fun updateSummary(newText: String) {
        val s = _uiState.value
        if (s is EditCardUiState.Ready) {
            _uiState.value = s.copy(summary = newText)
        }
    }

    // 하이라이트 토글
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

    // 저장 요청
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


    fun toggleAiBar(show: Boolean? =  null){
        _showAiBar.value = show ?: !_showAiBar.value
    }

    fun onAiQueryChange(text:String){_aiQuery.value = text}
    fun consumeToast() { _toast.value = null }

    fun requestAiEdit() = viewModelScope.launch {
        val token = authRepository.getAccessToken()
        val s = _uiState.value
        val prompt = _aiQuery.value.trim()
        if(token.isNullOrEmpty() || s !is EditCardUiState.Ready || prompt.isEmpty() || _aiLoading.value) return@launch

        _aiLoading.value = true
        runCatching {
            val body = EditSummaryAiRequest(prompt)
            cardRepository.updateSummaryWithAi(token,cardId, body).await()
        }.onSuccess { resp ->
            _uiState.value = EditCardUiState.Ready(
                summary = resp.summary,
                highlights = resp.highlights
            )
            _aiQuery.value = ""
            _showAiBar.value = false
        }.onFailure { e ->
        _toast.value = e.humanMessage()
        }
        _aiLoading.value = false
    }

}

// --- small helper for human-readable errors
private fun Throwable.humanMessage(): String = when (this) {
    is HttpException -> "서버 오류(${code()})가 발생했습니다."
    else -> message ?: "알 수 없는 오류가 발생했습니다."
}

