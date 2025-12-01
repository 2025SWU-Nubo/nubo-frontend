package com.example.nubo.ui.component.sheet


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.domain.model.InviteUser
import com.example.nubo.domain.repository.InviteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val repo: InviteRepository
) : ViewModel(){
    // 검색어 상태
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // 선택된 사용자
    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    // 선택된 사용자 전체 정보 (썸네일, 닉네임 포함)
    private val _selectedUsers = MutableStateFlow<List<InviteUser>>(emptyList())
    val selectedUsers: StateFlow<List<InviteUser>> = _selectedUsers.asStateFlow()


    // 선택 여부 플래그
    val hasSelection: StateFlow<Boolean> =
        _selected.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)


    // UI state (loading, results, error)
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InviteUiState> =
        _query
            .debounce(300)              // debouncing input
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { keyword ->
                if (keyword.length < 2) {
                    flowOf<InviteUiState>(InviteUiState.Idle)
                } else {
                    flow {
                        emit(InviteUiState.Loading)
                        try {
                            val users = repo.searchByEmail(keyword) // returns List<InviteUser>
                            emit(InviteUiState.Success(users))
                        } catch (e: HttpException) {
                            // 상태코드별 메시지 (403 특화)
                            val msg = if (e.code() == 403) {
                                "권한이 없거나 토큰이 유효하지 않습니다. 다시 로그인하거나 잠시 후 시도해주세요."
                            } else {
                                "요청 실패 (${e.code()})"
                            }
                            emit(InviteUiState.Error(msg))
                        }catch (t: Throwable) {
                            emit(InviteUiState.Error(t.message ?: "알 수 없는 오류"))
                        }
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                InviteUiState.Idle
            )

    // 검색어 변경
    fun onQueryChange(newValue: String) {
        _query.value = newValue
    }

    // 선택 토글
    fun toggleSelect(user: InviteUser) {
        _selected.update { current ->
            val isSelected = user.email in current
            val newSet =
                if (isSelected) current - user.email else current + user.email

            _selectedUsers.update { list ->
                if (user.email in newSet) {
                    // Add or update this user in the selected list
                    val without = list.filterNot { it.email == user.email }
                    without + user
                } else {
                    // Remove this user from the selected list
                    list.filterNot { it.email == user.email }
                }
            }

            newSet
        }
    }

    // 선택 초기화
    fun clearSelection() {
        _selected.value = emptySet()
        _selectedUsers.value = emptyList()
    }

    //검색화 초기화
    fun clearQuery() {
        _query.value = ""
    }


    // 외부에서 선택 복원
    fun setSelection(emails: Collection<String>) {
        _selected.value = emails.toSet()
    }

    // 다중 해제
    fun removeFromSelection(emails: Collection<String>) {
        if (emails.isEmpty()) return
        _selected.update { it - emails.toSet() }
    }

    // 모두 리셋
    fun resetAll() {
        _query.value = ""
        _selected.value = emptySet()
        _selectedUsers.value = emptyList()
    }

    // 초대 실행
    fun inviteSelected(
        onSuccess: (Set<String>) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // TODO: call real server API with _selected.value
                // repo.invite(_selected.value)

                onSuccess(_selected.value)
                // (Optional) Clear after successful invite:
                // _selected.value = emptySet()

            } catch (t: Throwable) {
                onError(t)
            }
        }
    }



}
