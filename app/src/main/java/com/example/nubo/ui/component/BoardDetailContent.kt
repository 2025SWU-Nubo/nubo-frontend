package com.example.nubo.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.nubo.ui.screen.myBoard.SelectionModeType
import formatIsoDateToDisplayLegacy
import kotlin.random.Random


@Composable
fun BoardDetailContent(
    modifier: Modifier = Modifier,
    boardItems: List<BoardItem>,
    cardItems: List<CardItem>,
    cardHeights: List<Dp>,
    onCardClick: (Int) -> Unit,
    onCardLongClick: ((Int) -> Unit)? = null,
    onSectionClick: (BoardItem) -> Unit,
    onSectionLongClick: ((BoardItem) -> Unit)? = null,
    onFavoriteClick: (BoardItem) -> Unit,
    // 선택 관련 상태 파라미터들
    isSelectionMode: Boolean,
    selectedSections: Set<Int>,
    selectedCards: Set<Int>,
    selectableSectionIds: Set<Int> = emptySet(),
    selectableCardIds: Set<Int> = emptySet(),

    isSharedBoard: Boolean,
    selectionModeType: SelectionModeType?
) {
    // Sections should be disabled when shared-board + CARD selection mode
    val forceDisableSections =
        isSharedBoard && isSelectionMode && selectionModeType == SelectionModeType.CARD

    //  should be disabled when shared-board + SECTION selection mode
    val forceDisableCards =
        isSharedBoard && isSelectionMode && selectionModeType == SelectionModeType.SECTION

    // 카드 Masonry 블록 패턴 생성
    val (leftItems, rightItems) = buildMasonryBlocks(cardItems)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // [1] 섹션 2열 그리드
        boardItems.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val section0Selectable =
                    !forceDisableSections && selectableSectionIds.contains(rowItems[0].id)

                // --- 첫 번째 아이템 ---
                // Column에 weight(1f)를 주어 50% 공간 차지
                Column(modifier = Modifier.weight(1f)) {
                    BoardCardWithText(
                        board = rowItems[0], // 첫 번째 아이템
                        onClick = { onSectionClick(rowItems[0]) },
                        onLongClick = onSectionLongClick?.let { { it(rowItems[0]) } },
                        onFavoriteClick = onFavoriteClick,
                        isSelectionMode = isSelectionMode,
                        // id로 선택 여부 확인
                        isSelected = selectedSections.contains(rowItems[0].id),
                        isSelectable = section0Selectable
                    )
                }

                // --- 두 번째 아이템 (또는 빈 공간) ---
                if (rowItems.size > 1) {
                    // 두 번째 아이템이 있으면, 동일하게 weight(1f)로 50% 공간 차지
                    Column(modifier = Modifier.weight(1f)) {
                        val section1Selectable =
                            !forceDisableSections && selectableSectionIds.contains(rowItems[1].id)

                        BoardCardWithText(
                            board = rowItems[1], // 두 번째 아이템
                            onClick = { onSectionClick(rowItems[1]) },
                            onLongClick = onSectionLongClick?.let { { it(rowItems[0]) } },
                            onFavoriteClick = onFavoriteClick,
                            isSelectionMode = isSelectionMode,
                            // id로 선택 여부 확인
                            isSelected = selectedSections.contains(rowItems[1].id),
                            isSelectable = section1Selectable
                        )
                    }
                } else {
                    // 아이템이 하나뿐이면, 오른쪽 절반을 빈 Spacer로 채움
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    // [2] 카드 Masonry
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            leftItems.forEach { (item, height) ->
                val cardSelectable = !forceDisableCards && selectableCardIds.contains(item.id)
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    onLongClick = onCardLongClick?.let { { it(item.id) } },
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedCards.contains(item.id),
                    isFavorite = item.isFavorite,
                    isSelectable = cardSelectable
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp) // 세로 4dp
        ) {
            rightItems.forEach { (item, height) ->
                val cardSelectable = !forceDisableCards && selectableCardIds.contains(item.id)
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    onLongClick = onCardLongClick?.let { { it(item.id) } },
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedCards.contains(item.id),
                    isFavorite = item.isFavorite,
                    isSelectable = cardSelectable
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(150.dp))
}

fun cardHeightForIndex(index: Int): Dp {
    // every 4th card → tall
    return if (index % 4 == 0) 300.dp else 148.dp
}
