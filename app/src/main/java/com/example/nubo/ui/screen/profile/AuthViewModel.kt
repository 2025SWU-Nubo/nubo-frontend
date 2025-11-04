package com.example.nubo.ui.screen.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.repository.AuthEvent
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.NotificationRepository
import com.example.nubo.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,      // 로컬 정리 등에 사용
    private val profileRepository: ProfileRepository,
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context,
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
            // 1. 저장된 최신 FCM 토큰 가져오기
            val token = getLatestFcmToken()
            if (!token.isNullOrBlank()) {
                runCatching { notificationRepository.deleteDeviceToken(token) }
                    .onSuccess { android.util.Log.d("LOGOUT", "서버 토큰 삭제 성공") }
                    .onFailure { android.util.Log.w("LOGOUT", "서버 토큰 삭제 실패: ${it.message}") }
            } else {
                android.util.Log.w("LOGOUT", "삭제할 FCM 토큰이 없음")
            }

            // 2. 로컬 인증정보 초기화
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
            // 1. 서버의 토큰 매핑 삭제
            val token = getLatestFcmToken()
            if (!token.isNullOrBlank()) {
                runCatching { notificationRepository.deleteDeviceToken(token) }
                    .onSuccess { android.util.Log.d("WITHDRAW", "서버 토큰 삭제 성공") }
                    .onFailure { android.util.Log.w("WITHDRAW", "서버 토큰 삭제 실패: ${it.message}") }
            }

            // 2. 회원 탈퇴 API 요청
            profileRepository.withdraw()
                .onSuccess {
                    // 3. 로컬 인증 정보 삭제
                    authRepository.clearAuthData()

                    _isLoading.value = false
                    _withdrawSuccess.value = Unit
                    android.util.Log.d("WITHDRAW", "회원 탈퇴 성공 및 로컬 초기화 완료")
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "회원 탈퇴에 실패했어요."
                    android.util.Log.e("WITHDRAW", "회원 탈퇴 실패: ${e.message}")
                }
        }
    }

    /**
     * SharedPreferences에 저장된 최신 FCM 토큰을 가져오는 헬퍼 함수
     * - logout / withdraw 시 서버 매핑 삭제용
     */
    private fun getLatestFcmToken(): String? {
        val prefs = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
        return prefs.getString("latest_fcm_token", null)
    }
}
