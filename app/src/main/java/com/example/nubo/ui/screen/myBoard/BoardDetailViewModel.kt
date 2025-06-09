package com.example.nubo.ui.screen.myBoard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.network.BoardService
import com.example.nubo.data.network.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val boardService: BoardService
) : ViewModel() {
    private val _board = MutableStateFlow<BoardResponse?>(null)
    val board: StateFlow<BoardResponse?> = _board

    fun fetchBoardDetail(boardId: String) {
        viewModelScope.launch {
            try {
                val boardResponse = boardService.getBoardDetail(boardId)
                _board.value = boardResponse
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
