package com.example.nubo.ui.screen.cardupload

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.nubo.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardUploadActivity : AppCompatActivity() {

    private val cardUploadViewModel: CardUploadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Compose 화면으로 전환하거나 ViewBinding 등 원하는 방식으로 처리 가능
        handleSharedVideoUrl(intent)
    }

    @Inject
    lateinit var authRepository: AuthRepository
    private fun handleSharedVideoUrl(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val videoUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            videoUrl?.let {
                val accessToken = authRepository.getAccessToken()
                if (accessToken != null) {
                    Log.d("handleSharedVideoUrl",accessToken)
                }
                if (!accessToken.isNullOrEmpty()) {
                    cardUploadViewModel.uploadCard(accessToken, it)
                    observeUploadResult()
                } else {
                    Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private fun observeUploadResult() {
        cardUploadViewModel.uploadResult.observe(this) { response ->
            response?.let {
                Toast.makeText(this, "영상 업로드가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

