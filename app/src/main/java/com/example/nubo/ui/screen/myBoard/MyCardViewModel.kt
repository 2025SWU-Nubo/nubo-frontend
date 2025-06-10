package com.example.nubo.ui.screen.myBoard

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.model.myBoard.MyCardItem
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
        fetchCards()
    }

    private fun fetchCards() {
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
                            _cards.value = response.body()?.map {
                                MyCardItem(
                                    id = it.id,
                                    imageUrl = it.videoThumbnailUrl
                                )
                            } ?: emptyList()
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
