package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// 섹션 정보
data class SectionDto(
    val id: Long,
    val name: String,
    val source: String,
    val updatedAt: String,
    val sectionCount: Int,
    val cardCount: Int,
    val shared: Boolean,
    val favorite: Boolean,
    @SerializedName(value = "videoThumbnailUrl", alternate = ["thumbnailUrl"])
    val thumbnailUrl: String?
)

// 카드 아이템 (상세의 cards.content 요소)
data class CardItemDto(
    @SerializedName(value = "cardId", alternate = ["id"])
    val id: Int,
    @SerializedName("videoThumbnailUrl")
    val imageUrl: String?,
    val viewed: Boolean? = null,
    val favorite: Boolean? = null,
    val title: String? = null,
    val category: String? = null,
    val description: String? = null,
    val mine: Boolean? = null
)

// 페이징 래퍼
data class PagedDto<T>(
    val content: List<T>,
    val last: Boolean,
    val totalElements: Int,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val numberOfElements: Int,
    val empty: Boolean
)

// 보드 상세 응답
data class BoardResponse(
    val id: Int,
    val name: String,
    val sections: List<SectionDto> = emptyList(),
    val cards: PagedDto<CardItemDto>,
    val shared: Boolean = false,
    val favorite: Boolean = false,
    val source: String? = null,
    val sectionCount: Int? = null,
    val cardCount: Int? = null,
    val updatedAt: String? = null,
    @SerializedName("videoThumbnailUrl")
    val videoThumbnailUrl: String? = null,
    val owner: Boolean = false
)
