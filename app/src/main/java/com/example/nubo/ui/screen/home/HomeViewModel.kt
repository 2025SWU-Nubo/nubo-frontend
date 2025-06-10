package com.example.nubo.ui.screen.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _cards = MutableLiveData<List<CardResponse>>()
    val cards: LiveData<List<CardResponse>> get() = _cards

    private val _cardDetail = MutableLiveData<CardDetailResponse?>()
    val cardDetail: LiveData<CardDetailResponse?> = _cardDetail

    private val _isLoading = MutableLiveData<Boolean>()

    private val _isDetailLoading = MutableLiveData<Boolean>()
    val isDetailLoading: LiveData<Boolean> = _isDetailLoading

    init {
        loadCards()
    }

    private fun loadCards() {
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

