package com.example.nubo.data.model

data class CardUploadRequest(
    val videoUrl: String,
    val boardId: Long? = null
)
