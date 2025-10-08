package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

//보드 검색 應答
data class BoardSearchItemResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("source") val source: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("sectionCount") val sectionCount: Int,
    @SerializedName("cardCount") val cardCount: Int,
    @SerializedName("videoThumbnailUrl") val videoThumbnailUrl: String?,
    @SerializedName("shared") val shared: Boolean,
    @SerializedName("favorite") val favorite: Boolean
)
