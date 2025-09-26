package com.example.nubo.ui.screen.myBoard

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
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

@HiltViewModel
class MyCardViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val authRepository: AuthRepository
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
                    sort = CardSort.LATEST,       // UPPERCASE to meet server spec
                    filter = CardFilter.ALL,
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
}
