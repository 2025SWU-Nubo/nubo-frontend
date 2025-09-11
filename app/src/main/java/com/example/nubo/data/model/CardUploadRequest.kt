package com.example.nubo.data.model

// 여러 보드/섹션 ID를 함께 보낼 수 있도록 확장
data class CardUploadRequest(
    val videoUrl: String,          // 업로드할 영상 URL
    val boardIds: List<Long>? = null   // 선택된 보드/섹션 ID 목록 (없으면 null)
)
