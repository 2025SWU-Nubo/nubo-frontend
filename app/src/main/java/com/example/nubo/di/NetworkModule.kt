// com/example/nubo/di/NetworkModule.kt
package com.example.nubo.di

import android.content.Context
import com.example.nubo.data.network.AuthService
import com.example.nubo.data.network.BoardService
import com.example.nubo.data.network.CardService
import com.example.nubo.data.network.LearnService
import com.example.nubo.data.network.NotificationService
import com.example.nubo.data.network.ProfileService
import com.example.nubo.data.network.TokenAuthenticator
import com.example.nubo.data.network.TokenInterceptor
import com.example.nubo.data.network.UserService
import com.example.nubo.data.network.VideoService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    // 로깅 인터셉터 제공
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }


    @Provides
    @Singleton
    fun provideOkHttpClient(
        httpLogging: HttpLoggingInterceptor,
        tokenInterceptor: TokenInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            //  토큰/우회 헤더 → 로깅
            .addInterceptor(tokenInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(httpLogging)
            .connectTimeout(10, TimeUnit.SECONDS)   // TCP connect
            .writeTimeout(120, TimeUnit.SECONDS)    // request body
            .readTimeout(600, TimeUnit.SECONDS)     // server processing
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://nubo-backend.fly.dev")
  //          .baseUrl("https://janae-nontenantable-endosmotically.ngrok-free.dev")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides @Singleton
    fun provideBoardService(retrofit: Retrofit): BoardService =
        retrofit.create(BoardService::class.java)

    @Provides @Singleton
    fun provideCardApiService(retrofit: Retrofit): CardService =
        retrofit.create(CardService::class.java)

    @Provides @Singleton
    fun provideUserService(retrofit: Retrofit): UserService =
        retrofit.create(UserService::class.java)

    @Provides @Singleton
    fun provideVideoApiService(retrofit: Retrofit): VideoService {
        return retrofit.create(VideoService::class.java)
    }

    @Provides @Singleton
    fun provideProfileApiService(retrofit: Retrofit): ProfileService {
        return retrofit.create(ProfileService::class.java)
    }

    @Provides @Singleton
    fun provideNotificationService(retrofit: Retrofit): NotificationService =
        retrofit.create(NotificationService::class.java)

    // LearnService 제공
    @Provides @Singleton
    fun provideLearnService(retrofit: Retrofit): LearnService =
        retrofit.create(LearnService::class.java)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        @ApplicationContext context: Context,
    ): TokenAuthenticator = TokenAuthenticator(context)

}
