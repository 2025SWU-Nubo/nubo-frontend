package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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


@Composable
fun CardContent() {
    data class CardItem(val id: Int, val height: Dp)

    //  더미 데이터 + 셔플
    val allItems = List(20) { index ->
        val height = when (index % 4) {
            0 -> 300.dp
            1 -> 130.dp
//            2 -> 230.dp
            else -> 180.dp
        }
        CardItem(id = index, height = height)
    }.shuffled()

    // 양쪽 열 나누기
    val leftItems = allItems.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = allItems.filterIndexed { i, _ -> i % 2 != 0 }

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
                MasonryCard(height = item.height)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rightItems.forEach { item ->
                MasonryCard(height = item.height)
            }
        }
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
