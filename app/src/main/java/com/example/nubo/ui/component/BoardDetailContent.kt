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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nubo.model.BoardItem
import com.example.nubo.model.CardItem

// 콘텐츠 타입
sealed class MixedItem {
    data class Board(val data: BoardItem) : MixedItem()
    data class Card(val data: CardItem) : MixedItem()
}

@Composable
fun BoardDetailContent(
    boardItems: List<BoardItem>,
    cardItems: List<CardItem>,
    onBoardClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        }

        item {
            TwoColumnCardMasonry(cardItems)
        }
    }
}

@Composable
fun TwoColumnBoardGrid(
    boardItems: List<BoardItem>,
    onBoardClick: (Int) -> Unit
) {
    val leftItems = boardItems.filterIndexed { index, _ -> index % 2 == 0 }
    val rightItems = boardItems.filterIndexed { index, _ -> index % 2 != 0 }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in leftItems.indices) {
                BoardCardWithText(board = leftItems[i], onClick = { onBoardClick(leftItems[i].id) })

                if (i < rightItems.size) {
                    BoardCardWithText(board = rightItems[i], onClick = { onBoardClick(rightItems[i].id) })
                } else {
                    Spacer(modifier = Modifier.width(180.dp)) // 우측 빈칸
                }
            }
        }
    }
}

@Composable
fun TwoColumnCardMasonry(cardItems: List<CardItem>) {
    val left = cardItems.filterIndexed { i, _ -> i % 2 == 0 }
    val right = cardItems.filterIndexed { i, _ -> i % 2 != 0 }

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            left.forEach { MasonryCard(it.height) }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            right.forEach { MasonryCard(it.height) }
        }
    }
}
