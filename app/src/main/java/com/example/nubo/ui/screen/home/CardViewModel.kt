package com.example.nubo.ui.screen.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    private val _cards = MutableLiveData<List<CardResponse>>()
    val cards: LiveData<List<CardResponse>> get() = _cards

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
}

