package com.example.nubo

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MyBoardScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TabHeader()
        TitleBar()
        FilterButtons()
        CardContent()
    }
}

@Composable
fun TabHeader() {
    val tabs = listOf("영상", "보드")
    var selectedTabIndex = 1 // 보드 탭 고정

    androidx.compose.material3.TabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = { tabPositions ->
            androidx.compose.material3.TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = androidx.compose.ui.graphics.Color(0xFF6C4EFF)
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            androidx.compose.material3.Tab(
                selected = selectedTabIndex == index,
                onClick = { /* No-op: 탭 이동은 다른 곳에서 처리 */ },
                text = { androidx.compose.material3.Text(title) }
            )
        }
    }
}

@Composable
fun TitleBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.Text(
            text = "나의 보드",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
        )

        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Search,
            contentDescription = "검색 아이콘",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun FilterButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(32.dp)
                    .background(androidx.compose.ui.graphics.Color.LightGray)
            )
        }
    }
}

