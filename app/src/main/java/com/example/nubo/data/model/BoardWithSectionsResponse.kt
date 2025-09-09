package com.example.nubo.data.model

// 보드 루트 DTO
data class BoardWithSectionsResponse(
    val id: Long,
    val name: String,
    val favorite: Boolean,
    val sections: List<BoardSectionDto>
)

// 섹션 DTO
data class BoardSectionDto(
    val id: Long,
    val name: String,
    val favorite: Boolean
)
