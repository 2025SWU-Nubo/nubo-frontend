package com.example.nubo.data.dto

import com.google.gson.annotations.SerializedName

data class CardItemDto(
    val id: Int,
    val title: String?,
    val category: String?,
    val description: String?,
    @SerializedName("videoThumbnailUrl") val imageUrl: String
)

