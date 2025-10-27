package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.nubo.data.model.CardResponse
import com.example.nubo.model.card.CardItem
import com.example.nubo.ui.theme.Grey50


@Composable
fun CardContent(cards: List<CardResponse>,
                onCardClick: (CardItem) -> Unit) {

    // 1) 서버 리스트 -> Masonry용 UI 아이템으로 변환
    val allItems = cards.mapIndexed { index, card ->
        val height = when (index % 4) {
            0 -> 300.dp
            1 -> 130.dp
            else -> 180.dp
        }
        CardItem(
            id = card.cardId,
            height = height,
            title = "Card ${card.cardId}",
            category = "카테고리", // 서버 응답에 추가
            description = "서버에서 가져온 카드입니다.",
            imageUrl = card.videoThumbnailUrl,
            isFavorite = card.favorite
        )
    }.shuffled()

    // 2) Masonry 좌/우 열 분리
    val leftItems = allItems.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = allItems.filterIndexed { i, _ -> i % 2 != 0 }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leftItems.forEach { item ->
                MasonryCard(item = item) {
                    onCardClick(item)
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rightItems.forEach { item ->
                MasonryCard(item = item) {
                    onCardClick(item)
                }
            }
        }
    }
}


@Composable
fun MasonryCard(item: CardItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(item.height)
            .clip(RoundedCornerShape(8.dp))
            .background(Grey50)
            .clickable { onClick() }, // 클릭 시 전면 상세 스크린 오픈
        contentAlignment = Alignment.Center
    ) {

        // 높이가 300dp일 때만 이미지를 1.2배 확대하는 Modifier 적용
        val imageModifier = if (item.height == 300.dp) { // 'item.height'로 조건 변경
            Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = 1.2f, // 가로로 1.2배 확대
                    scaleY = 1.2f,  // 세로로 1.2배 확대
                )
        } else{
            Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = 2.6f,
                    scaleY = 2.6f,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
                )
        }

        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
                .fillMaxSize()
        )
    }
}
