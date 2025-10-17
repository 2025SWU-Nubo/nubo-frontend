package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// 섹션 / 보드삭제 요청
data class BoardDeleteRequest(
    @SerializedName("boardIds")
    val boardIds: List<Long>,
    @SerializedName("deletelinkedCards")
    val deleteLinkedCards: String // "DELETE_ORPHANS" 또는 "DETACH_ONLY"
)

// 보드 삭제 응답
data class BoardDeleteResponse(
    val boardId: Long,
    val status: String,
    val option: String,
    val linksDetached: Int,
    val cardsSoftDeleted: Int,
    val sectionsDeleted: Int,
    val deletedSectionIds: List<Long>,
    val deletedCardIds: List<Long>
)

// 카드 삭제/제거 요청
data class CardDeleteRequest(
    @SerializedName("cardIds")
    val cardIds: List<Long>,
    @SerializedName("deleteMode")
    val deleteMode: String // "SOFT_DELETE" 또는 "DETACH_ONLY"
)

// 카드 삭제/제거 응답
data class CardDeleteResponse(
    @SerializedName("results")
    val results: List<CardDeleteResult>
)

data class CardDeleteResult(
    @SerializedName("cardId")
    val cardId: Long,
    @SerializedName("status")
    val status: String // "DELETED", "DETACHED" 등
)
