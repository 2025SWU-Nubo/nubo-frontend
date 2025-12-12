package com.example.nubo.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.nubo.R
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey50


@Composable
fun MyCardContent(
    cards: List<MyCardItem>,
    cardHeights: List<Dp>,
    onCardClick: (Int) -> Unit,
    onCardLongClick: (Int) -> Unit,
    // 선택 관련 파라미터
    isSelectionMode: Boolean,
    selectedCardIds: Set<Int>,
    selectableCardIds: Set<Int>? = null,
    forceDisableAll: Boolean = false
) {
    // 블록 패턴 카드
    val (leftItems, rightItems) = buildMasonryBlocks(cards)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), // 좌우 16dp
        horizontalArrangement = Arrangement.spacedBy(4.dp) // 가운데 4dp
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp) // 세로 4dp
        ) {
            leftItems.forEach { (item, height) ->
                val isSelected = selectedCardIds.contains(item.id)
                // selectableCardIds가 null이면 true
                val canSelectByPermission = selectableCardIds?.contains(item.id) ?: true
                // 모드 분리 때문에 전체 강제 disable 가능
                val isSelectable = canSelectByPermission && !forceDisableAll
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    onLongClick = { onCardLongClick(item.id) },
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    isFavorite = item.isFavorite,
                    isSelectable = isSelectable
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rightItems.forEach { (item, height) ->
                val isSelected = selectedCardIds.contains(item.id)
                val canSelectByPermission = selectableCardIds?.contains(item.id) ?: true
                val isSelectable = canSelectByPermission && !forceDisableAll
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    onLongClick = { onCardLongClick(item.id) },
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    isFavorite = item.isFavorite,
                    isSelectable = isSelectable
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyMasonryCard(
    height: Dp,
    imageUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    // 선택 관련 파라미터
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isFavorite: Boolean,
    isSelectable: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp)) // CardContent와 동일하게 6dp로 변경 (기존 8dp)
            .background(Grey50)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 높이가 300dp일 때만 이미지를 1.2배 확대하는 Modifier 적용
        val imageModifier = if (height == 300.dp) {
            Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = 1.2f, // 가로로 1.2배 확대
                    scaleY = 1.2f,  // 세로로 1.2배 확대
                )
        } else {
            Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = 2.6f,
                    scaleY = 2.6f,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
                )
        }

        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = imageModifier, // 위에서 만든 Modifier를 적용
            contentScale = ContentScale.Crop // Crop을 기본으로 두어 안정적인 크롭을 보장
        )

        //---- 즐겨찾기 추가 ---
        if (isFavorite) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_card_favorite),
                    contentDescription = "즐겨찾기",
                    tint = Color.Unspecified, // 아이콘 원본 색상 사용
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(bottom = 1.dp)
                )
            }
        }

        // --- 선택 모드 오버레이 ---
        if (isSelectionMode) {
            when {
                // 선택 불가 (mine=false)
                !isSelectable -> {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                }

                // 선택됨
                isSelected -> {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Grey1000.copy(alpha = 0.4f))
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_board_selected),
                        contentDescription = "선택됨",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }

                // 선택되지 않음
                else -> {
                    Icon(
                        painter = painterResource(id = R.drawable.board_unselect),
                        contentDescription = "선택되지 않음",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}

// 카드 3개 블록 패턴
fun <T> buildMasonryBlocks(
    items: List<T>,
    bigHeight: Dp = 300.dp,
    smallHeight: Dp = 148.dp
): Pair<List<Pair<T, Dp>>, List<Pair<T, Dp>>> {

    val left = mutableListOf<Pair<T, Dp>>()
    val right = mutableListOf<Pair<T, Dp>>()

    var index = 0
    var isFirstBlock = true

    // 가득 찬 블록 (3 cards per block)
    while (items.size - index >= 3) {
        if (isFirstBlock) {
            // Block 1: Left big (1) + Right small (2)
            left += items[index] to bigHeight
            right += items[index + 1] to smallHeight
            right += items[index + 2] to smallHeight
        } else {
            // Block 2: Left small (2) + Right big (1)
            left += items[index] to smallHeight
            left += items[index + 1] to smallHeight
            right += items[index + 2] to bigHeight
        }
        index += 3
        isFirstBlock = !isFirstBlock
    }

    // 남은 카드가 1개 또는 2개 일 때
    val remaining = items.size - index
    when (remaining) {
        1 -> {
            // One small card on the left
            left += items[index] to smallHeight
        }

        2 -> {
            // One small card on each column
            left += items[index] to smallHeight
            right += items[index + 1] to smallHeight
        }
    }

    return left to right
}
