package com.example.nubo.deeplink

object DeepLinkStore {
    @Volatile var pendingCardId: String? = null
    @Volatile var pendingGoUnread: Boolean = false
    @Volatile var pendingOpenNotificationCenter: Boolean = false
    @Volatile var pendingBoardId: Long? = null
    @Volatile var pendingBoardTitle: String? = null
    @Volatile var pendingInviteToken: String? = null
}
