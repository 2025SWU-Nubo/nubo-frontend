package com.example.nubo.data.model

data class CardResponse(
    val cardId: Int,
    val videoThumbnailUrl: String,
    val viewed: Boolean,
    val favorite: Boolean
)
