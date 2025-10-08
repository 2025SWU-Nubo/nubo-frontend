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
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.card.CardItem
import com.example.nubo.model.myBoard.BoardItem
import formatIsoDateToDisplayLegacy
import kotlin.random.Random


@Composable
fun BoardDetailContent(
    modifier: Modifier = Modifier,
    boardItems: List<BoardItem>,
    cardItems: List<CardItem>,
    cardHeights: List<Dp>,
    onCardClick: (Int) -> Unit,
    onSectionClick: (BoardItem) -> Unit,
    onFavoriteClick: (BoardItem) -> Unit
) {
    // 카드 Masonry 좌/우 컬럼 분리
    val leftItems = cardItems.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = cardItems.filterIndexed { i, _ -> i % 2 != 0 }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(start = 16.dp, end=16.dp,  bottom = 20.dp)
    ) {
        // [1] 보드(섹션) 2열 그리드
        items(boardItems.chunked(2)) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    BoardCardWithText(
                        board = item,
                        onClick = { onSectionClick(item) },
                        onFavoriteClick = onFavoriteClick
                    )
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.width(190.dp))
                }
            }
        }

        // [2] 카드 Masonry
        item {
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
                    leftItems.forEachIndexed { index, item ->
                        MyMasonryCard(
                            height = cardHeights.getOrNull(index * 2) ?: 180.dp,
                            imageUrl = item.imageUrl,
                            onClick = { onCardClick(item.id) }
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rightItems.forEachIndexed { index, item ->
                        MyMasonryCard(
                            height = cardHeights.getOrNull(index * 2 + 1) ?: 180.dp,
                            imageUrl = item.imageUrl,
                            onClick = { onCardClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

// 필요 시 다른 화면에서도 쓰는 보조 함수
fun randomCardHeight(): Dp {
    val heights = listOf(130.dp, 180.dp, 300.dp)
    return heights[Random.nextInt(heights.size)]
}
