package com.example.nubo.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val BASE_URL = "https://"

    private val logging = HttpLoggingInterceptor(PrettyJsonLogger()).apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .build()

    val cardApiService: CardApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(CardApiService::class.java)
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

