package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.nubo.R
import com.example.nubo.model.card.CardDetailDialogItem
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun DetailCardFront(
    item: CardDetailDialogItem,
    onDismiss: () -> Unit,
    onFlip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 흰색 배경 (테두리 역할)
            .clip(RoundedCornerShape(16.dp))
            .padding(4.dp) // 8dp 테두리 두께
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp)) // 내부 카드에 둥근 모서리 (선택사항)
        ) {
            // 배경 이미지 (카드 배경 꽉 차게)
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )


            // 상단 아이콘 버튼 Row
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 닫기 버튼
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                ) {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_icon),
                            contentDescription = "닫기",
                            tint = Color.Black
                        )
                    }
                }

                // Flip 버튼
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                ) {
                    IconButton(onClick = { onFlip() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.cached_icon),
                            contentDescription = "Flip",
                            tint = Color.Black
                        )
                    }
                }

            }

            // 3️⃣ 하단 내용
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,  //상단 투명
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.7f), // 중간 부분
                                Color.Black.copy(alpha = 0.9f), // 하단 진한 부분
                            )
                        )
                    )
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 3-1️⃣ 한줄 제목
                Text(
                    text = item.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 3-2️⃣ 카테고리, 저장날짜, 저장플랫폼 Row (수정된 부분)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp), // Row 높이 고정
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically // 이 부분 추가
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally // 텍스트 좌측 정렬
                    ) {
                        Text(
                            text = item.category,
                            color = Color.White,
                            style = AppTextStyles.label_medium_12
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.boardSource} 카테고리",
                            color = Color.White,
                            style = AppTextStyles.caption_regular_9
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally // 텍스트 중앙 정렬
                    ) {
                        Text(
                            text = item.date,
                            color = Color.White,
                            style = AppTextStyles.label_medium_12
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "저장 날짜",
                            color = Color.White,
                            style = AppTextStyles.caption_regular_9
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally // 텍스트 우측 정렬
                    ) {

                        // 플랫폼 로고 이미지
                        Icon(
                            painter = painterResource(id = getPlatformLogo(item.videoPlatform)),
                            contentDescription = item.videoPlatform,
                            tint = Color.Unspecified, // 원본 색상 유지
                            modifier = Modifier
                                .size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "저장 플랫폼",
                            color = Color.White,
                            style = AppTextStyles.caption_regular_9
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3-3️⃣ 버튼 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    // 왼쪽 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        Text(
                            text = "영상 보러가기",
                            style = AppTextStyles.label_semibold_14,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 오른쪽 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurpleMain500)
                    ) {
                        Text(
                            text = "학습공간에 저장하기",
                            style = AppTextStyles.label_semibold_14,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun getPlatformLogo(platform: String): Int {
    return when (platform.lowercase()) {
        "youtube" -> R.drawable.youtube_logo_wh
//        "instagram" -> R.drawable.instagram_logo
//        "tiktok" -> R.drawable.tiktok_logo
//        "facebook" -> R.drawable.facebook_logo
//        "twitter" -> R.drawable.twitter_logo
//        "linkedin" -> R.drawable.linkedin_logo
        else -> R.drawable.nubo_logo // 기본 로고
    }
}

