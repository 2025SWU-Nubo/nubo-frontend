package com.example.nubo.di

import com.example.nubo.data.repository.InviteRepositoryImpl
import com.example.nubo.data.repository.LearnRepository
import com.example.nubo.data.repository.LearnRepositoryImpl
import com.example.nubo.domain.repository.InviteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Bind domain interface to data implementation
    @Binds
    @Singleton
    abstract fun bindInviteRepository(
        impl: InviteRepositoryImpl
    ): InviteRepository

    // LearnRepository 바인딩
    @Binds
    @Singleton
    abstract fun bindLearnRepository(
        impl: LearnRepositoryImpl
    ): LearnRepository
}
