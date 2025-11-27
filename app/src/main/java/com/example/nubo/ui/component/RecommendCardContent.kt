package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.nubo.R
import com.example.nubo.data.model.CardResponse
import com.example.nubo.data.model.RecentBoardResponse
import com.example.nubo.data.model.RecommendCardResponse
import com.example.nubo.model.card.CardItem
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.GreyMain300
import kotlin.collections.lastIndex

@Composable
fun RecommendCardContent (
    cardId: Int,
    videoThumbnailUrl: String,
    onClick: (Int) -> Unit){
    val cardShape = RoundedCornerShape(4.dp)
    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 180.dp)
            .clickable(enabled = cardId > 0) { onClick(cardId) },
        shape = cardShape, // 카드 모양
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ){
        AsyncImage(
            model = videoThumbnailUrl,
            contentDescription = "보드 썸네일",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}
