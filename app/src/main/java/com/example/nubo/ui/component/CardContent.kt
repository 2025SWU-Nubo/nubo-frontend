package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.nubo.data.model.CardResponse
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.card.CardItem
import com.example.nubo.model.card.toDetailFallback
import com.example.nubo.ui.screen.card.CardDetailScreen
import com.example.nubo.ui.screen.home.HomeViewModel
import com.example.nubo.ui.theme.Grey50
import formatIsoDateToDisplayLegacy


@Composable
fun CardContent(cards: List<CardResponse>,
                homeViewModel: HomeViewModel = hiltViewModel()) {

    // CardResponse를 CardItem으로 변환
    val allItems = cards.mapIndexed { index, card ->
        val height = when (index % 4) {
            0 -> 300.dp
            1 -> 130.dp
            else -> 180.dp
        }
        CardItem(
            id = card.cardId,
            height = height,
            title = "Card ${card.cardId}",
            category = "카테고리", // 서버 응답에 추가
            description = "서버에서 가져온 카드입니다.",
            imageUrl = card.videoThumbnailUrl
        )
    }.shuffled()

    // 양쪽 열 나누기
    val leftItems = allItems.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = allItems.filterIndexed { i, _ -> i % 2 != 0 }

    // 선택된 아이템 상태관리
    var selectedCardId by remember { mutableStateOf<Int?>(null) }

    // 카드 상세 정보 관찰
    val cardDetail by homeViewModel.cardDetail.observeAsState()
    val isDetailLoading by homeViewModel.isDetailLoading.observeAsState(false)

    // Masonry 형태로 두 열 배치
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
            leftItems.forEach { item ->
                MasonryCard(item = item) {
                    selectedCardId = item.id
                    homeViewModel.getCardDetail(item.id)
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rightItems.forEach { item ->
                MasonryCard(item = item) {
                    selectedCardId = item.id
                    homeViewModel.getCardDetail(item.id)
                }
            }
        }
    }

    // 카드 상세 표시
    selectedCardId?.let { cardId ->
        if (isDetailLoading) {
            CircularProgressIndicator()
        }

        cardDetail?.let { detail ->
            // 서버에서 받은 상세 정보로 CardDetailDialogItem 생성
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

//            DetailCardDialog(
//                item = detailItem,
//                onDismiss = {
//                    selectedCardId = null
//                    homeViewModel.clearCardDetail()
//                }
//            )

            CardDetailScreen(
                item = detailItem,
                onBack = {
                    // 뒤로가기: 선택 해제 + 상세 캐시 정리 → 목록으로 복귀
                    selectedCardId = null
                    homeViewModel.clearCardDetail()
                },
                onInfoClick = { /* TODO: 필요 시 가이드/설명 처리 */ }
            )
        } ?: run {
            // cardDetail이 아직 로드되지 않은 경우 기본 아이템으로 표시
            val selectedItem = allItems.find { it.id == cardId }
            selectedItem?.let { item ->
//                DetailCardDialog(
//                    item = item.toShortformItem(),
//                    onDismiss = {
//                        selectedCardId = null
//                        homeViewModel.clearCardDetail()
//                    }
//                )
                val fallbackDetail = item.toDetailFallback()
                CardDetailScreen(
                    item = fallbackDetail,
                    onBack = {
                        selectedCardId = null
                        homeViewModel.clearCardDetail()
                    }
                )
            }
        }
    }

}

@Composable
fun MasonryCard(item: CardItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(item.height)
            .clip(RoundedCornerShape(12.dp))
            .background(Grey50)
            .clickable { onClick() }, // 클릭 시 상세 다이얼로그 호출
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}
