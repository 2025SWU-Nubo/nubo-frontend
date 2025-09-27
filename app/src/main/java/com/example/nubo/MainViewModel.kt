package com.example.nubo

import androidx.lifecycle.ViewModel
import com.example.nubo.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    // 로그인 여부를 Compose에서 관찰
    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> get() = _isLoggedIn

    /** 필요 시 로그인 상태 재평가 (ex. 로그아웃/로그인 후 갱신) */
    fun refreshLoginState() {
        _isLoggedIn.value = authRepository.isLoggedIn()
    }
}
