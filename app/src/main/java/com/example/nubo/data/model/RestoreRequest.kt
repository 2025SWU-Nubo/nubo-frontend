package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// 섹션(보드) 복구 요청
data class BoardRestoreRequest(
    @SerializedName("boardIds")
    val boardIds: List<Long>
)

// 섹션(보드) 복구 응답
data class BoardRestoreResponse(
    @SerializedName("restoredCount")
    val restoredCount: Int
)

// 카드 복구 요청
data class CardRestoreRequest(
    @SerializedName("cardIds")
    val cardIds: List<Long>
)

// 카드 복구 응답
data class CardRestoreResponse(
    @SerializedName("restoredCount")
    val restoredCount: Int
)
