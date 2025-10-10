package com.example.nubo.model.card

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.screen.myBoard.toCardItem

data class CardItem(
    val id: Int,
    val height: Dp,
    val title: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val isFavorite: Boolean
)

//fun CardItem.toDetailFallback(): CardDetailItem {
//    return CardDetailItem(
//        id = this.id,
//        imageUrl = this.imageUrl,
//        videoUrl = "",               // 상세 응답 오기 전이라 비움
//        title = this.title,
//        category = this.category,
//        boardSource = "",            // 알 수 없음 → 기본값
//        description = this.description,
//        date = "",                   // 알 수 없음 → 기본값
//        videoPlatform = "" ,          // 알 수 없음 → 기본값
//        tags =
//    )
//}

/*fun MyCardItem.toCardItem(): CardItem {
    return CardItem(
        id = this.id,
        height = 180.dp, // 기본값
        title = "제목 없음", // 기본값
        category = "카테고리 없음", // 기본값
        description = "설명 없음", // 기본값
        imageUrl = this.imageUrl,
        isFavorite = this.favorite?:false
    )
}*/
