package com.example.nubo.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val BASE_URL = "https://b08f-2406-5900-1038-741b-c576-33f6-c307-2665.ngrok-free.app"
    private val logging = HttpLoggingInterceptor(PrettyJsonLogger()).apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }


    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    // 카드 전체 조회
    val cardApiService: CardService by lazy {
        retrofit.create(CardService::class.java)
    }

    // 나의 보드 전체 조회
    val boardService: BoardService by lazy {
        retrofit.create(BoardService::class.java)
    }
}
