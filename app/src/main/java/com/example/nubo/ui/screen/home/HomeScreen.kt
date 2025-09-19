package com.example.nubo.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.nubo.model.home.BoardThumbnailCardItem
import com.example.nubo.ui.component.CardContent
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.NuboAppTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nubo.data.model.CardResponse
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.card.CardItem
import com.example.nubo.model.home.RecommendChipItem
import com.example.nubo.ui.component.RecommendationChipsRow
import com.example.nubo.ui.screen.card.CardDetailScreen
import formatIsoDateToDisplayLegacy


// com/example/nubo/ui/screen/home/HomeScreen.kt
@Composable
fun HomeScreen(
    padding: PaddingValues = PaddingValues(),
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier

) {
    val vm: HomeViewModel = hiltViewModel()

    val cards by vm.cards.observeAsState(emptyList())
    val chips by vm.chips.observeAsState(emptyList())
    val selectedChipId by vm.selectedChipId.observeAsState("all")

    // 카드 선택 상태 관리
    var selectedCardId by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastClickedItem by remember { mutableStateOf<CardItem?>(null) }
    val detail by vm.cardDetail.observeAsState()
    val isDetailLoading by vm.isDetailLoading.observeAsState(false)


    LaunchedEffect(Unit) {
        vm.loadBoards()
        vm.refreshForCurrentSelection()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                vm.loadBoards()
                vm.refreshForCurrentSelection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    when (selectedCardId) {
        null -> {
            // ─── 홈(목록) ───
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentPadding = PaddingValues(bottom = 60.dp)
            ) {
                item { Spacer(Modifier.height(12.dp)) }
                item { RecentBoardSection() }
                item {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Grey10)
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                }
                item {
                    RecommendedVideosSection(
                        cards = cards,
                        chips = chips,
                        selectedChipId = selectedChipId,
                        onChipClick = { vm.onChipClick(it) },
                        onCardClick = { item ->
                            lastClickedItem = item
                            selectedCardId = item.id
                            vm.getCardDetail(item.id)
                        }
                    )
                }
            }
        }
        else -> {
            // ─── 상세(전체 페이지) ───
            when {
                isDetailLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                detail != null -> {
                    // 매핑 함수 없이 여기서 바로 UI용 객체 생성
                    val item = CardDetailItem(
                        id = detail!!.id,
                        imageUrl = detail!!.videoThumbnailUrl ?: "",
                        videoUrl = detail!!.videoUrl ?: "",
                        title = detail!!.title ?: "제목 없음",
                        category = detail!!.boardName ?: "카테고리 없음",
                        boardSource = detail!!.boardSource ?: "",
                        description = detail!!.summary ?: "설명 없음",
                        date = formatIsoDateToDisplayLegacy(detail!!.createdAt),
                        videoPlatform = detail!!.videoPlatform ?: "알 수 없음"
                    )
                    CardDetailScreen(
                        item = item,
                        onBack = {
                            selectedCardId = null
                            vm.clearCardDetail()
                        }
                    )
                }
                else -> {
                    // Fallback: 마지막 클릭한 CardItem으로 상세 구성(제목/설명/카테고리 그대로 표시)
                    lastClickedItem?.let { item ->
                        val fallback = CardDetailItem(
                            id = item.id,
                            imageUrl = item.imageUrl,
                            videoUrl = "",
                            title = item.title,
                            category = item.category,
                            boardSource = "",
                            description = item.description,
                            date = "",
                            videoPlatform = ""
                        )
                        CardDetailScreen(
                            item = fallback,
                            onBack = {
                                selectedCardId = null
                                vm.clearCardDetail()
                            }
                        )
                    } ?: run {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun BoardThumbnailCard(item: BoardThumbnailCardItem) {
    Column(
        modifier = Modifier
            .size(width = 120.dp, height = 110.dp)
            .shadow(2.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        verticalArrangement = Arrangement.Top
    ) {
        // 이미지 상단 (80dp 높이)
        AsyncImage(
            model = item.imageResId,
            contentDescription = "썸네일",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        )

        // 텍스트 하단 (카테고리명)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = item.category,
                style = AppTextStyles.b2_medium_16,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun RecentBoardSection() {

    // 더미 데이터
    // 리스트 순서대로 인덱스 생성
    val items = listOf(
        BoardThumbnailCardItem("엔터테인먼트",imageResId = R.drawable.thumbnail_entertainment),
        BoardThumbnailCardItem("AI 및 개발", imageResId = R.drawable.thumbnail_it),
        BoardThumbnailCardItem("기초 디자인", imageResId = R.drawable.thumbnail_design),
        BoardThumbnailCardItem("요리 레시피", imageResId = R.drawable.thumbnail_recipe),
        BoardThumbnailCardItem("아이돌",imageResId = R.drawable.thumbnail_entertainment),
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "최근 본 보드", style = AppTextStyles.title_semibold_24)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow {
            itemsIndexed(items) { index, item ->
                //맨 처음과 맨 끝 보드는 패딩값 0으로
                val leftPadding = if (index == 0) 2.dp else 12.dp
                val rightPadding = if (index == items.lastIndex) 2.dp else 0.dp

                Box(
                    modifier = Modifier
                        .padding(start = leftPadding, end = rightPadding)
                ) {
                    BoardThumbnailCard(item)
                }
            }
        }
    }
}


@Composable
fun RecommendedVideosSection(
    cards: List<CardResponse>,
    chips: List<RecommendChipItem>,
    selectedChipId: String,
    onChipClick: (RecommendChipItem) -> Unit,
    onCardClick: (CardItem) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "추천 영상", style = AppTextStyles.title_semibold_24)
    }
    Spacer(modifier = Modifier.height(12.dp))
    //칩 컨포넌트
    RecommendationChipsRow(
        chips = chips,
        onChipClick = onChipClick
    )

    Spacer(modifier = Modifier.height(12.dp))

    Column(modifier = Modifier.padding(horizontal = 16.dp))
    {     CardContent(cards = cards, onCardClick = onCardClick) }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    NuboAppTheme {
        HomeScreen()
    }
}
