package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

data class CardItemDto(
    val id: Int,
    @SerializedName("videoThumbnailUrl") val imageUrl: String
)
