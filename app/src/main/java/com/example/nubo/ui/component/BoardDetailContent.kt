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
import com.example.nubo.model.card.CardDetailDialogItem
import com.example.nubo.model.card.CardItem
import com.example.nubo.model.card.toShortformItem
import com.example.nubo.model.myBoard.BoardItem
import formatIsoDateToDisplayLegacy
import kotlin.random.Random


@Composable
fun BoardDetailContent(
    boardItems: List<BoardItem>,
    cardItems: List<CardItem>,
    cardHeights: List<Dp>,
    selectedCardId: Int?,
    cardDetail: CardDetailResponse?,
    isDetailLoading: Boolean,
    onCardClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 카드 Masonry
    val leftItems = cardItems.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = cardItems.filterIndexed { i, _ -> i % 2 != 0 }


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
                        onClick = { /*Todo*/ }
                    )
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.width(190.dp))
                }
            }
        }

        // [2] 카드: Masonry
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

    // 상세 다이얼로그 (LazyColumn 밖에서 처리)
    selectedCardId?.let {
        if (isDetailLoading) {
            // Optional: LoadingDialog()
        }

        cardDetail?.let { detail ->
            val detailItem = CardDetailDialogItem(
                id = detail.id,
                imageUrl = detail.videoThumbnailUrl ?: "",
                videoUrl = detail.videoUrl ?: "",
                title = detail.title ?: "제목 없음",
                category = detail.boardName ?: "카테고리 없음",
                boardSource = detail.boardSource ?: "",
                description = detail.summary ?: "설명 없음",
                date = formatIsoDateToDisplayLegacy(detail.createdAt),
                videoPlatform = detail.videoPlatform ?: "알 수 없음"
            )
            DetailCardDialog(
                item = detailItem,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun TwoColumnCardMasonry(
    cardItems: List<CardItem>,
    selectedItem: CardItem?,
    onCardClick: (CardItem?) -> Unit
) {
    val left = cardItems.filterIndexed { i, _ -> i % 2 == 0 }
    val right = cardItems.filterIndexed { i, _ -> i % 2 != 0 }


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
            left.forEach { item ->
                val height = randomCardHeight()
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
                val height = randomCardHeight()
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item) }
                )
            }

        }
    }
}

fun randomCardHeight(): Dp {
    val heights = listOf(130.dp, 180.dp, 300.dp)
    return heights[Random.nextInt(heights.size)]
}
