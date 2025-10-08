package com.example.nubo.ui.screen.myBoard

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardSearchItemResponse
import com.example.nubo.data.network.BoardService
import com.example.nubo.data.network.CardSort
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.domain.model.CardFilter
import com.example.nubo.model.myBoard.MyCardItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

// 카드 정렬/필터 값
enum class CardSort { LATEST, OLDEST, ALPHABET }
enum class CardFilter { ALL, FAVORITE, SHARED }
// 현재 정렬/필터 상태
private var sort: CardSort = CardSort.LATEST   // 기본 최신순
private var filter: CardFilter = CardFilter.ALL

@HiltViewModel
class MyCardViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val authRepository: AuthRepository,
    private val boardService: BoardService
) : ViewModel() {

    private val _cards = mutableStateOf<List<MyCardItem>>(emptyList())
    val cards: State<List<MyCardItem>> get() = _cards

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> get() = _isLoading

    // 상세 카드 데이터 상태
    private val _cardDetail = mutableStateOf<CardDetailResponse?>(null)
    val cardDetail: State<CardDetailResponse?> get() = _cardDetail

    private val _isDetailLoading = mutableStateOf(false)
    val isDetailLoading: State<Boolean> get() = _isDetailLoading

    // --- Paging states for "my cards" ---
    private var page: Int = 0               // current page index
    private var size: Int = 20              // page size
    private var isLast: Boolean = false     // from server 'last' flag

    // 검색 결과 상태
    private val _searchResults = mutableStateOf<List<MyCardItem>>(emptyList())
    val searchResults: State<List<MyCardItem>> = _searchResults

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    init {
        refresh()
    }

    /** Refresh list from the first page */
    fun refresh() {
        loadCards(reset = true)
    }

    /** Load next page if available (for infinite scroll) */
    fun loadMore() {
        if (isLast || _isLoading.value) return
        loadCards(reset = false)
    }

    // 정렬 변경
    fun setSort(newSort: String) {
        val s = when (newSort) {
            "LATEST" -> CardSort.LATEST
            //"OLDEST" -> CardSort.OLDEST
            //"ALPHABET" -> CardSort.ALPHABET
            else -> sort
        }
        if (s != sort) {
            sort = s
            refresh()   // 재조회
        }
    }

    // 필터 변경
    fun setFilter(newFilter: String) {
        val f = when (newFilter) {
            "ALL" -> CardFilter.ALL
            "FAVORITE" -> CardFilter.FAVORITE
            "SHARED" -> CardFilter.SHARED
            else -> filter
        }
        if (f != filter) {
            filter = f
            refresh()   // 재조회
        }
    }

//    private fun fetchCards() {
//        _isLoading.value = true
//
//        authRepository.getAccessToken()?.let { token ->
//            cardRepository.getCards("Bearer $token", CardSort.LATEST, null, null)
//                .enqueue(object : Callback<List<CardResponse>> {
//                    override fun onResponse(
//                        call: Call<List<CardResponse>>,
//                        response: Response<List<CardResponse>>
//                    ) {
//                        _isLoading.value = false
//                        if (response.isSuccessful) {
//                            _cards.value = response.body()?.map {
//                                MyCardItem(
//                                    id = it.cardId,
//                                    imageUrl = it.videoThumbnailUrl
//                                )
//                            } ?: emptyList()
//                        } else {
//                            _cards.value = emptyList()
//                        }
//                    }
//
//                    override fun onFailure(call: Call<List<CardResponse>>, t: Throwable) {
//                        _isLoading.value = false
//                        _cards.value = emptyList()
//                    }
//                })
//        } ?: run {
//            _isLoading.value = false
//            _cards.value = emptyList()
//        }
//    }

    /**
     * Load cards with paging
     * @param reset true to reload from page 0, false to append next page
     */
    private fun loadCards(reset: Boolean) {
        val token = authRepository.getAccessToken() ?: run {
            // no token, clear states
            _cards.value = emptyList()
            _isLoading.value = false
            isLast = true
            return
        }

        // if appending and already last, do nothing
        if (!reset && isLast) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val targetPage = if (reset) 0 else page + 1

                // Repository uses suspend + Result<PagedResponse<CardResponse>>
                val pageRes = cardRepository.getCards(
                    token = token,
                    sort = sort,          // ← enum 그대로 넘김
                    filter = filter,
                    page = targetPage,
                    size = size
                ).getOrThrow()

                // map DTO -> UI item
                val mapped = pageRes.content.map {
                    MyCardItem(
                        id = it.cardId,
                        imageUrl = it.videoThumbnailUrl
                    )
                }

                _cards.value = if (reset) mapped else _cards.value + mapped

                // update paging flags
                page = pageRes.number
                size = pageRes.size
                isLast = pageRes.last
            } catch (e: Exception) {
                // on error, keep previous list; if reset, clear
                if (reset) _cards.value = emptyList()
            } finally {
                _isLoading.value = false
            }
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
        } ?: run {
            _isDetailLoading.value = false
            _cardDetail.value = null
        }
    }

    fun clearCardDetail() {
        _cardDetail.value = null
    }

    fun searchCards(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                // authRepository에서 토큰을 가져오는 방식이 getAccessToken()?.let {...} 이므로
                // 코루틴 내에서 직접 호출하여 사용
                val token = authRepository.getAccessToken() ?: run {
                    _searchResults.value = emptyList()
                    return@launch
                }
                val res: List<CardSearchItemResponse> = boardService.searchCards(
                    authHeader = "Bearer $token",
                    keyword = query,
                    sort = sort.name // enum 값을 String으로 변환 (LATEST, OLDEST 등)
                )

                /// API 응답(CardSearchItemResponse)을 실제 MyCardItem 모델로 정확하게 변환
                _searchResults.value = res.map { dto ->
                    MyCardItem(
                        id = dto.cardId,
                        imageUrl = dto.videoThumbnailUrl ?: "" // 썸네일이 null일 경우 빈 문자열로 처리
                    )
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                Log.e("MyCardViewModel", "Error searching cards", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    /** 검색 결과 초기화 */
    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}
