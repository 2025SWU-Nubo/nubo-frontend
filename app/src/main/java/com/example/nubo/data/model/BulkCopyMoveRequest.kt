package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// 복제 요청 Body
data class BulkCopyRequest(
    @SerializedName("targetBoardId") val targetBoardId: Long,
    @SerializedName("boardIds") val boardIds: List<Long>?,
    @SerializedName("cardIds") val cardIds: List<Long>?
)
// 복제 응답 Body (필요 시 사용)
data class BulkCopyResponse(
    @SerializedName("targetBoardId") val targetBoardId: Long,
    @SerializedName("boardIds") val boardIds: List<Long>,
    @SerializedName("cardIds") val cardIds: List<Long>
)
// 이동 요청 Body
data class BulkMoveRequest(
    @SerializedName("targetBoardId") val targetBoardId: Long,
    @SerializedName("boardIds") val boardIds: List<Long>?,
    @SerializedName("cardIds") val cardIds: List<Long>?
)

// 이동 응답 Body
data class BulkMoveResponse(
    @SerializedName("targetBoardId") val targetBoardId: Long,
    @SerializedName("boardIds") val boardIds: List<Long>,
    @SerializedName("cardIds") val cardIds: List<Long>
)

