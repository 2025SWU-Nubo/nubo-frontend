package com.example.nubo.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.NotificationSetRequest
import com.example.nubo.data.model.ProfileResponse
import com.example.nubo.data.network.ProfileService
import com.example.nubo.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

// 1) 이벤트 타입 정의: ViewModel은 Compose 타입에 의존하지 않도록 String + Kind만 전달
sealed interface ProfileEvent {
    data class ShowToast(
        val message: String,
        val kind: ToastKind = ToastKind.NORMAL,
        val durationMillis: Int = 1600
    ) : ProfileEvent
}

enum class ToastKind { NORMAL, POSITIVE, NEGATIVE }

data class ProfileUiState(
    val isLoading: Boolean = false,
    val data: ProfileResponse? = null,
    val error: String? = null,
    // ── 알림 설정 UI 상태 (서버 초기값이 있다면 refresh() 완료 시 채움)
    val pushEnabled: Boolean = true,
    val remindEnabled: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events

    init {
        // 로컬 캐시에서 마지막 알림 상태 복원
        val (push, remind) = profileRepository.readNotificationPrefs()
        _uiState.value = _uiState.value.copy(
            pushEnabled = push,
            remindEnabled = if (push) remind else false
        )
        refresh()
    }

    // ─────────────────────────────────────────────────────────────
    // 프로필 확인
    // 알림 토글 값은 유지하고, 프로필 데이터/로딩/에러만 갱신하는 버전
    fun refresh() {
        // 로딩 시작: 기존 토글 상태 보존
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            profileRepository.fetchProfile()
                .onSuccess { resp ->
                    // 성공: 프로필 데이터만 갱신, 토글 값 보존
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        data = resp,
                        error = null
                    )
                }
                .onFailure { t ->
                    // 실패: 에러만 갱신
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = t.message
                    )
                }
        }
    }

    // ─────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────
    // 전체 알림 토글
    fun togglePush(checked: Boolean, onError: (String) -> Unit = {}) {
        val prev = _uiState.value
        val nextRemind = if (!checked) false else prev.remindEnabled
        _uiState.value = prev.copy(pushEnabled = checked, remindEnabled = nextRemind)

        viewModelScope.launch {
            val req = NotificationSetRequest(
                pushEnabled = checked,
                remindEnabled = if (!checked) false else null
            )
            profileRepository.updateNotification(req)
                .onSuccess {

                    profileRepository.saveNotificationPrefs(checked, nextRemind)
                    _events.emit(
                        ProfileEvent.ShowToast(
                            message = if (checked) "전체 알림 켰어요." else "전체 알림 껐어요",
                            kind = ToastKind.NORMAL
                        )
                    )
                }
                .onFailure {
                    _uiState.value = prev
                    val msg = it.message ?: "전체 알림 설정 저장에 실패했습니다"

                    _events.emit(
                        ProfileEvent.ShowToast(
                            message = msg,
                            kind = ToastKind.NEGATIVE,
                            durationMillis = 2000
                        )
                    )
                }
        }
    }

    // 리마인드 토글
    fun toggleRemind(checked: Boolean, onError: (String) -> Unit = {}) {
        if (!_uiState.value.pushEnabled) return

        val prev = _uiState.value
        _uiState.value = prev.copy(remindEnabled = checked)

        viewModelScope.launch {
            val req = NotificationSetRequest(remindEnabled = checked)
            profileRepository.updateNotification(req)
                .onSuccess {
                    profileRepository.saveNotificationPrefs(_uiState.value.pushEnabled, checked)
                    _events.emit(
                        ProfileEvent.ShowToast(
                            message = if (checked) "리마인드 알림을 켰어요." else "리마인드 알림을 껐어요.",
                            kind = ToastKind.NORMAL
                        )
                    )
                }
                .onFailure {
                    _uiState.value = prev
                    val msg = it.message ?: "리마인드 설정 저장에 실패했습니다"

                    _events.emit(
                        ProfileEvent.ShowToast(
                            message = msg,
                            kind = ToastKind.NEGATIVE,
                            durationMillis = 2000
                        )
                    )
                }
        }
    }
}
