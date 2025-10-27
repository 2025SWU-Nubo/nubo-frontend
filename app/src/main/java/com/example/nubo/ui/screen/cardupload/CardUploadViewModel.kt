package com.example.nubo.ui.screen.cardupload

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject




@HiltViewModel
class CardUploadViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    sealed class UploadEvent {
        // 업로드 시작(요청 발행)
        data object Started : UploadEvent()
        // 서버가 생성/수락 완료)
        data object Succeeded : UploadEvent()
        // 200 OK created (혹은 처리 시작 시점)
//        data object Created : UploadEvent()
        // 409 Conflict (이미 생성됨)
        data object AlreadyExists : UploadEvent()
        // 그 외 실패
        data class Failed(val message: String) : UploadEvent()
    }

    private val _uploadResult = MutableLiveData<CardUploadResponse>()
    val uploadResult: LiveData<CardUploadResponse> get() = _uploadResult

    private val _uploadEvents = MutableSharedFlow<UploadEvent>(
        replay = 0, extraBufferCapacity = 1
    )
    val uploadEvents: SharedFlow<UploadEvent> = _uploadEvents.asSharedFlow()


    fun uploadCard(token: String, videoUrl: String, boardIds: List<Long>?) {

        val req = CardUploadRequest(
            videoUrl = videoUrl,
            boardIds = boardIds
        )

        // Optional: immediate feedback
        _uploadEvents.tryEmit(UploadEvent.Started)
        repository.uploadCard(token, req)  // returns Call<CardUploadResponse>
            .enqueue(object : retrofit2.Callback<CardUploadResponse> {
                override fun onResponse(
                    call: retrofit2.Call<CardUploadResponse>,
                    response: retrofit2.Response<CardUploadResponse>
                ) {
                    when (response.code()) {
                        200,201 -> _uploadEvents.tryEmit(UploadEvent.Succeeded)
                        409 -> _uploadEvents.tryEmit(UploadEvent.AlreadyExists)
                        else -> _uploadEvents.tryEmit(
                            UploadEvent.Failed("업로드 실패: ${response.code()}")
                        )
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<CardUploadResponse>,
                    t: Throwable
                ) {
                    _uploadEvents.tryEmit(
                        UploadEvent.Failed("네트워크 오류: ${t.localizedMessage ?: "알 수 없음"}")
                    )
                }
            })
    }
}

