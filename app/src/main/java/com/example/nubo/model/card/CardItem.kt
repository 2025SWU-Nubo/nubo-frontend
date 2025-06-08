package com.example.nubo.model.card

import androidx.compose.ui.unit.Dp

data class CardItem(
    val id: Int,
    val height: Dp,
    val title: String,
    val category: String,
    val description: String,
    val imageUrl: String
)


fun CardItem.toShortformItem(): ShortformItem {
    return ShortformItem(
        id = this.id,
        imageUrl = this.imageUrl,
        title = this.title,
        category = this.category,
        description = this.description,
        date = "2025-06-05", // 예시
        platform = "YouTube"  // 예시
    )
}
