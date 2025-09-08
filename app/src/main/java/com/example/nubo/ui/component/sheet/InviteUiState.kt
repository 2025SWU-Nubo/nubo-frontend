package com.example.nubo.ui.component.sheet

import com.example.nubo.domain.model.InviteUser

sealed interface InviteUiState {
    data object Idle : InviteUiState
    data object Loading : InviteUiState
    data class Success(val users: List<InviteUser>) : InviteUiState
    data class Error(val message: String) : InviteUiState
}
