package com.example.nubo.ui.screen.myBoard

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import com.example.nubo.data.network.BoardService
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.model.myBoard.BoardItem
import dagger.hilt.android.lifecycle.HiltViewModel
import getDisplayDate
import javax.inject.Inject


@HiltViewModel
class BoardViewModel @Inject constructor(
    private val boardService: BoardService,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _boards = mutableStateOf<List<BoardItem>>(emptyList())
    val boards: State<List<BoardItem>> = _boards

    init {
        fetchBoards()
    }

    private fun fetchBoards() {
        viewModelScope.launch {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val response = boardService.getMyBoards(token)
                _boards.value = response.mapIndexed { index, dto ->
                    BoardItem(
                        id = index,
                        serverBoardId = dto.id,
                        title = dto.name,
                        subtitle = "${dto.sectionCount} 섹션 ${dto.cardCount} 카드",
                        createdAt = getDisplayDate(dto.updatedAt),
                        source = dto.source,
                        imageUrl = dto.thumbnailUrl
                    )
                }
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Error fetching boards", e)
            }
        }
    }
}
