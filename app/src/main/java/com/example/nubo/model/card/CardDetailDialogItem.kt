package com.example.nubo.model.card

data class CardDetailDialogItem(
    val id: Int,
    val imageUrl: String, // videoThumbnailUrl 매핑
    val videoUrl: String,
    val title: String,
    val category: String,   // boardName 매핑
    val boardSource: String,  //boardSource
    val description: String,  // summary 매핑
    val date: String,  // createdAt 매핑
    val videoPlatform: String,
)


