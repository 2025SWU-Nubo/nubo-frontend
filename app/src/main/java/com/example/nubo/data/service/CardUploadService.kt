package com.example.nubo.data.service

import com.example.nubo.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.components.toast.AppToastType
import com.example.nubo.data.model.CardUploadRequest
import com.example.nubo.data.model.CardUploadResponse
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.ui.component.toast.GlobalToastBus
import com.example.nubo.utils.AppForegroundTracker
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class CardUploadService: Service() {

    @Inject
    lateinit var cardRepository: CardRepository

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val PROGRESS_NOTIFICATION_ID = 1001

        const val EXTRA_ACCESS_TOKEN = "access_token" // Intent에서 토큰을 전달받기 위한 키
        const val EXTRA_VIDEO_URL = "video_url" // Intent에서 비디오 URL을 전달받기 위한 키
        const val EXTRA_BOARD_ID = "board_id" // Intent에서 보드 ID를 전달받기 위한 키


    fun startService(context: Context, accessToken: String, videoUrl: String, boardId: Long? = null) {
        val intent = Intent(context, CardUploadService::class.java).apply {
            putExtra(EXTRA_ACCESS_TOKEN, accessToken)  // 토큰 intent에 추가
            putExtra(EXTRA_VIDEO_URL, videoUrl)  // 비디오 URL을 intent에 추가
            boardId?.let { putExtra(EXTRA_BOARD_ID, it) }  // 보드 id가 있으면 intent에 추가
        }

        // API 레벨에 따라 서비스 시작 방법 분기 처리
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent) // Android O 이상: 포그라운드 서비스로 시작
        } else {
            context.startService(intent) // Android N 이하: 일반 서비스로 시작
        }
    }
}

    override fun onCreate() {
        super.onCreate()
        com.example.nubo.push.PushChannels.ensure(this)// 알림 채널 생성 (Android O 이상 필수)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent에서 전달받은 데이터 추출
        val accessToken = intent?.getStringExtra(EXTRA_ACCESS_TOKEN)
        val videoUrl = intent?.getStringExtra(EXTRA_VIDEO_URL)
        val boardId = intent?.getLongExtra(EXTRA_BOARD_ID, -1L)?.takeIf { it != -1L }

        // 필수 데이터 없을 시 서비스 종료
        if (accessToken.isNullOrEmpty() || videoUrl.isNullOrEmpty()) {
            stopSelf() //서비스 종료
            return START_NOT_STICKY
        }

        //카드 업로드 중 알림 표시
        try {
            startForeground(PROGRESS_NOTIFICATION_ID, createProgressNotification())
        } catch (se: SecurityException) {
            // Usually due to missing POST_NOTIFICATIONS or FGS permission/type
            Log.e("CardUploadService", "startForeground SecurityException", se)
            toastOnMain("알림 권한이 없어 업로드 알림을 표시할 수 없습니다.")
        }
        // 백그라운드에서 카드 업로드 실행
        uploadCard(accessToken, videoUrl, boardId)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private fun toastOnMain(msg: String,type: AppToastType = AppToastType.NORMAL) {
        if (AppForegroundTracker.isForeground) {
            // 앱이 포그라운드일 때만 커스텀 토스트
            GlobalToastBus.showMessage(
                message = msg,
                type = type,
                durationMillis = 2200,
                preDelayMillis = 350
            )
        } else {
            // 앱이 떠 있지 않을 때는 시스템 토스트만
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 진행 중 알림 생성
    private fun createProgressNotification(): Notification {
        val builder = NotificationCompat.Builder(this, com.example.nubo.push.PushChannels.CH_UPLOAD_PROGRESS)
            .setContentTitle("카드 생성 중")
            .setContentText("영상을 업로드하고 있습니다..")
            .setSmallIcon(R.drawable.nubo_symbol)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun uploadCard(accessToken: String, videoUrl: String, boardId: Long?) {
        val request = CardUploadRequest(videoUrl)

        cardRepository.uploadCard( request)
            .enqueue(object : Callback<CardUploadResponse> {
                override fun onResponse(
                    call: Call<CardUploadResponse?>,
                    response: Response<CardUploadResponse?>
                ) {
                    // 1) 이미 추가된 영상 (409) 먼저 처리
                    if (response.code() == 409) {
                        Log.w("CardUploadService", "이미 추가된 영상(409)")
                        stopForeground(true)
                        toastOnMain(
                            msg = "이미 추가된 영상이에요",
                            type = AppToastType.NEGATIVE
                        )
                        stopSelf()
                        return
                    }

                    // 2) 그 외 성공이면 → 이제부터 카드 생성 프로세스 시작됨
                    if (response.isSuccessful) {
                        Log.d("CardUploadService", "카드 업로드 성공: ${response.body()}")

                        stopForeground(true)
                    } else {
                        Log.e(
                            "CardUploadService",
                            "카드 업로드 실패: ${response.code()} ${response.message()}"
                        )
                        stopForeground(true)
                        toastOnMain(
                            msg = "업로드에 실패했어요. 잠시 후 다시 시도해 주세요.",
                            type = AppToastType.NEGATIVE
                        )
                    }
                    stopSelf()
                }

                override fun onFailure(call: Call<CardUploadResponse?>, t: Throwable) {
                    Log.e("CardUploadService", "카드 업로드 네트워크 에러", t)
                    stopForeground(true)
                    toastOnMain(
                        msg = "네트워크 오류로 업로드에 실패했어요.",
                        type = AppToastType.NEGATIVE
                    )
                    stopSelf()
                }
            })
    }

}


