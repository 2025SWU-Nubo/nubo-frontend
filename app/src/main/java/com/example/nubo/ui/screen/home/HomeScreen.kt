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
import androidx.compose.foundation.layout.statusBars
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
import com.example.nubo.data.model.GroupDto
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.model.RecommendCardResponse
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.card.CardItem
import com.example.nubo.model.home.RecommendChipItem
import com.example.nubo.ui.component.RecommendCardContent
import com.example.nubo.ui.component.RecommendationChipsRow
import com.example.nubo.ui.screen.card.CardDetailScreen
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import formatIsoDateToDisplayLegacy
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Purple50


// com/example/nubo/ui/screen/home/HomeScreen.kt
@Composable
fun HomeScreen(
    padding: PaddingValues = PaddingValues(),
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenCardDetail:(Int) -> Unit,
    onOpenRecommendCard:(Int) -> Unit,
    onLogoClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onOpenBoard: (Int, String) -> Unit = { _, _ -> }


) {
    val vm: HomeViewModel = hiltViewModel()

    val cards by vm.cards.observeAsState(emptyList())
    val chips by vm.chips.observeAsState(emptyList())
    val selectedChipIds by vm.selectedChipIds.observeAsState(setOf("all"))

    // 최근 본 보드
    val recentBoards by vm.recentBoards.observeAsState(emptyList())

    // 추천 그룹 상태
    val recommendGroups by vm.recommendGroups.observeAsState(emptyList())

    // 새로운 알림 여부
    val hasUnread by vm.hasUnread.collectAsState()

    LaunchedEffect(Unit) {
        vm.refreshAll()
        vm.loadRecommendationGroups()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var firstResumeConsumed by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        // observe ON_RESUME to refresh whenever returning to Home
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // avoid double call right after first composition
                if (firstResumeConsumed) {
                    vm.refreshForCurrentSelection()
                    vm.loadRecentBoards()
                    vm.loadRecommendationGroups()
                } else {
                    firstResumeConsumed = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CustomTopBar(
                hasUnread = hasUnread,
                onLogoClick = onLogoClick,
                onNotificationsClick = onNotificationsClick
                )
        },
    ) { innerPadding ->

        val mergedPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + padding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + padding.calculateBottomPadding(),
            start = padding.calculateStartPadding(LayoutDirection.Ltr),
            end = padding.calculateEndPadding(LayoutDirection.Ltr)
        )

        val bottomBarHeight = 90.dp
        val extraBottom = bottomBarHeight + 36.dp
        val finalContentPadding = PaddingValues(
            top = mergedPadding.calculateTopPadding(),
            bottom = mergedPadding.calculateBottomPadding() + extraBottom,
            start = mergedPadding.calculateStartPadding(LayoutDirection.Ltr),
            end = mergedPadding.calculateEndPadding(LayoutDirection.Ltr)
        )

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White),
            contentPadding = finalContentPadding
        ) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                RecentBoardSection(
                    items = recentBoards,
                    onBoardClick = onOpenBoard // pass through to parent
                )
            }
            item {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Grey10)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            items(recommendGroups) { group ->

                    RecommendVideoSection(
                        group = group,
                        onCardClick = { cardId -> onOpenRecommendCard(cardId) }
                    )

                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Grey10)
                    )
                    Spacer(Modifier.height(16.dp))


            }

            item {
                UnviewedVideosSection(
                    cards = cards,
                    chips = chips,
                    onChipClick = { vm.onChipClick(it) },
                    onCardClick = { item -> onOpenCardDetail(item.id) }
                )

            }
        }


    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
    hasUnread : Boolean,
    onLogoClick: (() -> Unit)?,
    onNotificationsClick: (() -> Unit)?
){

    CenterAlignedTopAppBar(
        windowInsets = WindowInsets.statusBars,
        navigationIcon = {
            //  누보 로고
            IconButton(
                onClick = {},
                modifier = Modifier.size(96.dp).padding(start = 16.dp),
                enabled = false,
            ) {
                Icon(
                    painter = painterResource(R.drawable.nubo_logo),
                    contentDescription = "앱 로고",
                    tint = PurpleMain500,


                )
            }
        },
        title = { /* keep empty or place brand text */ },
        actions = {
            IconButton(onClick = { onNotificationsClick?.invoke() }) {
                Icon(
                    painter =   if (hasUnread)painterResource(R.drawable.bell_noti) else painterResource(R.drawable.bell),
                    contentDescription = "알림",
                    modifier = Modifier.size(26.dp),
                    tint = Color.Unspecified

                )

            }
        },
    )
}


