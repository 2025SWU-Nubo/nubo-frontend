package com.example.nubo.ui.screen.interest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.DefaultBoardItemResponse
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.InterestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 관심사 온보딩 화면 상태
 * - boards: 서버에서 받은 기본 보드 목록
 * - selectedIds: 사용자가 선택한 보드 ID 집합(중복 방지)
 * - isLoading: 목록 로딩 여부
 * - submitInProgress: 제출 중 여부
 * - error: 에러 메시지(문자열)
 * - canSubmit: '시작하기' 버튼 활성화 여부(선택≥1 && 제출중 아님)
 */
data class InterestUiState(
    val boards: List<DefaultBoardItemResponse> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val submitInProgress: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean get() = selectedIds.isNotEmpty() && !submitInProgress
}

@HiltViewModel
class OnBoardingInterestViewModel @Inject constructor(
    private val repo: InterestRepository,
    private val authRepository: AuthRepository
) : ViewModel(){
    private val _state = MutableStateFlow(InterestUiState())
    val state: StateFlow<InterestUiState> = _state

    // 토큰을 외부에 노출: 화면에서 collect해서 사용
    val accessToken: StateFlow<String?> = authRepository.accessTokenFlow

    /** 기본 보드 목록 조회 트리거 */
    fun loadBoards(accessToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repo.loadDefaultBoards(accessToken) }
                .onSuccess { list -> _state.update { it.copy(isLoading = false, boards = list) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "목록을 불러오지 못했어요")
                    }
                }
        }
    }

    /** 칩 탭 시 선택/해제 토글 */
    fun toggle(boardId: Long) {
        _state.update { s ->
            val next = if (boardId in s.selectedIds) s.selectedIds - boardId else s.selectedIds + boardId
            s.copy(selectedIds = next)
        }
    }

    /**
     * '시작하기' 제출
     * - 성공 응답의 completed=true 면 홈으로 분기(onCompleted 호출)
     * - selectedCount 는 트래킹/로그 용도로 필요 시 활용
     */
    fun submitSelected(
        accessToken: String,
        onCompleted: (selectedCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty() || _state.value.submitInProgress) return

        viewModelScope.launch {
            _state.update { it.copy(submitInProgress = true) }
            runCatching { repo.submitSelectedBoards(accessToken, ids) }
                .onSuccess { resp ->
                    _state.update { it.copy(submitInProgress = false) }
                    if (resp.completed) {
                        //완료로 내부에도 저장
                        authRepository.setInterestCompleted(true)
                        onCompleted(resp.selectedCount)
                    }
                    else onError("완료 상태를 확인하지 못했어요")
                }
                .onFailure { e ->
                    _state.update { it.copy(submitInProgress = false) }
                    onError(e.message ?: "네트워크 오류가 발생했어요")
                }
        }
    }

    /**
    * '건너뛰기' 제출
    * - 서버가 idempotent 처리 → 이미 완료된 유저도 completed=true 반환
    */
    fun submitSkip(
        accessToken: String,
        onCompleted: (selectedCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_state.value.submitInProgress) return

        viewModelScope.launch {
            _state.update { it.copy(submitInProgress = true) }
            runCatching { repo.submitSkip(accessToken) }
                .onSuccess { resp ->
                    _state.update { it.copy(submitInProgress = false) }
                    if (resp.completed) onCompleted(resp.selectedCount)
                    else onError("완료 상태를 확인하지 못했어요")
                }
                .onFailure { e ->
                    _state.update { it.copy(submitInProgress = false) }
                    onError(e.message ?: "네트워크 오류가 발생했어요")
                }
        }
    }

    fun updateSelected(next: Set<Long>) {
        _state.update { it.copy(selectedIds = next) }
    }
}
