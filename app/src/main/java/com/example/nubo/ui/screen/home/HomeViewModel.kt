package com.example.nubo.ui.screen.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.BoardRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.model.home.RecommendChipItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- Cards ---
    private val _cards = MutableLiveData<List<CardResponse>>()
    val cards: LiveData<List<CardResponse>> get() = _cards

    // --- Card Detail ---
    private val _cardDetail = MutableLiveData<CardDetailResponse?>()
    val cardDetail: LiveData<CardDetailResponse?> = _cardDetail

    private val _isLoading = MutableLiveData(false)
    private val _isDetailLoading = MutableLiveData(false)
    val isDetailLoading: LiveData<Boolean> = _isDetailLoading


    // --- Boards / Chips ---
    private val _boards = MutableLiveData<List<BoardResponse>>(emptyList())
    val boards: LiveData<List<BoardResponse>> = _boards

    private val _chips = MutableLiveData<List<RecommendChipItem>>(emptyList())
    val chips: LiveData<List<RecommendChipItem>> = _chips

    private val _selectedChipId = MutableLiveData("all")
    val selectedChipId: LiveData<String> = _selectedChipId


    init {
        // Try once with whatever token exists
        refreshAll()

        // Retry a few times in case token arrives slightly later (no Flow dependency)
        viewModelScope.launch {
            repeat(10) {
                if (!authRepository.getAccessToken().isNullOrBlank()) {
                    refreshAll()
                    return@launch
                }
                delay(300)
            }
        }
    }

    fun refreshAll() {
        loadBoards()
        loadCards()
    }

    fun loadCards() {
        _isLoading.value = true

        authRepository.getAccessToken()?.let { token ->
            cardRepository.getCards("Bearer $token", "latest", null, null)
                .enqueue(object : Callback<List<CardResponse>> {
                    override fun onResponse(
                        call: Call<List<CardResponse>>,
                        response: Response<List<CardResponse>>
                    ) {
                        _isLoading.value = false
                        if (response.isSuccessful) {
                            _cards.value = response.body() ?: emptyList()
                        } else {
                            _cards.value = emptyList()
                        }
                    }

                    override fun onFailure(call: Call<List<CardResponse>>, t: Throwable) {
                        _isLoading.value = false
                        _cards.value = emptyList()
                    }
                })
        } ?: run {
            _isLoading.value = false
            _cards.value = emptyList()
        }
    }

    fun loadBoards(){
        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: run {
                Log.d("HomeViewModel", "❌ Token is null, cannot load boards")
                return@launch}
            boardRepository.getMyBoards(token)
                .onSuccess { list ->
                    _boards.value = list
                    Log.d("HomeViewModel", "✅ Boards loaded: size=${list.size}, data=$list")
                    val built = buildList {
                        add(RecommendChipItem(id = "all", title = "전체", isSelected = false))
                        list.forEach { b ->
                            add(RecommendChipItem(id = b.id.toString(), title = b.name, isSelected = false ))
                        }
                    }

                    val current = _selectedChipId.value ?: "all"

                    val adjusted = if (current == "all" || built.any { it.id == current }) {
                        built.map { it.copy(isSelected = it.id == current) }
                    } else {
                        _selectedChipId.value = "all"
                        built.map { it.copy(isSelected = it.id == "all") }
                    }
                    _chips.value = adjusted

                }
                .onFailure { e ->
                    _boards.value = emptyList()
                    _chips.value = listOf(RecommendChipItem("all", "전체", isSelected = true))
                    _selectedChipId.value = "all"
                    Log.e("HomeViewModel", "❌ Failed to load boards: ${e.localizedMessage}", e)
                }
        }
    }

    fun onChipClick(chip: RecommendChipItem) {
        _selectedChipId.value = chip.id
        // Update selection flags in chips
        _chips.value = _chips.value.orEmpty().map { it.copy(isSelected = it.id == chip.id) }
        Log.d("HomeViewModel", "🔘 Chip clicked: id=${chip.id}, title=${chip.title}")

        val token = authRepository.getAccessToken() ?: return

        if(chip.id == "all"){loadCards()}
        else{
            val boardId = chip.id.toLongOrNull() ?: return
            _isLoading.value = true
            cardRepository.getUnviewedCardsByBoard(token,boardId)
                .enqueue(object : Callback<List<CardResponse>>{override fun onResponse(
                    call: Call<List<CardResponse>>,
                    response: Response<List<CardResponse>>
                ) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        _cards.value = response.body().orEmpty()
                        Log.d("HomeViewModel", "✅ Board $boardId cards loaded: size=${_cards.value?.size}")
                    } else {
                        _cards.value = emptyList()
                        Log.e("HomeViewModel", "❌ Failed to load board $boardId cards: code=${response.code()}")
                    }
                }

                    override fun onFailure(call: Call<List<CardResponse>>, t: Throwable) {
                        _isLoading.value = false
                        _cards.value = emptyList()
                        Log.e("HomeViewModel", "❌ Board $boardId cards request failed: ${t.localizedMessage}", t)
                    }
                })
        }
    }


    fun getCardDetail(cardId: Int) {
        _isDetailLoading.value = true

        authRepository.getAccessToken()?.let { token ->
            cardRepository.getCardDetail(token, cardId)
                .enqueue(object : Callback<CardDetailResponse> {
                    override fun onResponse(
                        call: Call<CardDetailResponse>,
                        response: Response<CardDetailResponse>
                    ) {
                        _isDetailLoading.value = false
                        if (response.isSuccessful) {
                            val cardDetailResponse = response.body()
                            Log.d("HomeViewModel", "Card detail fetched successfully: $cardDetailResponse")
                            _cardDetail.value = response.body()

                        } else {
                            _cardDetail.value = null
                        }
                    }

                    override fun onFailure(call: Call<CardDetailResponse>, t: Throwable) {
                        _isDetailLoading.value = false
                        _cardDetail.value = null
                    }
                })
        }
    }

    fun clearCardDetail() {
        _cardDetail.value = null
    }
}

