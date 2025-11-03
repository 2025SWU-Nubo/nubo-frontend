package com.example.nubo.ui.screen.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.repository.AuthEvent
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.NotificationRepository
import com.example.nubo.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,      // 로컬 정리 등에 사용
    private val profileRepository: ProfileRepository,
    
) : ViewModel() {

    // UI에서 사용할 상태
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _requireLogin = MutableLiveData<Unit>()
    val requireLogin: LiveData<Unit> get() = _requireLogin

    private val _withdrawSuccess = MutableLiveData<Unit>()
    val withdrawSuccess: LiveData<Unit> get() = _withdrawSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        // 레포가 발행하는 인증 이벤트 구독 → UI로 "로그인 필요" 신호만 전달
        viewModelScope.launch {
            profileRepository.authEvents.collectLatest { evt ->
                when (evt) {
                    is AuthEvent.LoginRequired -> _requireLogin.postValue(Unit)
                }
            }
        }
    }

    /**
     * 로그아웃 오케스트레이션
     * 1) 서버에 등록된 '현재 기기 FCM 토큰 매핑' 삭제 (best-effort, 실패해도 앱은 계속)
     * 2) 로컬 인증 정보(Access 토큰, 유저 ID/정보) 삭제
     * 3) latest_fcm_token 캐시는 유지하여 이후 재로그인 시 재등록 경로로 활용
     */
    fun logout() {
        viewModelScope.launch {
            // 로컬 인증 정보만 삭제 (FCM 매핑 삭제가 필요하면 ProfileRepository가 아닌 별도 Repo에서 수행)
            authRepository.clearAuthData()
        }
    }

    /**
     * 회원 탈퇴 오케스트레이션(클라이언트 측 단계)
     * 1) 현재 기기 FCM 토큰 매핑 삭제 (best-effort)
     * 2) 서버 탈퇴 API 호출 (콜백 인자로 실제 API를 주입)
     * 3) 로컬 인증 정보 삭제
     * 4) 후처리 콜백(onDone) 호출
     */
    fun withdraw() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            profileRepository.withdraw()
                .onSuccess {
                    _isLoading.value = false
                    _withdrawSuccess.value = Unit
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "회원 탈퇴에 실패했어요."
                }
        }
    }
}
