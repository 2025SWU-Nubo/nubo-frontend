package com.example.nubo.data.repository

import com.example.nubo.data.network.CardApiService
import javax.inject.Inject

//hilt di 적용
class CardRepository @Inject constructor(private val apiService: CardApiService) {
    fun getCards(token: String, sort: String, page: Int?, size: Int?) =
        apiService.getCards(token, "application/json", sort, page, size)
}
