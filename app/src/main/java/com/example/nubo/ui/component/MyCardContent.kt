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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.screen.myBoard.MyCardViewModel
import com.example.nubo.ui.theme.Grey50
import formatIsoDateToDisplayLegacy



@Composable
fun MyCardContent(
    cards: List<MyCardItem>,
    selectedCardId: Int?,
    cardHeights: List<Dp>,
    onCardClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    myCardViewModel: MyCardViewModel = hiltViewModel()
) {
    val cardDetail = myCardViewModel.cardDetail.value
    val isDetailLoading = myCardViewModel.isDetailLoading.value

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
            rightItems.forEachIndexed(){ index, item ->
                MyMasonryCard(
                    height = cardHeights.getOrNull(index * 2 + 1) ?: 180.dp,
                    imageUrl = item.imageUrl,
                    onClick = { onCardClick(item.id) }
                )
            }
        }
    }

    // 상세 다이얼로그
    selectedCardId?.let {
        if (isDetailLoading) {
            // Optional: LoadingDialog()
        }

        cardDetail?.let { detail ->
            val detailItem = CardDetailItem(
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
fun MyMasonryCard(height: Dp, imageUrl: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(182.dp)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Grey50)
            .clickable { onClick() },
        contentAlignment = Alignment.Center

    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
