package com.example.nubo.ui.screen.cardupload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class CardUploadViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    private val _uploadResult = MutableLiveData<CardUploadResponse>()
    val uploadResult: LiveData<CardUploadResponse> get() = _uploadResult

    fun uploadCard(token: String, videoUrl: String, boardId: Long? = null) {
        val request = CardUploadRequest(videoUrl, boardId)
        repository.uploadCard(token, request).enqueue(object : Callback<CardUploadResponse> {
            override fun onResponse(
                call: Call<CardUploadResponse>,
                response: Response<CardUploadResponse>
            ) {
                if (response.isSuccessful) {
                    _uploadResult.value = response.body()
                } else {
                    // TODO: 에러 처리
                }
            }

            override fun onFailure(call: Call<CardUploadResponse>, t: Throwable) {
                // TODO: 에러 처리
            }
        })
    }
}

