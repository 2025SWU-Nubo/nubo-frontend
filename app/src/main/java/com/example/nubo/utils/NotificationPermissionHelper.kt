package com.example.nubo.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationPermissionHelper {
    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001 // 알림 권한 요청 코드

        /**
         * 알림 권한이 있는지 확인
         * @param context 컨텍스트
         * @return 알림 권한 여부 (Android 13 미만은 항상 true)
         */
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 이상: POST_NOTIFICATIONS 권한 확인
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // Android 12 이하: 항상 true (권한 불필요)
                true
            }
        }

        /**
         * 알림 권한 요청이 필요한지 확인
         * @param context 컨텍스트
         * @return 권한 요청 필요 여부
         */
        fun shouldRequestNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 이상에서만 권한 요청 필요
                !hasNotificationPermission(context)
            } else {
                // Android 12 이하: 권한 요청 불필요
                false
            }
        }

        /**
         * 알림 권한 요청
         * @param activity 액티비티
         */
        fun requestNotificationPermission(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        /**
         * 사용자에게 알림 권한의 필요성을 설명하고 권한 요청
         * @param activity 액티비티
         * @param onPermissionRequested 권한 요청 후 콜백
         */
        fun showNotificationPermissionDialog(
            activity: Activity,
            onPermissionRequested: () -> Unit = {}
        ) {
            if (!shouldRequestNotificationPermission(activity)) {
                // 권한 요청이 필요하지 않으면 바로 콜백 실행
                onPermissionRequested()
                return
            }

            AlertDialog.Builder(activity)
                .setTitle("알림 권한 필요") // 다이얼로그 제목
                .setMessage(
                    "카드 업로드 완료 알림을 받기 위해서는 알림 권한이 필요합니다.\n" +
                        "권한을 허용하시겠습니까?"
                ) // 다이얼로그 메시지
                .setPositiveButton("허용") { dialog, _ ->
                    dialog.dismiss() // 다이얼로그 닫기
                    requestNotificationPermission(activity) // 권한 요청
                    onPermissionRequested() // 콜백 실행
                }
                .setNegativeButton("나중에") { dialog, _ ->
                    dialog.dismiss() // 다이얼로그 닫기
                    onPermissionRequested() // 콜백 실행
                }
                .setCancelable(false) // 뒤로가기 버튼으로 취소 불가
                .show()
        }

        /**
         * 권한 요청 결과 처리
         * @param requestCode 요청 코드
         * @param permissions 권한 배열
         * @param grantResults 권한 결과 배열
         * @param onGranted 권한 허용 시 콜백
         * @param onDenied 권한 거부 시 콜백
         */
        fun handlePermissionResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray,
            onGranted: () -> Unit = {},
            onDenied: () -> Unit = {}
        ) {
            if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허용됨
                    onGranted()
                } else {
                    // 권한 거부됨
                    onDenied()
                }
            }
        }

        /** 시스템(앱 전체) 알림 허용 여부 */
        fun isSystemNotificationEnabled(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled()

        /** 특정 채널 허용 여부 (O+), 없으면 false */
        fun isChannelEnabled(context: Context, channelId: String): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
            val nm = context.getSystemService(NotificationManager::class.java)
            val ch = nm.getNotificationChannel(channelId) ?: return false
            return ch.importance != NotificationManager.IMPORTANCE_NONE
        }

        /** 앱 알림 설정 화면 열기 */
        fun openAppNotificationSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
        }

        /** 채널 설정 화면 열기 (O+), 미만은 앱 설정으로 */
        fun openChannelSettings(context: Context, channelId: String) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
        }
    }
}
