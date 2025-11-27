package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

data class RecommendCardResponse(
    @SerializedName("groups")
    val groups: List<GroupDto>
)

data class GroupDto(
    val groupId : Int,
    val groupType: String,
    val title: String,
    val keyword: String,
    val category:String,
    @SerializedName("cards")
    val cards: List<CardDto>
)


data class CardDto(
    val cardId: Int,
    val videoThumbnailUrl: String
)

