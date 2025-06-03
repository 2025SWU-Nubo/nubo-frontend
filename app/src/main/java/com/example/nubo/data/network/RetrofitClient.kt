package com.example.nubo.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private const val BASE_URL = "https://bc9c-2406-5900-1038-741b-f496-70c5-30c9-f684.ngrok-free.app" // TODO: 서버 URL로 교체

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    // 나의 보드 전체 조회
    val boardService: BoardService by lazy {
        retrofit.create(BoardService::class.java)
    }

    // 나의 카드 전체 조회
    val cardService: CardService by lazy {
        retrofit.create(CardService::class.java)
    }
}
