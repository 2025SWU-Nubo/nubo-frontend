package com.example.nubo.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BoardDetailViewModel : ViewModel() {
    private val _board = MutableStateFlow<BoardResponse?>(null)
    val board: StateFlow<BoardResponse?> = _board

    private val _cards = MutableStateFlow<List<CardItemDto>>(emptyList())
    val cards: StateFlow<List<CardItemDto>> = _cards

    fun fetchBoardDetail(boardId: String) {
        viewModelScope.launch {
            try {
                val boardResponse = RetrofitClient.boardService.getBoardDetail(boardId)
                _board.value = boardResponse

                val cardResponse = RetrofitClient.cardService.getBoardCards(boardId)
                _cards.value = cardResponse
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
