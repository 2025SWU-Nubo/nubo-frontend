package com.example.nubo.ui.screen.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.PagedResponse
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.network.CardSort
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.BoardRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.domain.model.CardFilter
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
    private val _cards = MutableLiveData<List<CardResponse>>(emptyList())
    val cards: LiveData<List<CardResponse>> get() = _cards

    // --- Card Detail ---

    // Paging state for "전체 카드"
    private var cardPage: Int = 0                 // current page index
    private var cardPageSize: Int = 20            // page size
    private var cardIsLast: Boolean = false

    private val _cardDetail = MutableLiveData<CardDetailResponse?>()
    val cardDetail: LiveData<CardDetailResponse?> = _cardDetail

    private val _isLoading = MutableLiveData(false)
    private val _isDetailLoading = MutableLiveData(false)
    val isDetailLoading: LiveData<Boolean> = _isDetailLoading


    // --- Boards / Chips ---
    private val _boards = MutableLiveData<List<com.example.nubo.domain.model.BoardSummary>>(emptyList())
    val boards: LiveData<List<com.example.nubo.domain.model.BoardSummary>> = _boards


    private val _recentBoards = MutableLiveData<List<RecentBoardResponse>>(emptyList())
    val recentBoards: LiveData<List<RecentBoardResponse>> = _recentBoards

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
        loadRecentBoards()
        refreshForCurrentSelection()
    }

    /** 현재 선택된 칩("all" or boardId)에 맞춰 카드만 새로고침 */
