package com.example.nubo.data.model

data class UnviewedCardsByBoardsRequest(
    val boardIds: List<Long>,
    val limit: Int? = null // If null, server default applies (e.g., 20)
)
