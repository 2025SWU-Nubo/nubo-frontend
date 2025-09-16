package com.example.nubo.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.ProfileResponse
import com.example.nubo.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val data: ProfileResponse? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { refresh() }

    // 프로필 확인
    fun refresh() {
        _uiState.value = ProfileUiState(isLoading = true)
        viewModelScope.launch {
            profileRepository.fetchProfile()
                .onSuccess { resp ->
                    _uiState.value = ProfileUiState(isLoading = false, data = resp)
                }
                .onFailure { t ->
                    _uiState.value = ProfileUiState(isLoading = false, error = t.message)
                }
        }
    }

    // 닉네임 변경 API 호출
    fun updateName(newName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            profileRepository.updateNickname(newName)
                .onSuccess {
                    // 성공: 최신 데이터 재조회 후 콜백
                    refresh()
                    onResult(true, null)
                }
                .onFailure { e ->
                    onResult(false, e.message ?: "이름 변경에 실패했습니다.")
                }
        }
    }

    //override로 들어온 이름을 UI 상태에 즉시 반영
    fun applyLocalName(newName: String) {
        val cur = _uiState.value.data
        if (cur != null) {
            _uiState.value = _uiState.value.copy(
                data = cur.copy(name = newName)
            )
        }
    }
}
