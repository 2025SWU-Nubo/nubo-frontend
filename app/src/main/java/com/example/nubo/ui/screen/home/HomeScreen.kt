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



@Composable
fun HomeScreen(
    padding: PaddingValues = PaddingValues(),
    onMoreClick: () -> Unit = {}
) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val cardList by homeViewModel.cards.observeAsState(emptyList())



    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { RecentBoardSection() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { RecommendedCardsSection(onMoreClick = onMoreClick) }
        item {  Column {
            Spacer(modifier = Modifier.height(10.dp)) // 위 여백
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Grey10)
            )
            Spacer(modifier = Modifier.height(18.dp)) // 아래 여백
        } }
        item { RecommendedVideosSection(cardList) }
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
//        LazyRow(
//            horizontalArrangement = Arrangement.spacedBy(12.dp),
//            contentPadding = PaddingValues(horizontal = 12.dp)
//        ) {
//            items(items) { item ->
//                BoardThumbnailCard(item)
//            }
//        }
    }
}

@Composable
fun RecommendedCardsSection( onMoreClick: () -> Unit = {}) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "추천 학습 카드", style = AppTextStyles.title_semibold_24)
            Row( // 텍스트, 아이콘 묶기
                modifier = Modifier.clickable { onMoreClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "더보기",
                    style = AppTextStyles.label_semibold_14,
                    color = GreyMain100
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "더보기",
                    tint = GreyMain100
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        repeat(2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(
                        width = 1.dp,
                        color =  Grey30,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "봉골레 파스타 레시피",
                            style = AppTextStyles.b1_semibold_18
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "1. 팬에 올리브유와 다진 양파, 마늘, 크러쉬드 페퍼를 넣고 볶아주세요...",
                            style = AppTextStyles.b3_regular_14,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "상세 보기",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedVideosSection(cards: List<CardResponse>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "미시청/추천 영상", style = AppTextStyles.title_semibold_24)
    }
    Spacer(modifier = Modifier.height(12.dp))
    CardContent(cards)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    NuboAppTheme {
        HomeScreen()
    }
}
