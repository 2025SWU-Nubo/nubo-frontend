package com.example.nubo.ui.screen.myBoard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BoardDetailViewModel : ViewModel() {
    private val _board = MutableStateFlow<BoardResponse?>(null)
    val board: StateFlow<BoardResponse?> = _board

    fun fetchBoardDetail(boardId: String) {
        viewModelScope.launch {
            try {
                val boardResponse = RetrofitClient.boardService.getBoardDetail(boardId)
                _board.value = boardResponse
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
