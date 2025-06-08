package com.example.nubo.ui.screen.cardupload

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardUploadActivity : AppCompatActivity() {

    private val cardUploadViewModel: CardUploadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Compose 화면으로 전환하거나 ViewBinding 등 원하는 방식으로 처리 가능
        handleSharedVideoUrl(intent)
    }

    private fun handleSharedVideoUrl(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val videoUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            videoUrl?.let {
                // 바로 업로드 or Compose 화면으로 전달
                val accessToken = "ACCESS_TOKEN" // 실제로는 SharedPreferences 또는 DI로 가져오기
                cardUploadViewModel.uploadCard(accessToken, it)
                observeUploadResult()
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

