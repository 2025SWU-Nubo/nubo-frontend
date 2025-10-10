package com.example.nubo.data.model

data class UpsertBoardRequest(
    val name: String,
    val boardType: String = "BOARD", // 보드 생성 고정
    val source: String = "USER",  // 보드 생성 출처 고정
    val shared: Boolean,
    val parentBoardId: Long? = null,        // SECTION일 때 필수, BOARD일 때 null
    val favorite: Boolean = false,
    val memberEmails: List<String>? = null
)
