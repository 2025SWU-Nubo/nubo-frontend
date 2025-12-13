package com.example.nubo.ui.screen.card


import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.RecommendCardDetailResponse
import com.example.nubo.model.card.CardDetailItem
import formatIsoDateToDisplayLegacy

// 일반 카드 상세 → UI model
fun CardDetailResponse.toUi(): CardDetailItem {
    return CardDetailItem(
        cardId = cardId,
        videoThumbnailUrl = videoThumbnailUrl,
        videoUrl = videoUrl,
        title = title,
        boardName = aiCategoryName,
        summary = summary,
        videoPlatform = videoPlatform,
        createdAt = formatIsoDateToDisplayLegacy(createdAt),
        updatedAt = formatIsoDateToDisplayLegacy(updatedAt),
        tags = tags,
        isFavorite = isFavorite,
        stage = stage,
        berryGained = berryGained,
        stageUp = stageUp,
        mine = mine
    )
}

