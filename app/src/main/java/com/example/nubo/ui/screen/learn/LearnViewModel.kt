package com.example.nubo.ui.screen.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.DashboardResponse
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.LearnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val data: DashboardResponse) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val learnRepository: LearnRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            val token = authRepository.getAccessToken()
            if (token == null) {
                _uiState.value = DashboardUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            learnRepository.getDashboardStats(token)
                .onSuccess { data ->
                    _uiState.value = DashboardUiState.Success(data)
                }
                .onFailure { error ->
                    _uiState.value = DashboardUiState.Error(error.message ?: "데이터를 불러오는데 실패했습니다.")
                }
        }
    }
}
