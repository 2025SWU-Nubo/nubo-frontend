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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.await


/** 편집 ui 상태 **/
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

    /* 라우트 인자로 전달된 cardId */
    private val cardId: Int = checkNotNull(savedStateHandle["cardId"])

    /* 화면 전체 상태 */
    private val _uiState = MutableStateFlow<EditCardUiState>(EditCardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    /* Ai 프롬프트 바 표시 여부 */
    private val _showAiBar = MutableStateFlow(false)
    val showAiBar = _showAiBar.asStateFlow()

    /* Ai 프롬프트 입력값 */
    private val _aiQuery = MutableStateFlow("")
    val aiQuery = _aiQuery.asStateFlow()

    /* AI 호출 로딩 상태 */
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    /* 토스트용 단발성 메시지  화면에서 수집 후 consume */
    private val _toast = MutableStateFlow<String?>(null)
    val toast = _toast.asStateFlow()

    /* 되돌리기용 백업 텍스트  AI 적용 전의 summary 보관 */
    private var prevSummaryBackup: String? = null

    /* 되돌리기 칩 활성화 여부 */
    private val _canUndoAiEdit = MutableStateFlow(false)
    val canUndoAiEdit: StateFlow<Boolean> = _canUndoAiEdit

    init { bootstrap() }

    /* 초기 데이터 로드  카드 상세에서 요약과 하이라이트 미리 채움 */
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

    /* 요약 텍스트 로컬 반영  타이핑이나 툴바 적용 시 호출 */
    fun updateSummary(newText: String) {
        val s = _uiState.value
        if (s is EditCardUiState.Ready) {
            _uiState.value = s.copy(summary = newText)
        }
    }

    init {
        bootstrap()
    }

    /* 하이라이트 토글  같은 구간이 있으면 제거 없으면 추가 */
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

    /* 저장 요청  서버의 정규화된 응답으로 다시 Ready 설정 */
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
            _toast.value = "저장되었습니다."
        }.onFailure {
            _uiState.value = EditCardUiState.Error(it.humanMessage())
            _toast.value = it.humanMessage()
        }
    }

    /* AI 바 토글  null이면 반전 */
    fun toggleAiBar(show: Boolean? = null) {
        _showAiBar.value = show ?: !_showAiBar.value
    }

    /* AI 프롬프트 입력 변경 */
    fun onAiQueryChange(text: String) {
        _aiQuery.value = text
    }

    /* 토스트 소비  화면에서 스낵바로 노출 후 호출 */
    fun consumeToast() {
        _toast.value = null
    }

    /* AI 적용 전 현재 요약 백업하고 되돌리기 활성화 */
    fun startAiEditBackup(currentSummary: String) {
        prevSummaryBackup = currentSummary
        _canUndoAiEdit.value = true
    }

    /* 서버에서 받은 AI 편집 결과를 화면 상태에 반영 */
    fun applyAiEditedSummary(newSummary: String) {
        updateSummary(newSummary)
        /* 되돌리기는 유지  사용자가 원하면 즉시 복원 가능 */
    }

    /* AI 편집 되돌리기  백업이 있으면 복원하고 비활성화 */
    fun undoAiEdit() {
        val backup = prevSummaryBackup ?: return
        updateSummary(backup)
        prevSummaryBackup = null
        _canUndoAiEdit.value = false
        _toast.value = "되돌리기 완료"
    }

    /* 사용자가 추가 수정한 경우 등  되돌리기 의미가 없어지면 호출 */
    fun clearUndoIfNotNeeded() {
        prevSummaryBackup = null
        _canUndoAiEdit.value = false
    }

    /* AI 편집 요청
       절차
       1 현재 요약 백업 및 되돌리기 활성화
       2 서버에 프롬프트와 함께 편집 요청
       3 성공 시 응답 요약을 적용  프롬프트 초기화  성공 토스트
       4 실패 시 되돌리기 상태는 그대로 두고 에러 토스트  사용자가 원하면 되돌리기 가능 */
    fun requestAiEdit() = viewModelScope.launch {
        val token = authRepository.getAccessToken()
        val s = _uiState.value
        val prompt = _aiQuery.value.trim()
        if (token.isNullOrEmpty() || s !is EditCardUiState.Ready || prompt.isEmpty() || _aiLoading.value) return@launch

        /* 1 현재 요약 백업 */
        startAiEditBackup(s.summary)

        _aiLoading.value = true
        runCatching {
            val body = EditSummaryAiRequest(prompt)
            cardRepository.updateSummaryWithAi(token, cardId, body).await()
        }.onSuccess { resp ->
            /* 3 성공  응답 채택 후 프롬프트 초기화  UI 토스트 */
            applyAiEditedSummary(resp.summary)
            _aiQuery.value = ""
            _toast.value = "AI가 요약노트를 정리했어요!👍🏻"
            /* AI 바를 자동으로 닫고 싶다면 아래 주석 해제
               키보드 유지가 필요하면 닫지 않고 그대로 둬도 됨 */
            // _showAiBar.value = false
        }.onFailure { e ->
            /* 4 실패  되돌리기 가능 상태 유지  에러 토스트 */
            _toast.value = e.humanMessage()
        }
        _aiLoading.value = false
    }
}

/* 사용자에게 읽기 쉬운 오류 메시지로 변환 */
private fun Throwable.humanMessage(): String = when (this) {
    is HttpException -> "서버 오류(${code()})가 발생했습니다"
    else -> message ?: "알 수 없는 오류가 발생했습니다"
}



