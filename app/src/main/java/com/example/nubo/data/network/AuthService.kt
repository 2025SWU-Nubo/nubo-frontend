package com.example.nubo.data.network

import com.example.nubo.data.model.LoginRequest
import com.example.nubo.data.model.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthService {
    @Headers("Content-Type: application/json")
    @POST("/api/auth/login/google")
    fun loginWithGoogle(@Body request: LoginRequest): Call<LoginResponse>
}
