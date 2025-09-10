package com.example.nubo.ui.component.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardWithSectionsResponse
import com.example.nubo.data.network.VideoService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 내부에 UI 표시용 모델로 변환하기 위한 data class ---
data class UiBoardNode(
    val id: Long,
    val title: String,
    val children: List<UiBoardNode> = emptyList(), // 섹션들
)

// 서버 검증 호출과 임시 URL 저장을 담당하는 뷰모델
@HiltViewModel
class AddVideoViewModel @Inject constructor(
    private val videoService: VideoService
) : ViewModel() {

    // UI 상태: Idle/Loading/Success(유효)/Invalid(무효)/Error(에러)
    sealed class ValidateState {
        data object Idle : ValidateState()
        data object Loading : ValidateState()
        data class Success(val platform: String?) : ValidateState()
        data object Invalid : ValidateState()
        data class Error(val message: String) : ValidateState()
    }

    // 현재 검증 상태
    private val _state = MutableStateFlow<ValidateState>(ValidateState.Idle)
    val state: StateFlow<ValidateState> = _state

    // 다음 단계(보드 선택)에서 다시 서버로 보낼 "검증 통과한 URL" 보관
    var rememberedUrl: String? = null
        private set

    // 검증 호출
    fun validate(url: String) {
        viewModelScope.launch {
            _state.value = ValidateState.Loading
            rememberedUrl = null

            try {
                val res = videoService.validateLink(url)
                if (res.isSuccessful) {
                    val body = res.body()
                    if (body?.valid == true) {
                        rememberedUrl = url     // 보드 선택 단계에서 재사용
                        _state.value = ValidateState.Success(body.platform)
                    } else {
                        _state.value = ValidateState.Invalid
                    }
                } else {
                    _state.value = ValidateState.Error("서버 오류: ${res.code()}")
                }
            } catch (e: Exception) {
                _state.value = ValidateState.Error("네트워크 오류: ${e.localizedMessage ?: "알 수 없음"}")
            }
        }
    }

    // --- 보드 트리 로딩 상태 ---
    sealed class BoardsState {
        data object Idle : BoardsState()
        data object Loading : BoardsState()
        data class Loaded(val boards: List<UiBoardNode>) : BoardsState()
        data class Error(val message: String) : BoardsState()
    }

    private val _boards = MutableStateFlow<BoardsState>(BoardsState.Idle)
    val boards: StateFlow<BoardsState> = _boards

    // 토큰은 Hilt로 AuthRepository를 받거나, 호출 측에서 넘겨줘도 됨.
    // 여기서는 호출 측(Compose)에서 accessToken을 파라미터로 넘겨준다고 가정.
    fun loadBoards(accessToken: String) {
        if (_boards.value is BoardsState.Loading) return
        viewModelScope.launch {
            _boards.value = BoardsState.Loading
            try {
                val res = videoService.getBoardsWithSections("Bearer $accessToken")
                if (res.isSuccessful) {
                    val body = res.body().orEmpty()
                    _boards.value = BoardsState.Loaded(body.toUiNodes())
                } else {
                    _boards.value = BoardsState.Error("서버 오류: ${res.code()}")
                }
            } catch (e: Exception) {
                _boards.value = BoardsState.Error("네트워크 오류: ${e.localizedMessage ?: "알 수 없음"}")
            }
        }
    }

    // 서버 DTO → UI 노드 변환
    private fun List<BoardWithSectionsResponse>.toUiNodes(): List<UiBoardNode> {
        return map { board ->
            UiBoardNode(
                id = board.id,
                title = board.name,
                children = board.sections.map { sec ->
                    UiBoardNode(id = sec.id, title = sec.name)
                }
            )
        }
    }
}
