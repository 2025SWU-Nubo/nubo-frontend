package com.example.nubo.data.model

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.network.RetrofitClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import com.example.nubo.model.myBoard.MyCardItem

class CardViewModel : ViewModel() {
    private val _cards = mutableStateOf<List<MyCardItem>>(emptyList())
    val cards: State<List<MyCardItem>> = _cards

    init {
        fetchCards()
    }

    private fun fetchCards() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.cardService.getMyCards()
                _cards.value = response.map { dto ->
                    MyCardItem(
                        id = dto.id,
                        imageUrl = dto.imageUrl
                    )
                }
            } catch (e: Exception) {
                Log.e("CardViewModel", "Error fetching cards", e)
            }
        }
    }
}
