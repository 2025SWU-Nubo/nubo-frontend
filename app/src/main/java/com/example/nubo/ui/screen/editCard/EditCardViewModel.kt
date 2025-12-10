package com.example.nubo.ui.screen.editCard

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.await

/** UI state for edit screen **/
sealed interface EditCardUiState {
    data object Loading : EditCardUiState
    data class Ready(
        val summary: String,
        val highlights: List<HighlightDto>
    ) : EditCardUiState

    data class Error(val message: String) : EditCardUiState
    data object Saving : EditCardUiState
    data object Saved : EditCardUiState // kept for completeness, not strictly required
}

sealed interface EditCardUiEvent {
    data class ApplyAiEdit(val markdown: String) : EditCardUiEvent
    data object HideKeyboard : EditCardUiEvent
}

@HiltViewModel
class EditCardViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /* Route argument: cardId */
    private val cardId: Int = checkNotNull(savedStateHandle["cardId"])

    /* Screen state */
    private val _uiState = MutableStateFlow<EditCardUiState>(EditCardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    /* AI prompt bar visibility */
    private val _showAiBar = MutableStateFlow(false)
    val showAiBar = _showAiBar.asStateFlow()

    /* AI prompt text */
    private val _aiQuery = MutableStateFlow("")
    val aiQuery = _aiQuery.asStateFlow()

    /* AI loading flag */
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    /* One-shot toast message */
    private val _toast = MutableStateFlow<String?>(null)
    val toast = _toast.asStateFlow()

    /* Backup text for AI undo feature */
    private var prevSummaryBackup: String? = null

    /* AI undo availability */
    private val _canUndoAiEdit = MutableStateFlow(false)
    val canUndoAiEdit: StateFlow<Boolean> = _canUndoAiEdit

    /* UI events (one-shot) */
    private val _uiEvent = MutableSharedFlow<EditCardUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        bootstrap()
    }

    /** Load initial data from server **/
    private fun bootstrap() = viewModelScope.launch {
        val token = authRepository.getAccessToken()
        if (token.isNullOrEmpty()) {
            _uiState.value = EditCardUiState.Error("인증 토큰이 없습니다. 다시 로그인해주세요.")
            return@launch
        }
        runCatching {
            val detail = cardRepository.getCardDetail( cardId).await()
            EditCardUiState.Ready(
                summary = detail.summary.orEmpty(),
                highlights = detail.highlights ?: emptyList()
            )
        }.onSuccess { ready ->
            _uiState.value = ready
        }.onFailure {
            _uiState.value = EditCardUiState.Error(it.humanMessage())
        }
    }

    /** Update summary locally (typing, toolbar, AI apply, etc.) **/
    fun updateSummary(newText: String) {
        val s = _uiState.value
        if (s is EditCardUiState.Ready) {
            _uiState.value = s.copy(summary = newText)
        }
    }

    /** Toggle highlight range **/
    fun toggleHighlight(start: Int, end: Int) {
        val s = _uiState.value
        if (s is EditCardUiState.Ready) {
            if (start == end) return

            val normalizedStart = minOf(start, end)
            val normalizedEnd = maxOf(start, end)

            val existing = s.highlights.any {
                it.rangeStart == normalizedStart && it.rangeEnd == normalizedEnd
            }
            val next = if (existing) {
                s.highlights.filterNot {
                    it.rangeStart == normalizedStart && it.rangeEnd == normalizedEnd
                }
            } else {
                s.highlights + HighlightDto(normalizedStart, normalizedEnd)
            }
            _uiState.value = s.copy(highlights = next)
        }
    }

    /** Save summary and highlights to server **/
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
            cardRepository.updateCardSummary( cardId, body).await()
        }.onSuccess { resp ->
            // Adopt canonical response from server
            _uiState.value = EditCardUiState.Ready(
                summary = resp.summary,
                highlights = resp.highlights
            )
            _uiState.value = EditCardUiState.Saved
            onSuccess()
            _toast.value = "저장되었습니다."
        }.onFailure { e ->
            _uiState.value = EditCardUiState.Error(e.humanMessage())
            _toast.value = e.humanMessage()
        }
    }

    /** Toggle AI bar; if null, invert current value **/
    fun toggleAiBar(show: Boolean? = null) {
        _showAiBar.value = show ?: !_showAiBar.value
    }

    /** Update AI prompt text **/
    fun onAiQueryChange(text: String) {
        _aiQuery.value = text
    }

    /** Consume toast after UI shows it **/
    fun consumeToast() {
        _toast.value = null
    }

    /** Backup current summary before AI edit **/
    fun startAiEditBackup(currentSummary: String) {
        prevSummaryBackup = currentSummary
        _canUndoAiEdit.value = true
    }

    /** Apply AI-edited summary into state **/
    fun applyAiEditedSummary(newSummary: String) {
        updateSummary(newSummary)
        // undo remains available until user explicitly clears or undoes
    }

    /** Undo AI edit using backup summary **/
    fun undoAiEdit() {
        val backup = prevSummaryBackup ?: return

        updateSummary(backup)

        viewModelScope.launch {
            _uiEvent.emit(EditCardUiEvent.ApplyAiEdit(backup))
        }

        prevSummaryBackup = null
        _canUndoAiEdit.value = false
        _toast.value = "되돌리기 완료!"
    }

    /** Clear undo state when not needed anymore **/
    fun clearUndoIfNotNeeded() {
        prevSummaryBackup = null
        _canUndoAiEdit.value = false
    }

    /** Request AI edit for current summary **/
    fun requestAiEdit() = viewModelScope.launch {
        val token = authRepository.getAccessToken()
        val s = _uiState.value
        val prompt = _aiQuery.value.trim()
        if (token.isNullOrEmpty() || s !is EditCardUiState.Ready || prompt.isEmpty() || _aiLoading.value) {
            return@launch
        }

        // Backup current summary for undo
        startAiEditBackup(s.summary)

        _aiLoading.value = true
        runCatching {
            val body = EditSummaryAiRequest(prompt)
            cardRepository.updateSummaryWithAi( cardId, body).await()
        }.onSuccess { resp ->
            applyAiEditedSummary(resp.summary)
            _aiQuery.value = ""
            _toast.value = "AI가 요약노트를 정리했어요!👍🏻"

            _uiEvent.emit(EditCardUiEvent.ApplyAiEdit(resp.summary))
            _uiEvent.emit(EditCardUiEvent.HideKeyboard)

            _showAiBar.value = false
        }.onFailure { e ->
            val msg = if (e is HttpException && e.code() == 400) {
                "요구 사항을 정확하게 입력해주세요."
            } else {
                e.humanMessage()
            }
            _toast.value = msg
        }
        _aiLoading.value = false
    }
}

/** Convert Throwable to user-friendly message **/
private fun Throwable.humanMessage(): String = when (this) {
    is HttpException -> "서버 오류(${code()})가 발생했습니다"
    else -> message ?: "알 수 없는 오류가 발생했습니다"
}
