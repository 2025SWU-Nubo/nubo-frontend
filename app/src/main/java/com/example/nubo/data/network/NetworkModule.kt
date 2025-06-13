package com.example.nubo.data.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // 로깅용
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://b08f-2406-5900-1038-741b-c576-33f6-c307-2665.ngrok-free.app")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideBoardService(retrofit: Retrofit): BoardService {
        return retrofit.create(BoardService::class.java)
    }

    @Provides
    @Singleton
    fun provideCardApiService(retrofit: Retrofit): CardService {
        return retrofit.create(CardService::class.java)
    }

}


//@Module
//@InstallIn(SingletonComponent::class)
//object NetworkModule {
//
//    private const val BASE_URL = "https://b727-2406-5900-1038-741b-c576-33f6-c307-2665.ngrok-free.app"
//
//    @Provides
//    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
//        HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//
//    @Provides
//    fun provideOkHttpClient(
//        logging: HttpLoggingInterceptor
//    ): OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(logging)
//        .connectTimeout(10, TimeUnit.SECONDS)
//        .writeTimeout(120, TimeUnit.SECONDS)
//        .readTimeout(600, TimeUnit.SECONDS)
//        .build()
//
//    @Provides
//    fun provideRetrofit(
//        okHttpClient: OkHttpClient
//    ): Retrofit = Retrofit.Builder()
//        .baseUrl(BASE_URL)
//        .addConverterFactory(GsonConverterFactory.create())
//        .client(okHttpClient)
//        .build()
//
//    @Provides
//    fun provideCardApiService(
//        retrofit: Retrofit
//    ): CardService = retrofit.create(CardService::class.java)
//
//    @Provides
//    fun provideAuthService(
//        retrofit: Retrofit
//    ): AuthService = retrofit.create(AuthService::class.java)
//
//    @Provides
//    fun provideBoardService(
//        retrofit: Retrofit
//    ): BoardService = retrofit.create(BoardService::class.java)
//}
