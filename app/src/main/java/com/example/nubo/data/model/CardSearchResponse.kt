package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

data class CardSearchItemResponse(
    @SerializedName("cardId") val cardId: Int,
    @SerializedName("videoThumbnailUrl") val videoThumbnailUrl: String?,
    @SerializedName("viewed") val viewed: Boolean,
    @SerializedName("favorite") val favorite: Boolean
)
