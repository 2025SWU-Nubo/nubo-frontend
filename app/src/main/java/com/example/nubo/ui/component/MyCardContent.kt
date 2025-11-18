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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.nubo.R
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.screen.myBoard.MyCardViewModel
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.PurpleMain500
import formatIsoDateToDisplayLegacy



@Composable
fun MyCardContent(
    cards: List<MyCardItem>,
    cardHeights: List<Dp>,
    onCardClick: (Int) -> Unit,
    onCardLongClick: (Int) -> Unit,
    // 선택 관련 파라미터
    isSelectionMode: Boolean,
    selectedCardIds: Set<Int>
) {
    // 1. CardContent와 동일한 높이 로직 적용 (index % 4)
    // 리스트를 분리하기 전에 높이 정보를 매핑합니다.
    val cardsWithHeight = cards.mapIndexed { index, item ->
        val height = if (index % 4 == 0) 300.dp else 148.dp
        item to height
    }

    // 2. 좌우 컬럼 분리
    val leftItems = cardsWithHeight.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = cardsWithHeight.filterIndexed { i, _ -> i % 2 != 0 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), // 가로 여백 16dp
        horizontalArrangement = Arrangement.spacedBy(4.dp) // 가운데 사이 여백 4dp
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp) // 위아래 여백 4dp
        ) {
            leftItems.forEach { (item, height) ->
                val isSelected = selectedCardIds.contains(item.id)
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    onLongClick = { onCardLongClick(item.id) },
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    isFavorite = item.isFavorite
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp) // 위아래 여백 4dp
        ) {
            rightItems.forEach { (item, height) ->
                val isSelected = selectedCardIds.contains(item.id)
                MyMasonryCard(
                    height = height,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    onLongClick = { onCardLongClick(item.id) },
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    isFavorite = item.isFavorite
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
    isFavorite: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp)) // CardContent와 동일하게 6dp로 변경 (기존 8dp)
            .background(Grey50)
            .combinedClickable(
                onClick = onClick,
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
            Icon(
                painter = painterResource(id = R.drawable.card_favorite_new),
                contentDescription = "즐겨찾기",
                tint = Color.Unspecified, // 아이콘 원본 색상 사용
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
            )
        }

        // --- 선택 모드 오버레이 ---
        if (isSelectionMode && isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.5f))
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_board_selected), // 체크 아이콘
                contentDescription = "선택됨",
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}
