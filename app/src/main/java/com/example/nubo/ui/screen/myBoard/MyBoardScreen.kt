package com.example.nubo.ui.screen.myBoard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.example.nubo.ui.theme.Purple200
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import com.example.nubo.R
import com.example.nubo.ui.component.BoardContent
import com.example.nubo.ui.component.CardContent
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.Grey200
import androidx.navigation.NavController
import com.example.nubo.ui.theme.AppTextStyles


@Composable
fun MyBoardScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(1) } //카드 탭 상태 기억

    Column(modifier = Modifier.fillMaxSize()) {
        TabHeader(
            selectedTabIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        TitleBar(selectedTab = selectedTab)
        FilterButtons()

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ScrollableCardContent()
                1 -> BoardContent(
                    onCardClick = { cardId ->
                        navController.navigate("board_detail/$cardId")
                    }
                )
            }
        }
    }
}


@Composable
fun TabHeader(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("영상", "보드")

    Column(modifier = Modifier.padding(top = 40.dp)) {
        // 탭 전체 중앙 정렬
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(25.dp), // 탭 간 간격
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = index == selectedTabIndex
                    val textColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onTabSelected(index) } // 탭 클릭 처리
                    ) {
                        // 텍스트 설정
                        Text(
                            text = title,
                            style = b1_semibold_18,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(14.dp)) // 텍스트-인디케이터  간격

                        // 인디케이터 설정 (선택여부에 따라 색상 다르게)
                        Box(
                            modifier = Modifier
                                .width(83.dp)
                                .height(1.5.dp)
                                .background(textColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TitleBar(selectedTab: Int) {
    val titleText = if (selectedTab == 0) "나의 영상" else "나의 보드"

    Column(modifier = Modifier.padding(top = 27.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = titleText,
                style = AppTextStyles.headline_regular_26,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "검색 아이콘",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
fun FilterButtons() {
    val filters = listOf("최근 저장순", "즐겨찾기", "공유됨")
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
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
                        style = AppTextStyles.label_medium_12,
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

                        "공유됨" -> {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                }
            }
        }
    }
}


// 카드 콘텐츠 영역 스크롤 가능하도록
@Composable
fun ScrollableCardContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 카드 두 열을 하나의 Row로 넣어줌
        item {
            CardContent()
        }
    }
}
