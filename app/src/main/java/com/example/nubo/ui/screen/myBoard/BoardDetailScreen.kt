package com.example.nubo.ui.screen.myBoard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nubo.R
import com.example.nubo.data.model.CardItemDto
import com.example.nubo.data.model.SectionDto
import com.example.nubo.model.myBoard.BoardItem
import com.example.nubo.ui.component.BoardDetailContent
import com.example.nubo.ui.theme.AppTextStyles.label_medium_12
import com.example.nubo.ui.theme.AppTextStyles.headline_regular_26
import com.example.nubo.ui.theme.AppTextStyles.subtitle_medium_16
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple200
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.model.card.CardItem
import com.example.nubo.ui.component.TwoColumnCardMasonry
import com.example.nubo.ui.component.randomCardHeight
import getDisplayDate
import java.net.URLDecoder


@Composable
fun BoardDetailScreen(
    boardId: String,
    boardTitle: String,
    navController: NavController,
    viewModel: BoardDetailViewModel = hiltViewModel()
) {

    var selectedItem by remember { mutableStateOf<CardItem?>(null) }

    LaunchedEffect(boardId) {
        viewModel.fetchBoardDetail(boardId)
    }


    val boardState by viewModel.board.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        //뒤로가기
        DetailTopBar(onBack = { navController.popBackStack() })

        //타이틀
        BoardTitleBar(title = boardTitle)

        // 필터 + 보라 버튼
        BoardFilterButton()

        // 데이터 있을 때만 출력
        if (boardState != null) {
            val boardItems = boardState?.sections?.map { it.toBoardItem() } ?: emptyList()
            val cardItems = boardState?.cards?.map { it.toCardItem() } ?: emptyList()

            if (boardItems.isNotEmpty()) {
                BoardDetailContent(
                    boardItems = boardItems,
                    cardItems = cardItems,
                    onBoardClick = { /* TODO */ }
                )
            } else {
                TwoColumnCardMasonry(
                    cardItems = cardItems,
                    selectedItem = selectedItem,
                    onCardClick = { selectedItem = it }
                )
            }
        }else {
            Text("Loading...")
        }
    }
}

@Composable
fun DetailTopBar(onBack: () -> Unit) {
    val titleText = "나의 보드"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back), // ← ← 아이콘 파일 필요
            contentDescription = "뒤로가기",
            tint = GreyMain300,
            modifier = Modifier
                .clickable { onBack() }
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = titleText,
            style = subtitle_medium_16,
            color = GreyMain300
        )
    }
}


@Composable
fun BoardTitleBar(title: String) {
    val decodedTitle = URLDecoder.decode(title, "utf-8")

    Column(modifier = Modifier.padding(top = 27.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 16.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = decodedTitle,
                style = headline_regular_26,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BoardFilterButton() {
    val filters = listOf("최근 저장순", "즐겨찾기")
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 기존 필터 버튼
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { label ->

                val isSelected = selected == label
                OutlinedButton(
                    onClick = { selected = if (isSelected) null else label },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Purple200 else Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Grey200),
                    modifier = Modifier.height(35.dp),
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = label,
                            style = label_medium_12,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        when (label) {
                            "최근 저장순" -> {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter_arrow_down),
                                    contentDescription = "정렬 옵션",
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            "즐겨찾기" -> {
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter_star),
                                    contentDescription = "즐겨찾기",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 오른쪽: 보라색 버튼 2개
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("+", "선택").forEach { label ->
                Button(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple100.copy(alpha = 0.3f),
                        contentColor = PurpleMain500
                    ),
                    border = BorderStroke(0.5.dp, PurpleMain500),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (label == "+") {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_filter_add),
                            contentDescription = "섹션 추가",
                            tint = PurpleMain500
                        )
                    } else {
                        Text(
                            text = label,
                            style = label_medium_12,
                            color = PurpleMain500
                        )
                    }
                }
            }
        }
    }
}

fun SectionDto.toBoardItem(): BoardItem {
    return BoardItem(
        id = this.id,
        serverBoardId = this.id,
        title = this.name,
        subtitle = "${this.cardCount} 카드",
        createdAt = getDisplayDate(this.updatedAt),
        isBookmarked = this.favorite,
        source = this.source,
        imageUrl = this.thumbnailUrl
    )
}


fun CardItemDto.toCardItem(): CardItem {
    return CardItem(
        id = this.id,
        height = randomCardHeight(), // 기존 randomCardHeight() 함수 사용
        title = this.title ?: "No Title", // 서버 데이터 없을 경우 기본값
        category = this.category ?: "No Category", // 마찬가지
        description = this.description ?: "No Description",
        imageUrl = this.imageUrl ?: ""
    )
}

