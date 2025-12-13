package com.example.nubo.ui.screen.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.GroupDto
import com.example.nubo.data.model.PagedResponse
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.network.CardSort
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.BoardRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.data.repository.NotificationRepository
import com.example.nubo.domain.model.CardFilter
import com.example.nubo.model.home.RecommendChipItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.LongArraySerializer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val boardRepository: BoardRepository,
    private val notiRepository: NotificationRepository,
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

    private val _selectedChipIds = MutableLiveData<Set<String>>(setOf("all"))
    val selectedChipIds: LiveData<Set<String>> = _selectedChipIds

    // --- notifications ---
    val hasUnread: StateFlow<Boolean> = notiRepository.hasUnread


    // 추천 카드 그룹 상태
    private val _recommendGroups = MutableLiveData<List<GroupDto>>()
    val recommendGroups: LiveData<List<GroupDto>> = _recommendGroups

    // 추천 카드 그룹 불러오기
    fun loadRecommendationGroups() {

        val token = authRepository.getAccessToken() ?: return
        viewModelScope.launch {
            cardRepository.getRecommendCards()
                .onSuccess { response ->
                    _recommendGroups.value = response.groups
                }
                .onFailure {
                    _recommendGroups.value = emptyList()
                }
        }
    }



    init {
        // Try once with whatever token exists
        refreshAll()

        viewModelScope.launch {
            repeat(10) {
                val token = authRepository.getAccessToken()
                if (!token.isNullOrBlank()) {
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
        loadUnreadFlag()
    }

    fun refreshForCurrentSelection() {
        val token = authRepository.getAccessToken() ?: return
        val selected = _selectedChipIds.value ?: setOf("all")

        if ("all" in selected) {
            _isLoading.value = true

            cardPage = 0
            cardPageSize = 20
            cardIsLast = true

            cardRepository.getUnviewedAllCards(limit = 20)
                .enqueue(object : Callback<List<CardResponse>> {
                    override fun onResponse(
                        call: Call<List<CardResponse>>,
                        response: Response<List<CardResponse>>
                    ) {
                        _isLoading.value = false
                        if (response.isSuccessful) {
                            val list = response.body().orEmpty()
                            _cards.value = list
                            Log.d("HomeViewModel", "✅ ALL: loaded size=${list.size}")
                        } else {
                            _cards.value = emptyList()
                            Log.e("HomeViewModel", "❌ ALL: failed code=${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<List<CardResponse>>, t: Throwable) {
                        _isLoading.value = false
                        _cards.value = emptyList()
                        Log.e("HomeViewModel", "❌ ALL: request failed: ${t.localizedMessage}", t)
                    }
                })

            return
        }

        val boardIds = selected.mapNotNull { it.toLongOrNull() }.distinct()
        if (boardIds.isEmpty()) {
            _selectedChipIds.value = setOf("all")
            _chips.value = _chips.value.orEmpty().map { it.copy(isSelected = it.id == "all") }
            refreshForCurrentSelection()
            return
        }

        _isLoading.value = true

        cardRepository.getUnviewedCardsByBoards(boardIds = boardIds, limit = 15)
            .enqueue(object : Callback<List<CardResponse>> {
                override fun onResponse(
                    call: Call<List<CardResponse>>,
                    response: Response<List<CardResponse>>
                ) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        val list = response.body().orEmpty()
                        _cards.value = list
                        cardPage = 0
                        cardIsLast = true
                        Log.d("HomeViewModel", "✅ BOARDS $boardIds loaded size=${list.size}")
                    } else {
                        _cards.value = emptyList()
                        Log.e("HomeViewModel", "❌ BOARDS $boardIds failed code=${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<CardResponse>>, t: Throwable) {
                    _isLoading.value = false
                    _cards.value = emptyList()
                    Log.e("HomeViewModel", "❌ BOARDS request failed: ${t.localizedMessage}", t)
                }
            })
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

    fun loadRecentBoards(){
        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: run {
                Log.d("HomeViewModel", "❌ Token is null, cannot load boards")
                return@launch
            }
            boardRepository.getRecentBoards()
                .onSuccess { list ->
                    _recentBoards.value = list
                    Log.d("HomeViewModel", "✅ Recent Boards loaded: size=${list.size}, data=$list")
                }.onFailure { e ->
                    _recentBoards.value = emptyList()
                    Log.e("HomeViewModel", "❌ Failed to load recent boards: ${e.localizedMessage}", e)
                }
        }
    }

    fun loadBoards() {
        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: run {
                Log.d("HomeViewModel", "❌ Token is null, cannot load boards")
                return@launch
            }

            boardRepository.getHomeBoards(sort = "LATEST")
                .onSuccess { list ->
                    val built = buildList {
                        add(RecommendChipItem(id = "all", title = "전체", isSelected = false))
                        list.forEach { b ->
                            add(
                                RecommendChipItem(
                                    id = b.boardId.toString(),
                                    title = b.boardName,
                                    isSelected = false
                                )
                            )
                        }
                    }

                    val selected = _selectedChipIds.value ?: setOf("all")

                    // If "all" is selected, enforce single selection
                    val normalized = if ("all" in selected || selected.isEmpty()) {
                        setOf("all")
                    } else {
                        // Keep only ids that exist in the built list
                        selected.filter { id -> built.any { it.id == id } }.toSet().ifEmpty { setOf("all") }
                    }

                    _selectedChipIds.value = normalized
                    _chips.value = built.map { it.copy(isSelected = it.id in normalized) }

                    Log.d("HomeViewModel", "✅ Chips loaded size=${list.size}")
                }
                .onFailure { e ->
                    _chips.value = listOf(RecommendChipItem("all", "전체", isSelected = true))
                    _selectedChipIds.value = setOf("all")
                    Log.e("HomeViewModel", "❌ Failed to load home boards: ${e.localizedMessage}", e)
                }
        }
    }


    fun onChipClick(chip: RecommendChipItem) {
        val current = _selectedChipIds.value ?: setOf("all")
        val next = current.toMutableSet()

        if (chip.id == "all") {
            // Selecting "all" clears everything else
            next.clear()
            next.add("all")
        } else {
            // Selecting any board clears "all"
            next.remove("all")

            // Toggle behavior
            if (next.contains(chip.id)) next.remove(chip.id) else next.add(chip.id)

            // If nothing selected, fallback to "all"
            if (next.isEmpty()) next.add("all")
        }

        _selectedChipIds.value = next.toSet()
        _chips.value = _chips.value.orEmpty().map { it.copy(isSelected = it.id in next) }

        Log.d("HomeViewModel", "Chip toggled: id=${chip.id}, selected=$next")

        refreshForCurrentSelection()
    }


    fun getCardDetail(cardId: Int) {
        _isDetailLoading.value = true

        authRepository.getAccessToken()?.let { token ->
            cardRepository.getCardDetail( cardId)
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

    fun loadUnreadFlag() {
        val token = authRepository.getAccessToken() ?: return
        viewModelScope.launch {
            notiRepository.refreshUnreadFlag(token)
        }
    }
}

