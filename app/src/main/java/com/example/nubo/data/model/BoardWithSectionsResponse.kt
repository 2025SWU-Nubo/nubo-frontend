package com.example.nubo.data.model

//영상 추가 시 보드 섹션 계층 조회
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
