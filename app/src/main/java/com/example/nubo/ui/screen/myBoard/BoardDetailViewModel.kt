package com.example.nubo.ui.screen.myBoard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.network.BoardService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.nubo.data.repository.AuthRepository

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val boardService: BoardService,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _board = MutableStateFlow<BoardResponse?>(null)
    val board: StateFlow<BoardResponse?> = _board


    fun fetchBoardDetail(boardId: Int) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val boardResponse = boardService.getBoardDetail(token, boardId = boardId)
                _board.value = boardResponse
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

