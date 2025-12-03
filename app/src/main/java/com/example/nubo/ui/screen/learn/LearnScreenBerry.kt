package com.example.nubo.ui.screen.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_medium_16
import com.example.nubo.ui.theme.AppTextStyles.b2_regular_16
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_medium_14
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.AppTextStyles.subtitle_semibold_20
import com.example.nubo.ui.theme.AppTextStyles.title_semibold_24
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.Purple200
import com.example.nubo.ui.theme.PurpleMain500

// 데이터 클래스: 화면에 표시할 베리 아이템 정보
data class BerryItem(
    val id: Int,
    val name: String,
    val imageRes: Int,
    val isUnlocked: Boolean
)

// 모은 베리 전체 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreenBerry(
    berryCount: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    // 오늘의 날씨 섹션 아이템 리스트
    val todayWeatherItems = listOf(
        BerryItem(
            id = 1,
            name = "함박눈",
            imageRes = R.drawable.learn_berry_snow_item,
            isUnlocked = true
        ),
        BerryItem(
            id = 2,
            name = "이슬비 톡톡",
            imageRes = R.drawable.learn_berry_lock_item,
            isUnlocked = false
        ),
        BerryItem(
            id = 3,
            name = "흐릿흐릿",
            imageRes = R.drawable.learn_berry_lock_item,
            isUnlocked = false
        ),
        BerryItem(
            id = 4,
            name = "가을 바람",
            imageRes = R.drawable.learn_berry_lock_item,
            isUnlocked = false
        ),
        BerryItem(
            id = 5,
            name = "모래언덕",
            imageRes = R.drawable.learn_berry_lock_item,
            isUnlocked = false
        ),
        BerryItem(
            id = 6,
            name = "베리가 내려와",
            imageRes = R.drawable.learn_berry_lock_item,
            isUnlocked = false
        )
    )

    // 구름의 감정 섹션 아이템 리스트
    val cloudEmotionItems = listOf(
        BerryItem(7, "신나요", R.drawable.learn_berry_lock_item, false),
        BerryItem(8, "궁금해요", R.drawable.learn_berry_lock_item, false),
        BerryItem(9, "찡긋", R.drawable.learn_berry_lock_item, false),
        BerryItem(10, "졸려요", R.drawable.learn_berry_lock_item, false),
        BerryItem(11, "축하해요", R.drawable.learn_berry_lock_item, false),
        BerryItem(12, "슬퍼요", R.drawable.learn_berry_lock_item, false)
    )

    // 상단 앱바 높이와 뒤로가기 영역 처리를 위해 Scaffold 사용
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BerryTopBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 상단 보라 헤더 + 베리 수집 현황
            item {
                BerryHeader(berryCount = berryCount)
            }

            // 성장 리워드 섹션 제목/설명
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "성장 리워드",
                        style = subtitle_semibold_20,
                        color = Grey1000,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "베리를 모으면 성장보드의 다양한 모습을 볼 수 있어요.",
                        style = b3_regular_14,
                        color = Grey500,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                        color = Color(0xFFD0D0D0) // 예시 hex 컬러
                    )
                }
            }

            // 오늘의 날씨 섹션
            item {
                SectionTitle(
                    title = "오늘의 날씨",
                    topPadding = 20.dp,
                    bottomPadding = 24.dp
                )
            }

            item {
                BerryGridSection(
                    items = todayWeatherItems,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }

            // 구름의 감정 섹션
            item {
                SectionTitle(
                    title = "구름의 감정",
                    topPadding = 0.dp,
                    bottomPadding = 24.dp
                )
            }

            item {
                BerryGridSection(
                    items = cloudEmotionItems,
                    modifier = Modifier
                        .padding(bottom = 36.dp)
                )
            }
        }
    }
}

// 상단 앱바 영역 (뒤로가기 + 가운데 타이틀)
@Composable
private fun BerryTopBar(
    onBackClick: () -> Unit
) {

    Surface(
        shadowElevation = 0.dp,
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top=12.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // 뒤로가기 버튼 (좌측 정렬)
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "뒤로가기",
                    tint = Grey1000,
                    modifier = Modifier
                        .size(24.dp),

                )
            }

            // 가운데 타이틀
            Text(
                text = "모은 베리",
                style = subtitle_semibold_20,
                color = Grey1000,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// 상단 보라 헤더 + 가운데 베리 아이콘 / 개수
@Composable
private fun BerryHeader(
    berryCount: Int
) {
    // 화면 가로에 맞춰 헤더 넓이 설정
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // 보라색 곡선 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.learn_berry_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 중앙 내용
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 동그란 베리 아이콘 배지
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.learn_berry_berry),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "베리 수집 현황",
                style = b2_semibold_16,
                color = Purple200
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${berryCount}개",
                style = title_semibold_24,
                color = Color.White
            )
        }
    }
}

// 섹션 제목 공통 컴포저블
@Composable
private fun SectionTitle(
    title: String,
    topPadding: Dp,
    bottomPadding: Dp
) {
    Text(
        text = title,
        style = b1_semibold_18,
        color = Grey1000,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = topPadding, bottom = bottomPadding)
    )
}

// 그리드 형태로 베리 아이템들을 보여주는 섹션
@Composable
private fun BerryGridSection(
    items: List<BerryItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp) // 전체 그리드 좌우 여백 고정
    ) {
        val itemSpacing = 12.dp // 아이템 간 간격 (원하는 대로 조절 가능)

        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing) // 균등 간격 설정
            ) {
                rowItems.forEach { item ->
                    BerryGridItem(
                        item = item,
                        modifier = Modifier
                            .weight(1f) // 각 아이템 폭 동일
                    )
                }

                // 마지막 행이 3개 미만일 때 빈칸 채우기
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

// 실제 베리 아이콘 + 이름 하나를 표시하는 컴포저블
@Composable
private fun BerryGridItem(
    item: BerryItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 베리 아이콘
        Image(
            painter = painterResource(id = item.imageRes),
            contentDescription = item.name,
            modifier = Modifier
                .size(105.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = item.name,
            style = b2_medium_16,
            color = if (item.isUnlocked) Grey1000 else GreyMain100,
            textAlign = TextAlign.Center
        )
    }
}
