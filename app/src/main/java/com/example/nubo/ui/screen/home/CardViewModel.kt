package com.example.nubo.ui.screen.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository: CardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _cards = MutableLiveData<List<CardResponse>>()
    val cards: LiveData<List<CardResponse>> get() = _cards

    private val _cardDetail = MutableLiveData<CardDetailResponse?>()
    val cardDetail: LiveData<CardDetailResponse?> = _cardDetail

    private val _isDetailLoading = MutableLiveData<Boolean>()
    val isDetailLoading: LiveData<Boolean> = _isDetailLoading

    fun loadCards(token: String, sort: String = "latest", page: Int? = null, size: Int? = null) {
        repository.getCards(token, sort, page, size)
            .enqueue(object : Callback<List<CardResponse>> {
                override fun onResponse(
                    call: Call<List<CardResponse>>,
                    response: Response<List<CardResponse>>
                ) {
                    if (response.isSuccessful) {
                        _cards.value = response.body()
                    } else {
                        // TODO: error handling
                    }
                }

                override fun onFailure(call: Call<List<CardResponse>>, t: Throwable) {
                    // TODO: error handling
                }
            })
    }


    fun getCardDetail(cardId: Int) {
        _isDetailLoading.value = true

        authRepository.getAccessToken()?.let { token ->
            repository.getCardDetail(token, cardId).enqueue(object : Callback<CardDetailResponse> {
                override fun onResponse(call: Call<CardDetailResponse>, response: Response<CardDetailResponse>) {
                    _isDetailLoading.value = false
                    if (response.isSuccessful) {
                        _cardDetail.value = response.body()
                    } else {
                        Log.e("CardViewModel", "카드 상세 조회 실패: ${response.code()}")
                        _cardDetail.value = null
                    }
                }

                override fun onFailure(call: Call<CardDetailResponse>, t: Throwable) {
                    _isDetailLoading.value = false
                    Log.e("CardViewModel", "카드 상세 조회 오류", t)
                    _cardDetail.value = null
                }
            })
        }
    }
}

