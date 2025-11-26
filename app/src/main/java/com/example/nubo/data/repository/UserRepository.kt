package com.example.nubo.data.repository

import android.util.Log
import com.example.nubo.data.network.UserService
import retrofit2.HttpException
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userApi: UserService
) {

    suspend fun markTutorialCompleted(): Result<Unit> {
        return runCatching {
            val res = userApi.markTutorialCompleted()

            if (!res.isSuccessful) {
                throw HttpException(res)
            }

            Log.d("UserRepository", "Tutorial completed flag updated")
        }
    }
}
