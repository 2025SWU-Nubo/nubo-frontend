package com.example.nubo.ui.screen.profile

import androidx.lifecycle.ViewModel
import com.example.nubo.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // 모든 인증 관련 데이터(access, userId, userInfo) 삭제
    fun logoutClearAll() {
        authRepository.clearAuthData()
    }

    // access/userId만 삭제
    fun logoutBasic() {
        authRepository.logout()
    }
}
