package com.example.nubo.ui.screen.cardupload

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.example.components.toast.AppToastHostState
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastOverlay
import com.example.components.toast.AppToastType
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.service.CardUploadService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardUploadActivity : AppCompatActivity() {

    private val cardUploadViewModel: CardUploadViewModel by viewModels()

    @Inject lateinit var authRepository: AuthRepository

    // POST_NOTIFICATIONS 권한 요청 도중 보류할 공유 인텐트
    private var pendingShareIntent: Intent? = null

    // 커스텀 토스트를 띄우기 위한 호스트 상태와 ComposeView
    private var toastHost: AppToastHostState? = null
    private var toastComposeView: ComposeView? = null

    // Android 13 이상에서 알림 권한 요청 런처
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val intentToUse = pendingShareIntent
        pendingShareIntent = null

        if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // 권한 허용 또는 필요 없음 → 업로드 진행
            intentToUse?.let { startUploadFromShareIntent(it) } ?: finish()
        } else {
            // 권한 거부 → 커스텀 토스트 안내 후 종료
            lifecycleScope.launch {
                showAppToast(
                    title = "알림 권한이 필요합니다",
                    type = AppToastType.NEGATIVE,
                    layout = AppToastLayout.TitleOnly,
                    duration = 1400
                )
                // 토스트가 보일 시간을 조금 확보한 뒤 종료
                delay(500)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 현재 윈도우 최상단에 커스텀 토스트 오버레이 설치
        installToastOverlayIfNeeded()

        // 공유 인텐트 처리 시작
        handleSharedVideoUrl(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 윈도우 누수 방지를 위해 ComposeView 제거
        (toastComposeView?.parent as? ViewGroup)?.removeView(toastComposeView)
        toastComposeView = null
        toastHost = null
    }

    // 공유 인텐트로부터 업로드 시작
    private fun startUploadFromShareIntent(shareIntent: Intent) {
        val url = shareIntent.getStringExtra(Intent.EXTRA_TEXT) ?: run {
            finish()
            return
        }
        val token = authRepository.getAccessToken() ?: run {
            finish()
            return
        }

        CardUploadService.startService(
            context = this,
            accessToken = token,
            videoUrl = url
        )

        // 사용자가 바로 다른 작업을 할 수 있도록 빠르게 종료
        finish()
    }

    // 공유 인텐트 유효성 검사 및 권한 처리 분기
    private fun handleSharedVideoUrl(incoming: Intent?) {
        // 액션이 SEND이고 타입이 text/plain인지 확인
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val videoUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            val accessToken = authRepository.getAccessToken()

            if (videoUrl.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
                // 로그인 필요 시 커스텀 토스트 안내 후 종료
                lifecycleScope.launch {
                    showAppToast(
                        title = "로그인이 필요합니다",
                        type = AppToastType.NEGATIVE,
                        layout = AppToastLayout.TitleOnly,
                        duration = 1400
                    )
                    delay(500)
                    finish()
                }
                return
            }

            Log.d("CardUploadActivity", "token=${accessToken.take(8)}...")

            // Android 13 이상은 알림 권한 필요
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pendingShareIntent = incoming
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // 권한 불필요
                incoming?.let { safeIntent ->
                    startUploadFromShareIntent(safeIntent)
                } ?: finish()
            }
        } else {
            // 공유 인텐트가 아닌 경우 종료
            finish()
        }
    }

    // 커스텀 토스트 오버레이를 한 번만 설치
    private fun installToastOverlayIfNeeded() {
        if (toastHost != null && toastComposeView != null) return

        val host = AppToastHostState()
        val cv = ComposeView(this).apply {
            // 전체 화면 크기 차지 → 하단 중앙 정렬 가능
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                // 팝업 형태로 최상단에 토스트를 렌더링
                AppToastOverlay(hostState = host)
            }
        }
        // 현재 윈도우에 오버레이 추가
        addContentView(cv, cv.layoutParams)

        toastHost = host
        toastComposeView = cv
    }

    // 액티비티 컨텍스트에서 커스텀 토스트를 간편 호출
    // duration은 화면에 머무는 시간
    private fun showAppToast(
        title: String,
        type: AppToastType,
        layout: AppToastLayout = AppToastLayout.TitleOnly,
        duration: Int = 1600
    ) {
        val host = toastHost ?: return
        lifecycleScope.launch {
            host.show(
                title = androidx.compose.ui.text.AnnotatedString(title),
                layout = layout,
                type = type,
                durationMillis = duration
            )
        }
    }
}
