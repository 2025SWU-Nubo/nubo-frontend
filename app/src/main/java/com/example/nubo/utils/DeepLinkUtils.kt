package com.example.nubo.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.nubo.deeplink.DeepLinkContract
import com.example.nubo.deeplink.DeepLinkStore
import com.example.nubo.ui.screen.onBoardingLogin.OnBoardingLoginActivity

// ---- 안전한 Intent extra 추출기 ----
private fun Intent.getAsString(key: String): String? {
    val b: Bundle = extras ?: return null
    if (!b.containsKey(key)) return null
    return when (val v = b.get(key)) {
        null -> null
        is String -> v
        is CharSequence -> v.toString()
        is Number -> v.toString()
        else -> v.toString()
    }
}

private fun Intent.getAsLong(key: String): Long? {
    val b: Bundle = extras ?: return null
    if (!b.containsKey(key)) return null
    return when (val v = b.get(key)) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull()
        is CharSequence -> v.toString().toLongOrNull()
        else -> null
    }
}

// 여러 키(camel/snake) 중 먼저 존재하는 값을 고름
private fun Intent.pickString(vararg keys: String): String? {
    for (k in keys) getAsString(k)?.let { return it }
    return null
}

private fun Intent.pickLong(vararg keys: String): Long? {
    for (k in keys) getAsLong(k)?.let { return it }
    return null
}

// ---- 여기가 핵심: 타입 안전하게 읽도록 수정 ----
fun cacheToStore(intent: Intent) {
    // 1) 명시적 딥링크 컨트랙트 우선
    when (intent.getAsString(DeepLinkContract.EXTRA_DEEPLINK_TARGET)) {
        DeepLinkContract.TARGET_CARD_DETAIL -> {
            // 카드 ID를 문자열로 캐시 (값이 Long으로 와도 toString 처리됨)
            val idStr = intent.getAsString(DeepLinkContract.EXTRA_CARD_ID)
                ?: intent.getAsLong(DeepLinkContract.EXTRA_CARD_ID)?.toString()
            if (!idStr.isNullOrBlank()) {
                DeepLinkStore.pendingCardId = idStr
            }
            return
        }
        DeepLinkContract.TARGET_CARD_UNREAD_LIST -> {
            DeepLinkStore.pendingGoUnread = true
            return
        }
        DeepLinkContract.TARGET_BOARD_DETAIL,
        DeepLinkContract.TARGET_BOARD_INVITE -> {
            DeepLinkStore.pendingBoardId =
                intent.getAsLong(DeepLinkContract.EXTRA_BOARD_ID)?.takeIf { it > 0L }
            DeepLinkStore.pendingBoardTitle =
                intent.getAsString(DeepLinkContract.EXTRA_BOARD_TITLE)
            DeepLinkStore.pendingInviteToken =
                intent.getAsString(DeepLinkContract.EXTRA_INVITE_TOKEN)
            return
        }
    }

    // 2) FCM data (camel/snake 혼재) 처리
    val type = intent.pickString("type", "TYPE")
    when (type) {
        // 서버가 card_id를 "123"(String)로 주기도, Long로 오기도 하므로 문자열로 그대로 저장
        "CARD_ADDED", "CARD_CREATED", "UNREAD_RECOMMEND" -> {
            val idStr = intent.pickString("card_id", "cardId")
                ?: intent.pickLong("card_id", "cardId")?.toString()
            if (!idStr.isNullOrBlank()) DeepLinkStore.pendingCardId = idStr
        }
        "REMINDER" -> {
            DeepLinkStore.pendingGoUnread = true
        }
        "BOARD", "BOARD_INVITE", "INVITE_RESULT" -> {
            DeepLinkStore.pendingOpenNotificationCenter = true
            val bid = intent.pickLong("board_id", "boardId")?.takeIf { it > 0L }
            DeepLinkStore.pendingBoardId = bid
            DeepLinkStore.pendingBoardTitle =
                intent.pickString("board_title", "boardTitle")
            DeepLinkStore.pendingInviteToken =
                intent.pickString("invitation_id", "invitationId")
        }
    }
}

// 온보딩(로그인)으로 보내는 헬퍼
fun startOnboardingForLogin(context: Context) {
    context.startActivity(
        Intent(context, OnBoardingLoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
}
