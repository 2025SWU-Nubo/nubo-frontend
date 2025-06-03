package com.example.nubo.data.model

//HomeScreen 최근 본 보드 아이템
data class BoardThumbnailCardItem(
    val category: String,
    val imageResId: Int? = null,  // 저장된 image
    val imageUrl: String? = null  // url 입력시
)
