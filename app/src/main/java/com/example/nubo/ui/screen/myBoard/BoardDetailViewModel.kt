package com.example.nubo.ui.screen.myBoard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.BoardRepository

data class BoardDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val board: BoardResponse? = null,
    val favoriteOnly: Boolean = false,
    val page: Int = 0,
    val isLast: Boolean = false
)

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(BoardDetailUiState())
    val ui: StateFlow<BoardDetailUiState> = _ui

    private var currentBoardId: Int = -1
    private var bootstrapped = false
    private val pageSize = 20

    fun init(boardId: Int) {
        if (bootstrapped && currentBoardId == boardId) return
        bootstrapped = true
        currentBoardId = boardId
        _ui.value = BoardDetailUiState(isLoading = true, favoriteOnly = false)
        loadPage(reset = true)
    }

    fun setFavoriteFilter(enabled: Boolean) {
        if (_ui.value.favoriteOnly == enabled) return
        _ui.value = _ui.value.copy(favoriteOnly = enabled)
        loadPage(reset = true)
    }

    fun loadNextPage() {
        val s = _ui.value
        if (s.isLoading || s.isLast) return
        loadPage(reset = false)
    }

    private fun loadPage(reset: Boolean) {
        val s = _ui.value
        val targetPage = if (reset) 0 else s.page + 1
        _ui.value = s.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val token = authRepository.getAccessToken().orEmpty()
            boardRepository.getBoardDetail(
                token = token,
                boardId = currentBoardId,
                favoriteOnly = _ui.value.favoriteOnly,
                page = targetPage,
                size = pageSize
            ).onSuccess { resp ->
                val merged = if (reset || s.board == null) {
                    resp
                } else {
                    val prev = s.board!!
                    val newContent = prev.cards.content + resp.cards.content
                    resp.copy(cards = resp.cards.copy(content = newContent))
                }
                _ui.value = s.copy(
                    isLoading = false,
                    board = merged,
                    page = resp.cards.number,
                    isLast = resp.cards.last
                )
            }.onFailure { e ->
                _ui.value = s.copy(
                    isLoading = false,
                    error = e.message ?: "불러오기에 실패했습니다."
                )
            }
        }
    }
}

