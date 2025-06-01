package com.example.nubo.ui.screen.onBoardingLogin

import com.example.nubo.data.model.UserInfo

data class OnBoardingUiState(
    val isLoading: Boolean = false,
    val showLoginButton: Boolean = false,
    val existingUser: UserInfo? = null,
    val loginResponseUser: UserInfo? = null,
    val logoShrinked: Boolean = false
)
