package com.example.nubo.ui.screen.cardupload

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.service.CardUploadService
import com.example.nubo.utils.NotificationPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardUploadActivity : AppCompatActivity() {

    private val cardUploadViewModel: CardUploadViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    private var pendingShareIntent: Intent? = null

    // Runtime permission launcher for POST_NOTIFICATIONS (Android 13+)

    private val requestNotifPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        val intentToUse = pendingShareIntent
        pendingShareIntent = null

        if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Permission granted (or not required) → proceed
            intentToUse?.let { startUploadFromShareIntent(it) } ?: finish()
        } else {
            // Permission denied → inform and finish
            Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Compose 화면으로 전환하거나 ViewBinding 등 원하는 방식으로 처리 가능
        handleSharedVideoUrl(intent)
    }

    private fun startUploadFromShareIntent(shareIntent: Intent) {
        val url = shareIntent.getStringExtra(Intent.EXTRA_TEXT) ?: return finish()
        val token = authRepository.getAccessToken() ?: return finish()

        Toast.makeText(this, "카드 생성 중입니다...", Toast.LENGTH_SHORT).show()

        CardUploadService.startService(
            context = this,
            accessToken = token,
            videoUrl = url
        )
        // Close quickly so user can continue other tasks
        finish()
    }

    private fun handleSharedVideoUrl(incoming: Intent?) {

        // Intent의 액션이 SEND이고 타입이 text/plain인지 확인 (공유 인텐트인지 확인)
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            // 공유된 텍스트(URL) 추출
            val videoUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            val accessToken = authRepository.getAccessToken()


            if (videoUrl.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            Log.d("CardUploadActivity", "token=${accessToken.take(8)}...")


            // Android 13+ requires POST_NOTIFICATIONS to show FGS notification reliably
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Save intent then request permission; continue in callback
                pendingShareIntent = incoming
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // No runtime permission required
                incoming?.let { safeIntent ->
                    startUploadFromShareIntent(safeIntent)
                } ?: finish()
            }
        } else {
            finish()
        }
    }
}

