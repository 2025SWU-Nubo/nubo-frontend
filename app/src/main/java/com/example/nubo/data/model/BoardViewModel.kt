package com.example.nubo.data.model

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.network.RetrofitClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import com.example.nubo.model.myBoard.BoardItem
import getDisplayDate


class BoardViewModel : ViewModel() {
    private val _boards = mutableStateOf<List<BoardItem>>(emptyList())
    val boards: State<List<BoardItem>> = _boards

    init {
        fetchBoards()
    }

    private fun fetchBoards() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.boardService.getMyBoards()
                _boards.value = response.mapIndexed { index, dto ->
                    BoardItem(
                        id = index,  // 앱 내부 ID
                        serverBoardId = dto.id,  // 서버 ID
                        title = dto.name,
                        subtitle = "${dto.sectionCount}섹션 ${dto.cardCount}카드",
                        createdAt = getDisplayDate(dto.updatedAt),
                        source = dto.source
                    )
                }
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Error fetching boards", e)
            }
        }
    }
}
