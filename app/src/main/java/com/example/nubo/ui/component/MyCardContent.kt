package com.example.nubo.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.nubo.model.MyCardItem
import com.example.nubo.ui.theme.Grey50


@Composable
fun MyCardContent(cards: List<MyCardItem>) {
    // 양쪽 열 나누기
    val leftItems = cards.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = cards.filterIndexed { i, _ -> i % 2 != 0 }

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
                MyMasonryCard(
                    height = randomCardHeight(item.id),
                    imageUrl = item.imageUrl
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rightItems.forEach { item ->
                MyMasonryCard(
                    height = randomCardHeight(item.id),
                    imageUrl = item.imageUrl
                )
            }
        }
    }
}

@Composable
fun MyMasonryCard(height: Dp, imageUrl: String) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Grey50),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

