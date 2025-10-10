package com.example.nubo.ui.component

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
    // 선택 관련 파라미터
    isSelectionMode: Boolean,
    selectedCardIds: Set<Int>
) {

    // Masonry 구성 동일
    val leftItems = cards.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = cards.filterIndexed { i, _ -> i % 2 != 0 }

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
            leftItems.forEachIndexed(){ index, item ->
                // 여기서 각 카드가 선택되었는지 여부를 계산
                val isSelected = selectedCardIds.contains(item.id)
                MyMasonryCard(
                    height = cardHeights.getOrNull(index * 2) ?: 180.dp,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    // 계산된 값을 파라미터로 전달
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rightItems.forEachIndexed(){ index, item ->
                // 여기서 각 카드가 선택되었는지 여부를 계산
                val isSelected = selectedCardIds.contains(item.id)
                MyMasonryCard(
                    height = cardHeights.getOrNull(index * 2) ?: 180.dp,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) },
                    // 계산된 값을 파라미터로 전달
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected
                )
            }
        }
    }
}


@Composable
fun MyMasonryCard(
    height: Dp,
    imageUrl: String,
    onClick: () -> Unit,
    // [추가] 선택 관련 파라미터
    isSelectionMode: Boolean,
    isSelected: Boolean)
{
    Box(
        modifier = Modifier
            .width(182.dp)
            .height(height)
            .clip(RoundedCornerShape(12.dp)) // 확대된 이미지를 잘라내는 역할
            .background(Grey50)
            .clickable { onClick() },
        contentAlignment = Alignment.Center

    ) {
        // 높이가 300dp일 때만 이미지를 1.2배 확대하는 Modifier 적용
        val imageModifier = if (height == 300.dp) {
            Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = 1.2f, // 가로로 1.2배 확대
                    scaleY = 1.2f  // 세로로 1.2배 확대
                )
        } else {
            Modifier.fillMaxSize()
        }

        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = imageModifier, // 위에서 만든 Modifier를 적용
            contentScale = ContentScale.Crop // Crop을 기본으로 두어 안정적인 크롭을 보장
        )
        // --- [추가] 선택 모드 오버레이 ---
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

