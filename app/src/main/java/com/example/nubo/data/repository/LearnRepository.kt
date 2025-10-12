package com.example.nubo.data.repository

import com.example.nubo.data.model.DashboardResponse
import com.example.nubo.data.network.LearnService
import javax.inject.Inject
import javax.inject.Singleton

interface LearnRepository {
    suspend fun getDashboardStats(token: String): Result<DashboardResponse>
}

@Singleton
class LearnRepositoryImpl @Inject constructor(
    private val learnService: LearnService
) : LearnRepository {
    override suspend fun getDashboardStats(token: String): Result<DashboardResponse> {
        return try {
            val response = learnService.getDashboardStats("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
