package com.example.nubo.ui.screen.learn

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.DashboardResponse
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.LearnRepository
import com.example.nubo.di.UserProgressEventHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val authRepository: AuthRepository,
    private val eventHolder: UserProgressEventHolder,
    application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // 레벨업 이벤트를 UI(Screen)에 전달하기 위한 StateFlow
    // (null이 아니면 '보여줄 이벤트가 있음', 값은 새 스테이지 번호)
    private val _levelUpEvent = MutableStateFlow<Int?>(null)
    val levelUpEvent = _levelUpEvent.asStateFlow()

    // 누베리 획득 이벤트를 UI(Screen)에 전달하기 위한 StateFlow
    private val _berryGainedEvent = MutableStateFlow<Boolean>(false)
    val berryGainedEvent = _berryGainedEvent.asStateFlow()

    init {
        fetchDashboardData()
        observePendingEvents() // 앱 전역 이벤트 관찰 시작
    }

    private val prefs = application.getSharedPreferences("learn_prefs", Context.MODE_PRIVATE)

    // 이미 팝업 본 적 있는지 여부
    fun hasSeenInfoPopup(): Boolean {
        return prefs.getBoolean("has_seen_info_popup", false)
    }

    fun markInfoPopupSeen() {
        prefs.edit().putBoolean("has_seen_info_popup", true).apply()
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
    /**
     * UserProgressEventHolder를 구독
     */
    private fun observePendingEvents() {
        // 1. 레벨업 이벤트 구독
        eventHolder.pendingLevelUp
            .onEach { stage ->
                if (stage != null) {
                    _levelUpEvent.value = stage
                }
            }
            .launchIn(viewModelScope)

        // 2. 누베리 획득 이벤트 구독
        eventHolder.pendingBerryGained
            .onEach { gained ->
                if (gained) {
                    _berryGainedEvent.value = true
                }
            }
            .launchIn(viewModelScope)
    }
    /**
     * LearnScreen에서 레벨업 애니메이션이 끝난 후 호출할 함수
     */
    fun onLevelUpAnimationFinished() {
        // 1. UI 상태를 null로 되돌림 (애니메이션 중복 방지)
        _levelUpEvent.value = null
        // 2. 전역 홀더의 이벤트를 '소비' 처리 (중복 방지)
        eventHolder.consumeLevelUpEvent()

    }

    /**
     * LearnScreen에서 누베리 획득 토스트를 보여준 후 호출할 함수
     */
    fun onBerryAnimationFinished() {
        // 1. UI 상태를 false로 되돌림
        _berryGainedEvent.value = false
        // 2. 전역 홀더의 이벤트를 '소비' 처리
        eventHolder.consumeBerryGainedEvent()

    }
}
