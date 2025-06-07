package com.example.nubo.data.network

import com.example.nubo.data.model.CardResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CardApiService {
    @GET("api/card")
    fun getCards(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Query("sort") sort: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): Call<List<CardResponse>>
}