@Composable
fun RecentBoardSection(
    items: List<RecentBoardResponse>,
    onBoardClick: (Int, String) -> Unit
) {

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "최근 본 보드", style = AppTextStyles.b1_semibold_18)
        Spacer(modifier = Modifier.height(14.dp))

        if (items.isEmpty()){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ){
                Text(
                    text = "아직 최근 본 보드가 없어요!",
                    style = AppTextStyles.b3_regular_14,
                    color = GreyMain300
                )
            }
            return@Column
        }

        LazyRow {
            itemsIndexed(items) { index, item ->
                //맨 처음과 맨 끝 보드는 패딩값 0으로
                val leftPadding = if (index == 0) 2.dp else 8.dp
                val rightPadding = if (index == items.lastIndex) 2.dp else 0.dp

                Box(
                    modifier = Modifier
                        .padding(start = leftPadding, end = rightPadding)
                ) {
                    RecentBoardCard(
                        boardId = item.boardId,
                        boardName = item.boardName,
                        // If API uses a different field, replace here (e.g., item.thumbnail)
                        videoThumbnailUrl = item.videoThumbnailUrl,
                        onClick = onBoardClick
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentBoardCard(
    boardId: Int,
    boardName: String,
    videoThumbnailUrl: String?,
    onClick: (Int, String) -> Unit
) {
    val cardShape = RoundedCornerShape(8.dp)
    Card(
        modifier = Modifier
            .size(width = 120.dp, height = 102.dp)
            .clickable(enabled = boardId > 0) { onClick(boardId, boardName) },
        shape = cardShape, // 카드 모양
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Top image
        AsyncImage(
            model = videoThumbnailUrl ?: R.drawable.bg_profile_header, // fallback
            contentDescription = "보드 썸네일",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        )

        // Bottom title
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = boardName,
                style = AppTextStyles.label_medium_12,
                color = Color.Black,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun RecommendVideoSection(
    group: GroupDto,
    onCardClick: (Int) -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = group.title,
            style = AppTextStyles.b1_semibold_18
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (group.cards.isEmpty()) {
            // Empty state when group has no cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "누보가 열심히 찾는 중이에요!",
                    style = AppTextStyles.b3_regular_14,
                    color = GreyMain300,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow {
                itemsIndexed(group.cards) { index, card ->
                    val startPadding = if (index == 0) 2.dp else 4.dp
                    val endPadding = if (index == group.cards.lastIndex) 2.dp else 0.dp

                    Box(
                        modifier = Modifier.padding(start = startPadding, end = endPadding)
                    ) {
                        RecommendCardContent(
                            cardId = card.cardId,
                            videoThumbnailUrl = card.videoThumbnailUrl,
                            onClick = onCardClick
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun UnviewedVideosSection(
    cards: List<CardResponse>,
    chips: List<RecommendChipItem>,
    onChipClick: (RecommendChipItem) -> Unit,
    onCardClick: (CardItem) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "아직 안 본 영상", style = AppTextStyles.b1_semibold_18)
    }
    Spacer(modifier = Modifier.height(12.dp))
    //칩 컨포넌트
//    RecommendationChipsRow(
//        chips = chips,
//        onChipClick = onChipClick
//    )
//
//    Spacer(modifier = Modifier.height(10.dp))

    if (cards.isEmpty()) {
        // 카드가 없을 경우 칩도 안보이게
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(vertical = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_card),
                    contentDescription = "앱 로고",
                    tint = Grey200,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "안 본 영상이 없어요",
                    style = AppTextStyles.b3_regular_14,
                    color = GreyMain300
                )
            }
        }
    } else {
        //칩 컨포넌트
        RecommendationChipsRow(
            chips = chips,
            onChipClick = onChipClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 카드 컨텐츠
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            CardContent(cards = cards, onCardClick = onCardClick)
        }

        Spacer(Modifier.height(80.dp))
    }
}
