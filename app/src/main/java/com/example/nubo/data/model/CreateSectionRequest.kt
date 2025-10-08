package com.example.nubo.data.model

data class CreateSectionRequest (
    val name: String,
    val boardType: String = "SECTION",
    val shared: Boolean,
    val favorite: Boolean = false,
    val parentBoardId: Long? = null,        // SECTION일 때 필수, BOARD일 때 null
    val source: String = "USER",
    val memberEmails: List<String>? = null
)
