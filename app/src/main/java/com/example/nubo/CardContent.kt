package com.example.nubo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain300
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun CardContent() {
    data class CardItem(val id: Int, val height: Dp)

    //  더미 데이터 + 셔플
    val allItems = List(20) { index ->
        val height = when (index % 4) {
            0 -> 210.dp
            1 -> 180.dp
            2 -> 260.dp
            else -> 200.dp
        }
        CardItem(id = index, height = height)
    }.shuffled()

    // 양쪽 열 나누기
    val leftItems = allItems.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = allItems.filterIndexed { i, _ -> i % 2 != 0 }

    // 스크롤 상태 공유
    val scrollState = rememberScrollState()

    // 단일 스크롤 가능한 내부 두 Column
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(10.dp)) // top padding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                leftItems.forEach { item ->
                    MasonryCard(height = item.height)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rightItems.forEach { item ->
                    MasonryCard(height = item.height)
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp)) // bottom padding
    }
}

@Composable
fun MasonryCard(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Grey50),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Image, contentDescription = null, tint = GreyMain300)
    }
}
