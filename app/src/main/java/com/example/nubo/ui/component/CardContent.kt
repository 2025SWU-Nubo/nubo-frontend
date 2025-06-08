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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.nubo.data.model.CardResponse
import com.example.nubo.model.card.CardItem
import com.example.nubo.model.card.toShortformItem
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain300


@Composable
fun CardContent(cards: List<CardResponse>) {

    // CardResponse를 CardItem으로 변환
    val allItems = cards.mapIndexed { index, card ->
        val height = when (index % 4) {
            0 -> 300.dp
            1 -> 130.dp
            else -> 180.dp
        }
        CardItem(
            id = card.id,
            height = height,
            title = "Card ${card.id}",
            category = "카테고리", // 서버 응답에 추가
            description = "서버에서 가져온 카드입니다.",
            imageUrl = card.videoThumbnailUrl
        )
    }.shuffled()

    // 양쪽 열 나누기
    val leftItems = allItems.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = allItems.filterIndexed { i, _ -> i % 2 != 0 }

    // 선택된 아이템 상태관리
    var selectedItem by remember { mutableStateOf<CardItem?>(null) }

    // Masonry 형태로 두 열 배치
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leftItems.forEach { item ->
                MasonryCard(item = item) {
                    selectedItem = item
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rightItems.forEach { item ->
                MasonryCard(item = item) {
                    selectedItem = item
                }
            }
        }
    }
    selectedItem?.let { item ->
        DetailCardDialog(
            item = item.toShortformItem(), // CardItem → ShortformItem 변환
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
fun MasonryCard(item: CardItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Grey50)
            .clickable { onClick() }, // 클릭 시 상세 다이얼로그 호출
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}
