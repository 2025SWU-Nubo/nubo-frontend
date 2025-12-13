package com.example.nubo.data.network

import com.example.nubo.data.model.DashboardResponse
import retrofit2.http.GET

interface LearnService {
    @GET("/api/stat/dashboard")
    suspend fun getDashboardStats(): DashboardResponse
}
