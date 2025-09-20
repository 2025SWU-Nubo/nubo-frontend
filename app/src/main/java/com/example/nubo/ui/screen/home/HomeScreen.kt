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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
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
import com.example.nubo.ui.theme.PurpleMain500
import formatIsoDateToDisplayLegacy


// com/example/nubo/ui/screen/home/HomeScreen.kt
@Composable
fun HomeScreen(
    padding: PaddingValues = PaddingValues(),
    onMoreClick: () -> Unit = {},
    onOpenCardDetail:(Int) -> Unit,
    onLogoClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
) {
    val vm: HomeViewModel = hiltViewModel()

    val cards by vm.cards.observeAsState(emptyList())
    val chips by vm.chips.observeAsState(emptyList())
    val selectedChipId by vm.selectedChipId.observeAsState("all")

    LaunchedEffect(Unit) {
        vm.loadBoards()
        vm.refreshForCurrentSelection()
    }

    Scaffold(
        topBar = {
            CustomTopBar(
                onLogoClick = onLogoClick,
                onNotificationsClick = onNotificationsClick
                )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        val mergedPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + padding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + padding.calculateBottomPadding(),
            start = padding.calculateStartPadding(LayoutDirection.Ltr),
            end = padding.calculateEndPadding(LayoutDirection.Ltr)
        )


        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White),
            contentPadding = mergedPadding
        ) {
            item { Spacer(Modifier.height(12.dp)) }
            item { RecentBoardSection() }
            item {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Grey10)
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
            item {
                RecommendedVideosSection(
                    cards = cards,
                    chips = chips,
                    selectedChipId = selectedChipId,
                    onChipClick = { vm.onChipClick(it) },
                    onCardClick = { item ->
                        onOpenCardDetail(item.id)
                    }
                )
            }
        }


    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
    onLogoClick: (() -> Unit)?,
    onNotificationsClick: (() -> Unit)?
){

    CenterAlignedTopAppBar(
        modifier = Modifier.padding(horizontal = 12.dp),
        windowInsets = WindowInsets(0),
        // If you prefer title centered text, set `title = { Text("Nubo") }`
        navigationIcon = {
            // Left logo (clickable)
            IconButton(
                onClick = { onLogoClick?.invoke() },
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.nubo_logo),
                    contentDescription = "앱 로고",
                    tint = PurpleMain500

                )
            }
        },
        title = { /* keep empty or place brand text */ },
        actions = {
            IconButton(onClick = { onNotificationsClick?.invoke() }) {
                // You can use painterResource or Material Icons
                Icon(
                    painter = painterResource(R.drawable.alarm),
                    contentDescription = "알림"
                )

            }
        },
    )
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
        Spacer(modifier = Modifier.height(14.dp))
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
    Spacer(modifier = Modifier.height(16.dp))
    //칩 컨포넌트
    RecommendationChipsRow(
        chips = chips,
        onChipClick = onChipClick
    )

    Spacer(modifier = Modifier.height(12.dp))

    Column(modifier = Modifier.padding(horizontal = 16.dp))
    {     CardContent(cards = cards, onCardClick = onCardClick) }

}
