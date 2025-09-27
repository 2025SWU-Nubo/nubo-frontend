package com.example.nubo.utils

import android.content.Context
import android.content.Intent
import com.example.nubo.deeplink.DeepLinkContract
import com.example.nubo.deeplink.DeepLinkStore
import com.example.nubo.ui.screen.onBoardingLogin.OnBoardingLoginActivity

fun cacheToStore(intent: Intent) {
    when (intent.getStringExtra(DeepLinkContract.EXTRA_DEEPLINK_TARGET)) {
        DeepLinkContract.TARGET_CARD_DETAIL -> {
            intent.getLongExtra(DeepLinkContract.EXTRA_CARD_ID, -1L)
                .takeIf { it > 0 }?.let { DeepLinkStore.pendingCardId = it }
        }
        DeepLinkContract.TARGET_CARD_UNREAD_LIST -> {
            DeepLinkStore.pendingGoUnread = true
        }
        DeepLinkContract.TARGET_BOARD_DETAIL,
        DeepLinkContract.TARGET_BOARD_INVITE -> {
            DeepLinkStore.pendingBoardId =
                intent.getLongExtra(DeepLinkContract.EXTRA_BOARD_ID, -1L).takeIf { it > 0 }
            DeepLinkStore.pendingBoardTitle =
                intent.getStringExtra(DeepLinkContract.EXTRA_BOARD_TITLE)
            DeepLinkStore.pendingInviteToken =
                intent.getStringExtra(DeepLinkContract.EXTRA_INVITE_TOKEN)
        }
    }
}

fun startOnboardingForLogin(context: Context) {
    context.startActivity(
        Intent(context, OnBoardingLoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
}
