package com.example.nubo.ui.component.sheet

import com.example.nubo.data.model.BoardItemResponse

data class CreateBoardUiState(
    val name: String = "",
    val isShared: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val created: BoardItemResponse? = null,
    val invitedEmails: List<String> = emptyList()
)
