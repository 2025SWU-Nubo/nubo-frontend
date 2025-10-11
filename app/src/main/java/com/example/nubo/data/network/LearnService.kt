package com.example.nubo.data.network

import com.example.nubo.data.model.DashboardResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface LearnService {
    @GET("/api/stat/dashboard")
    suspend fun getDashboardStats(
        @Header("Authorization") authHeader: String
    ): DashboardResponse
}
