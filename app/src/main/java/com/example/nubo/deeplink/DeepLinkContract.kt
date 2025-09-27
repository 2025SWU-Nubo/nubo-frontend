package com.example.nubo.deeplink

object DeepLinkContract {
    // extras
    const val EXTRA_DEEPLINK_TARGET = "deeplink_target"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    const val EXTRA_CARD_ID = "card_id"
    const val EXTRA_BOARD_ID = "board_id"
    const val EXTRA_BOARD_TITLE = "board_title"
    const val EXTRA_INVITE_TOKEN = "invite_token"

    // targets
    const val TARGET_CARD_DETAIL = "target_card_detail"
    const val TARGET_CARD_UNREAD_LIST = "target_card_unread"
    const val TARGET_BOARD_DETAIL = "target_board_detail"
    const val TARGET_BOARD_INVITE = "target_board_invite"

    // notification types
    const val NOTI_CARD_CREATED = "card_created"
    const val NOTI_CARD_UNREAD = "card_unread"
    const val NOTI_BOARD_INVITE = "board_invite"
}

