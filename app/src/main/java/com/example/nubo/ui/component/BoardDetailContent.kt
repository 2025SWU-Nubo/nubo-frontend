package com.example.nubo.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nubo.model.card.CardItem
import com.example.nubo.model.card.toShortformItem
import com.example.nubo.model.myBoard.BoardItem


@Composable
fun BoardDetailContent(
    boardItems: List<BoardItem>,
    cardItems: List<CardItem>,
    onBoardClick: (Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf<CardItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // [1] 보드: 2열 그리드
        items(boardItems.chunked(2)) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    BoardCardWithText(
                        board = item,
                        onClick = { onBoardClick(item.id) }
                    )
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.width(190.dp))
                }
            }
        }

        // [2] 카드: Masonry 그대로
        item {
            TwoColumnCardMasonry(
                cardItems = cardItems,
                selectedItem = selectedItem,
                onCardClick = { selectedItem = it }
            )
        }
    }
}

@Composable
fun TwoColumnCardMasonry(
    cardItems: List<CardItem>,
    selectedItem: CardItem?,
    onCardClick: (CardItem?) -> Unit
)
{
    val left = cardItems.filterIndexed { i, _ -> i % 2 == 0 }
    val right = cardItems.filterIndexed { i, _ -> i % 2 != 0 }


    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            left.forEach { item ->
                val height = randomCardHeight(item.id)
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item) }
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            right.forEach { item ->
                val height = randomCardHeight(item.id)
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item) }
                )
            }

        }
    }
    selectedItem?.let { item ->
        DetailCardDialog(
            item = item.toShortformItem(), // 변환 함수 필요 시 작성
            onDismiss = { onCardClick(null) }
        )
    }
}

fun randomCardHeight(id: Int): Dp {
    val heights = listOf(130.dp, 180.dp, 230.dp)
    return heights[id % heights.size]
}
