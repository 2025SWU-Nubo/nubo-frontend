package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// 섹션(보드) 복구 요청
data class BoardRestoreRequest(
    val boardIds: List<Long>,
    val sectionIds: List<Long>,
    val cardRestore: CardRestoreRequest?
)

// 보드 / 섹션 복구 응답
data class BoardRestoreResponse(
    val restoredCount: Int,
    val restoredBoardIds: List<Long>,
    val restoredSectionIds: List<Long>,
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
