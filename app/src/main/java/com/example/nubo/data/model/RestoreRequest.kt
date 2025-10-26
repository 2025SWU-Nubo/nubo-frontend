package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// 섹션(보드) 복구 요청
data class BoardRestoreRequest(
    @SerializedName("boardIds")
    val boardIds: List<Long>,

    @SerializedName("sectionIds")
    val sectionIds: List<Long>,

    @SerializedName("cardRestores")
    val cardRestores: List<CardRestoreInfo>
)

// 보드 / 섹션 복구 응답
data class BoardRestoreResponse(
    @SerializedName("restoredCount")
    val restoredCount: Int,

    @SerializedName("restoredBoardIds")
    val restoredBoardIds: List<Long>,

    @SerializedName("restoredSectionIds")
    val restoredSectionIds: List<Long>,

    @SerializedName("restoredCardIds")
    val restoredCardIds: List<Long>
)

// 카드 복구 요청
data class CardRestoreRequest(
    val cardIds: List<Long>,
    val boardId: Long?,
    val deleteMode: String
)

// 카드 복구 응답
data class CardRestoreResponse(
    @SerializedName("restoredCount")
    val restoredCount: Int
)
