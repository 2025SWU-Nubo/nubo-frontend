package com.example.nubo.deeplink

object DeepLinkStore {
    @Volatile var pendingCardId: Long? = null
    @Volatile var pendingGoUnread: Boolean = false
    @Volatile var pendingBoardId: Long? = null
    @Volatile var pendingBoardTitle: String? = null
    @Volatile var pendingInviteToken: String? = null
}
