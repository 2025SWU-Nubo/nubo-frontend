package com.example.nubo.ui.screen.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles.b2_regular_16
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.AppTextStyles.label_semibold_14
import com.example.nubo.ui.theme.AppTextStyles.title_semibold_22
import com.example.nubo.ui.theme.AppTextStyles.subtitle_semibold_20
import com.example.nubo.ui.theme.Grey0
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple700
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun LearnScreenInformation(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,      // 마지막 시작하기 또는 닫기 버튼 눌렀을 때 호출
) {
    val totalPages = 4

    // Pager 상태 (현재 페이지 / 애니메이션 스크롤 등에 사용)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { totalPages }
    )

    // 버튼 클릭 시 부드럽게 페이지 이동시키기 위한 코루틴 스코프
    val scope = rememberCoroutineScope()

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
                .padding(top = 140.dp)
                .height(500.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            // 페이지를 좌우로 넘기는 영역
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    // 0번 화면: 배경 전체 이미지
                    0 -> LearnInfoPageFirst(
                        onClose = onClose,
                        onNext = {
                            // 버튼 클릭 시 부드럽게 다음 페이지로 이동
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )

                    // 나머지 3개 화면
                    1 -> LearnInfoContentPage(
                        pageIndex = 1,
                        title = "식물 키우는 법",
                        description = "카드 열람 1회마다 물방울이 1개 만들어집니다\n물방울 5개면 다음 단계로 성장해요",
                        imageRes = R.drawable.learn_popup_bg2,
                        onNext = {
                            // 버튼 클릭 시 부드럽게 다음 페이지로 이동
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        onClose = onClose,
                        onPrev = {
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )

                    2 -> LearnInfoContentPage(
                        pageIndex = 2,
                        title = "성장 단계",
                        description = "식물은 총 5단계로 성장하며\n마지막 단계에서 베리를 수확할 수 있어요",
                        imageRes = R.drawable.learn_popup_bg3,
                        onNext = {
                            scope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        onClose = onClose,
                        onPrev = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )

                    3 -> LearnInfoContentPage(
                        pageIndex = 3,
                        title = "베리 리워드",
                        description = "베리로 성장보드의 새로운 모습을 얻을 수 있어요\n접속할 때마다 달라지는 보드를 만나보세요",
                        imageRes = R.drawable.learn_popup_bg4,
                        onNext = {
                            // 마지막 페이지에서는 시작하기 → 인포 닫기
                            onClose()
                        },
                        onClose = onClose,
                        onPrev = {
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                }
            }
        }
    }
}

// 성장보드 첫번째 인포 페이지
@Composable
private fun LearnInfoPageFirst(
    onClose: () -> Unit,
    onNext: () -> Unit
) {
    // 카드 전체를 배경 이미지로 채움
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 배경 이미지를 카드 전체에 꽉 채워서 표시
        // 이미지가 카드 영역 전체를 채우도록 설정
        Image(
            painter = painterResource(id = R.drawable.learn_popup_bg_1),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()              // 가로를 부모 너비에 딱 맞춤
                .align(Alignment.Center), // 가운데 정렬
            alignment = Alignment.BottomCenter, // 잘릴 때 기준이 되는 위치
            contentScale = ContentScale.FillWidth // 가로 기준으로 맞추고 세로는 넘치는 부분만 잘림
        )

        // 내용 오버레이
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 닫기 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            }

            Spacer(modifier = Modifier.height(235.dp))

            // 타이틀 텍스트
            Text(
                text = "성장보드",
                style = title_semibold_22,
                color = Grey1000
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "숏폼을 볼 때마다 식물이 자라는",
                style = b2_semibold_16,
                color = Grey1000
            )

            Text(
                text = "당신의 성장을 보여주는 공간입니다",
                style = b2_regular_16,
                color = Grey1000
            )
            // 나머지 장과 여백 맞추기
            Spacer(modifier = Modifier.weight(1f))

           /* // 페이지 인디케이터
            LearnInfoIndicator(
                currentPage = 0,
                totalPages = 4,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )*/

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(41.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .paint(
                        painter = painterResource(R.drawable.learn_popup_btn),
                        contentScale = ContentScale.FillBounds
                    )
                    .noRippleClickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Text("다음", style = label_semibold_14, color = Color.White)
            }


            Spacer(modifier = Modifier.height(8.dp))
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
    onNext: () -> Unit,
    onClose: () -> Unit,
    onPrev: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 닫기 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        }

        // 닫기 버튼과 카드 이미지 사이 간격
        Spacer(modifier = Modifier.height(18.dp))

        // 상단 이미지 카드 영역
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),      // 가로를 카드에 맞춤
                contentScale = ContentScale.FillWidth    // 비율 유지 + 가로 기준으로 맞추기
            )
        }

        // 이미지와 타이틀 사이 간격 (시안 24)
        Spacer(modifier = Modifier.height(24.dp))

        //타이틀
        Text(
            text = title,
            style = subtitle_semibold_20,
            color = Grey1000
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = b3_regular_14,
            color = Grey500,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,      // 가운데 정렬
            modifier = Modifier.fillMaxWidth() // 줄 기준 폭을 넓게 잡아주면 더 확실
        )

        // 아래 영역을 채워서 인디케이터/버튼을 하단 쪽에 배치
        Spacer(modifier = Modifier.weight(1f))

        /*// 페이지 인디케이터
        LearnInfoIndicator(
            currentPage = currentPage,
            totalPages = totalPages,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )*/

        // 하단 버튼
        if (pageIndex !=3) {
            // 두 번째 페이지부터는 이전/다음 두 개 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 이전 버튼
                Button(
                    onClick = { onPrev() },
                    modifier = Modifier
                        .weight(1f)
                        .height(41.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7),
                        contentColor = GreyMain300
                    )
                ) {
                    Text("이전", style = label_semibold_14)
                }

                // 다음 버튼
                Button(
                    onClick = { onNext() },
                    modifier = Modifier
                        .weight(1f)
                        .height(41.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple100,
                        contentColor = Purple700
                    )
                ) {
                    Text("다음", style = label_semibold_14)
                }
            }
        } else {
            // 마지막 페이지(시작하기)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 이전 버튼
                Button(
                    onClick = { onPrev() },
                    modifier = Modifier
                        .weight(1f)
                        .height(41.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7),
                        contentColor = GreyMain300
                    )
                ) {
                    Text("이전", style = label_semibold_14)
                }

                // 다음 버튼
                Button(
                    onClick = { onNext() },
                    modifier = Modifier
                        .weight(1f)
                        .height(41.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleMain500,
                        contentColor = Grey0
                    )
                ) {
                    Text("완료", style = label_semibold_14)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