//    fun refreshForCurrentSelection(){
//        val token = authRepository.getAccessToken() ?: return
//        val sel = _selectedChipId.value ?: "all"
//
//        if(sel == "all"){
//            loadCards()
//        }else{
//            val boardId = sel.toLongOrNull() ?: return
//            _isLoading.value = true
//
//            //미지청 추천 영상 조회
//            cardRepository.getUnviewedCardsByBoard(token,boardId)
//                .enqueue(object : Callback<List<CardResponse>>{
//                    override fun onResponse(call: Call<List<CardResponse>?>, response: Response<List<CardResponse>?>) {
//                        _isLoading.value = false
//                        if (response.isSuccessful) {
//                            _cards.value = response.body().orEmpty()
//                            Log.d("HomeViewModel", "✅ Board $boardId cards loaded: size=${_cards.value?.size}")
//                        } else {
//                            _cards.value = emptyList()
//                            Log.e("HomeViewModel", "❌ Failed to load board $boardId cards: code=${response.code()}")
//                        }
//                    }
//
//                    override fun onFailure(call: Call<List<CardResponse>?>, t: Throwable) {
//                        _isLoading.value = false
//                        _cards.value = emptyList()
//                        Log.e("HomeViewModel", "❌ Board $boardId cards request failed: ${t.localizedMessage}", t)
//                    }
//                })
//        }
//    }

    fun refreshForCurrentSelection(){
        val token = authRepository.getAccessToken() ?: return
        val sel = _selectedChipId.value ?: "all"

        if(sel == "all"){
            cardPage = 0
            cardPageSize = 20
            cardIsLast = true

            cardRepository.getUnviewedAllCards(token, limit = 20)
                .enqueue(object : Callback<List<CardResponse>> {
                    override fun onResponse(
                        call: Call<List<CardResponse>>,
                        response: Response<List<CardResponse>>
                    ) {
                        _isLoading.value = false
                        if (response.isSuccessful) {
                            val list = response.body().orEmpty()
                            _cards.value = list
                            Log.d("HomeViewModel", "✅ ALL chip: unviewed-all loaded size=${list.size}")
                        } else {
                            _cards.value = emptyList()
                            Log.e("HomeViewModel", "❌ ALL chip: failed code=${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<List<CardResponse>>, t: Throwable) {
                        _isLoading.value = false
                        _cards.value = emptyList()
                        Log.e("HomeViewModel", "❌ ALL chip: request failed: ${t.localizedMessage}", t)
                    }
                })
            return
        }else{
            val boardId = sel.toLongOrNull() ?: return
            _isLoading.value = true

            // 미시청 카드 조회
            cardRepository.getUnviewedCardsByBoard(token,boardId = boardId, limit = 10)
                .enqueue(object : Callback<List<CardResponse>> {
                    override fun onResponse(call: Call<List<CardResponse>?>, response: Response<List<CardResponse>?>) {
                        _isLoading.value = false
                        if (response.isSuccessful) {
                            val list = response.body().orEmpty()
                            if (list.isEmpty()) {
                                Log.d("HomeViewModel", "❌ No unviewed cards for board $boardId")
                            }
                            _cards.value = list
                            // 보드 칩 모드에서는 전체 페이징과 무관하게 고정
                            cardPage = 0
                            cardIsLast = true
                            Log.d("HomeViewModel", "✅ Board $boardId cards loaded: size=${list.size}")
                        } else {
                            _cards.value = emptyList()
                            Log.e("HomeViewModel", "❌ Failed to load board $boardId cards: code=${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<List<CardResponse>?>, t: Throwable) {
                        _isLoading.value = false
                        _cards.value = emptyList()
                        Log.e("HomeViewModel", "❌ Board $boardId cards request failed: ${t.localizedMessage}", t)
                    }
                })
        }
    }


    fun loadCards(reset: Boolean = false) {
        val token = authRepository.getAccessToken() ?: run {
            _isLoading.value = false
            _cards.value = emptyList()
            cardIsLast = true
            return
        }

        if (!reset && cardIsLast) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val targetPage = if (reset) 0 else cardPage + 1
                val result = cardRepository.getCards(
                    token = token,
                    sort = CardSort.LATEST,
                    filter = CardFilter.ALL,
                    page = targetPage,
                    size = cardPageSize
                )

                val page: PagedResponse<CardResponse> = result.getOrThrow()

                val newList = if (reset) page.content else _cards.value.orEmpty() + page.content
                _cards.value = newList

                cardPage = page.number
                cardPageSize = page.size
                cardIsLast = page.last
            } catch (e: Exception) {
                if (reset) _cards.value = emptyList()
                Log.e("HomeViewModel", "❌ Failed to load cards: ${e.localizedMessage}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreCardsIfPossible() {
        // only works for "전체" chip
        if (_selectedChipId.value != "all") return
        loadCards(reset = false)
    }

    fun loadRecentBoards(){
        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: run {
                Log.d("HomeViewModel", "❌ Token is null, cannot load boards")
                return@launch
            }
            boardRepository.getRecentBoards(token)
                .onSuccess { list ->
                    _recentBoards.value = list
                    Log.d("HomeViewModel", "✅ Recent Boards loaded: size=${list.size}, data=$list")
                }.onFailure { e ->
                    _recentBoards.value = emptyList()
                    Log.e("HomeViewModel", "❌ Failed to load recent boards: ${e.localizedMessage}", e)
                }
        }
    }

    fun loadBoards(){
        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: run {
                Log.d("HomeViewModel", "❌ Token is null, cannot load boards")
                return@launch}

            boardRepository.getMyBoards(
                token = token,
                sort = com.example.nubo.domain.model.BoardCardSort.LATEST,
                filter = com.example.nubo.domain.model.BoardCardFilter.ALL,
                page = 0,
                size = 20
            )

                .onSuccess { paged ->
                    val list = paged.items
                    _boards.value = list
                    Log.d("HomeViewModel", "✅ Boards loaded: size=${list.size}, data=$list")

                    val built = buildList {
                        add(RecommendChipItem(id = "all", title = "전체", isSelected = false))
                        list.forEach { b ->
                            add(RecommendChipItem(id = b.id.toString(), title = b.name, isSelected = false))
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
        Log.d("HomeViewModel", "Chip clicked: id=${chip.id}, title=${chip.title}")

        //새로고침
        refreshForCurrentSelection()

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

