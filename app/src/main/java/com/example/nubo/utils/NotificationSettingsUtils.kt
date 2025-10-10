package com.example.nubo.utils

import androidx.activity.result.ActivityResult
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Check if app notifications are enabled at system level
@Composable
fun rememberNotificationSettingsLauncher(
    onReturn: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    // Re-check when returning from settings
    return rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onReturn() // caller will re-check notifications here
    }
}

fun buildAppNotificationSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
            // Some OEMs still read legacy extras:
            putExtra("app_package", context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

// Open OS "App notifications" settings for this app
fun openAppNotificationSettings(context: android.content.Context) {
    // Intent to the app's notification settings page
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        // Fallback for pre-O
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(intent)
}
