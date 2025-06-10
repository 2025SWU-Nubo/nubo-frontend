package com.example.nubo.model.card

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nubo.model.myBoard.MyCardItem

data class CardItem(
    val id: Int,
    val height: Dp,
    val title: String,
    val category: String,
    val description: String,
    val imageUrl: String
)


fun CardItem.toShortformItem(): CardDetailDialogItem {
    return CardDetailDialogItem(
        id = this.id,
        imageUrl = this.imageUrl,
        title = this.title,
        category = this.category,
        description = this.description,
        date = "", // 예시
        videoPlatform = "YouTube",
        videoUrl = "",
        boardSource = ""
    )
}

fun MyCardItem.toCardItem(): CardItem {
    return CardItem(
        id = this.id,
        height = 180.dp, // 기본값
        title = "제목 없음", // 기본값
        category = "카테고리 없음", // 기본값
        description = "설명 없음", // 기본값
        imageUrl = this.imageUrl
    )
}
