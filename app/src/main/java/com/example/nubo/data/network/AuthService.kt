package com.example.nubo.data.network

import com.example.nubo.data.model.LoginRequest
import com.example.nubo.data.model.LoginResponse
import com.example.nubo.data.model.TokenCheckRequest
import com.example.nubo.data.model.TokenValidationResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthService {
    @Headers("Content-Type: application/json")
    @POST("/api/auth/login/google")
    fun loginWithGoogle(@Body request: LoginRequest): Call<LoginResponse>

    //토큰 유효 검증
    @Headers("Content-Type: application/json")
    @POST("/api/auth/check-token")
    fun checkToken(@Body request: TokenCheckRequest): Call<TokenValidationResponse>
}
