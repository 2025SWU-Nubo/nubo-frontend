package com.example.nubo.ui.component.sheet

sealed interface InviteUiState {
    data object Idle : InviteUiState
    data object Loading : InviteUiState
    data class Success(val users: List<InviteUserItem>) : InviteUiState
    data class Error(val message: String) : InviteUiState
}
