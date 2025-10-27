package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// 섹션 / 보드삭제 요청
data class BoardDeleteRequest(
    @SerializedName("boardIds")
    val boardIds: List<Long>,
    @SerializedName("deletelinkedCards")
    val deleteLinkedCards: String // "DELETE_ORPHANS" 또는 "DETACH_ONLY"
)

// 보드 삭제 응답 카드 배열
data class CardRestoreInfo(
    @SerializedName("cardIds")
    val cardIds: List<Long>,

    @SerializedName("boardId")
    val boardId: Long
)

//보드 삭제 응답 메인 데이터 클래스
data class BoardDeleteResponse(
    @SerializedName("boardId")
    val boardId: Long,

    @SerializedName("status")
    val status: String,

    @SerializedName("option")
    val option: String,

    @SerializedName("linksDetached")
    val linksDetached: Int,

    @SerializedName("cardsSoftDeleted")
    val cardsSoftDeleted: Int,

    @SerializedName("sectionsDeleted")
    val sectionsDeleted: Int,

    // 추가된 필드: 'null'일 수 있으므로 Nullable(String?)
    @SerializedName("error")
    val error: String?,

    @SerializedName("deletedSectionIds")
    val deletedSectionIds: List<Long>,

    // 변경된 필드: 'deletedCardIds' -> 'cardRestores'
    @SerializedName("cardRestores")
    val cardRestores: List<CardRestoreInfo>
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
