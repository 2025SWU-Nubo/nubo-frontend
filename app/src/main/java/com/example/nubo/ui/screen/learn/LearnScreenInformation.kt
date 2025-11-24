package com.example.nubo.ui.screen.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles.b2_regular_16
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.AppTextStyles.label_semibold_14
import com.example.nubo.ui.theme.AppTextStyles.title_semibold_24
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun LearnScreenInformation(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,      // 마지막 시작하기 또는 닫기 버튼 눌렀을 때 호출
) {
    // 현재 페이지 인덱스 상태 0 ~ 3
    var pageIndex by remember { mutableIntStateOf(0) }
    val totalPages = 4

    // 스와이프 판정 기준 거리(dp → px)
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 60.dp.toPx() }

    // 드래그 누적 거리
    var dragX by remember { mutableFloatStateOf(0f) }


    // 전체 화면을 덮는 어두운 배경과 팝업 카드 레이아웃
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)), // 뒤 배경 어둡게
        // 팝업을 화면 중앙보다 약간 위쪽에 배치
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                // 좌우 여백 24dp 확보 후 가로는 꽉 차게
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                // 화면 상단에서 조금 띄워서 배치
                .padding(top = 72.dp)
                .height(580.dp)
                // 카드 전체에서 좌우 스와이프 제스처 처리
                .pointerInput(pageIndex) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            // 다른 제스처로 전달되지 않도록 소비
                            change.consume()
                            dragX += dragAmount
                        },
                        onDragEnd = {
                            when {
                                // 오른쪽으로 밀었을 때 → 다음 페이지
                                // 마지막 페이지에서는 동작하지 않도록 조건
                                dragX > swipeThresholdPx && pageIndex >0 -> {
                                    pageIndex -= 1
                                }
                                // 왼쪽으로 밀었을 때 → 이전 페이지
                                // 0번 페이지에서는 동작하지 않도록 조건
                                dragX < -swipeThresholdPx && pageIndex < totalPages - 1 -> {
                                    pageIndex += 1
                                }
                            }
                            dragX = 0f
                        },
                        onDragCancel = {
                            dragX = 0f
                        }
                    )
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (pageIndex == 0) Color.White else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            when (pageIndex) {
                // 1번 화면: 배경 전체 이미지
                0 -> LearnInfoPageFirst(
                    onNext = { pageIndex = 1 },
                    onClose = onClose
                )
                // 나머지 3개 화면
                1 -> LearnInfoContentPage(
                    pageIndex = 1,
                    title = "식물 키우는 법",
                    description = "카드 열람 1회마다 물방울이 1개 만들어집니다\n물방울 5개면 다음 단계로 성장해요",
                    imageRes = R.drawable.learn_popup_bg2,
                    buttonText = "다음",
                    currentPage = pageIndex,
                    totalPages = totalPages,
                    onNext = { pageIndex = 2 },
                    onClose = onClose
                )

                2 -> LearnInfoContentPage(
                    pageIndex = 2,
                    title = "성장 단계",
                    description = "식물은 총 5단계로 성장하며\n마지막 단계에서 베리를 수확할 수 있어요",
                    imageRes = R.drawable.learn_popup_bg3,
                    buttonText = "다음",
                    currentPage = pageIndex,
                    totalPages = totalPages,
                    onNext = { pageIndex = 3 },
                    onClose = onClose
                )

                3 -> LearnInfoContentPage(
                    pageIndex = 3,
                    title = "베리 리워드",
                    description = "베리로 성장보드의 새로운 모습을 얻을 수 있어요\n접속할 때마다 달라지는 보드를 만나보세요",
                    imageRes = R.drawable.learn_popup_bg4,
                    buttonText = "시작하기",
                    currentPage = pageIndex,
                    totalPages = totalPages,
                    onNext = onClose,
                    onClose = onClose
                )
            }
        }
    }
}

// 성장보드 첫번째 인포 페이지
@Composable
private fun LearnInfoPageFirst(
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    // 카드 전체를 배경 이미지로 채움
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 배경 이미지를 카드 전체에 꽉 채워서 표시
        // 이미지가 카드 영역 전체를 채우도록 설정
        Image(
            painter = painterResource(id = R.drawable.learn_popup_bg1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),

            // [수정 포인트] 가로에 억지로 맞추지 않고, '중앙'을 기준으로 배치
            alignment = Alignment.Center,

            // 옵션 1: 화면을 꽉 채우되, 중앙을 기준으로 자르기 (가장 추천)
            contentScale = ContentScale.Crop

            // 옵션 2: 만약 이미지가 잘리는 게 싫고 세로 전체를 다 보여주고 싶다면 아래 사용
            // contentScale = ContentScale.FillHeight
        )

        // 내용 오버레이
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {

            Spacer(modifier = Modifier.height(4.dp))
            // 상단 닫기 버튼
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Grey1000
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier.padding(start = 3.dp),
                text = "성장보드",
                style = title_semibold_24,
                color = Grey1000
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier.padding(start = 3.dp),
                text = "숏폼을 볼 때마다 식물이 자라는",
                style = b2_semibold_16,
                color = Grey1000
            )

            Text(
                modifier = Modifier.padding(start = 3.dp),
                text = "당신의 성장을 보여주는 공간입니다",
                style = b2_regular_16,
                color = Grey1000
            )
            // 나머지 장과 여백 맞추기
            Spacer(modifier = Modifier.weight(1f))

            // 페이지 인디케이터
            LearnInfoIndicator(
                currentPage = 0,
                totalPages = 4,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

// 성장보드 나머지 인포 페이지
@Composable
private fun LearnInfoContentPage(
    pageIndex: Int,
    title: String,
    description: String,
    imageRes: Int,
    buttonText: String,
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // 상단 닫기 버튼
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(24.dp)
                .padding(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = Grey1000
            )
        }

        Spacer(modifier = Modifier.height(84.dp))

        Text(
            modifier = Modifier.padding(start = 3.dp),
            text = title,
            style = title_semibold_24,
            color = Grey1000
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(start = 3.dp),
            text = description,
            style = b3_regular_14,
            color = Grey500,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(46.dp))

        // 중앙 이미지 영역: 가로는 카드 너비에 맞추고, 세로는 원본 비율 유지
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),      // 가로만 부모에 맞춤
                contentScale = ContentScale.FillWidth    // 비율 유지 + 가로 기준으로 맞추기
            )
        }


        Spacer(modifier = Modifier.height(14.dp))

        // 페이지 인디케이터
        LearnInfoIndicator(
            currentPage = currentPage,
            totalPages = totalPages,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(69.dp))

        // 하단 버튼
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(41.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (pageIndex == 3) PurpleMain500 else Purple100,
                contentColor = if (pageIndex == 3) Color.White else PurpleMain500
            )
        ) {
            Text(
                text = buttonText,
                style = label_semibold_14
            )
        }

        Spacer(modifier = Modifier.height(5.dp))
    }
}

@Composable
private fun LearnInfoIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    // 하단 작은 점 네 개
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF4C6FFF)
                        else Color(0xFFCDD2E0)
                    )
            )
        }
    }
}

