package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.example.nubo.model.BoardItem
import com.example.nubo.model.CardItem
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.button_medium_12
import com.example.nubo.ui.theme.DefaultText
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain300

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
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // [1] 보드: 2열 그리드 (BoardContent 스타일)
        itemsIndexed(boardItems.chunked(2)) { _, rowItems ->
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
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
