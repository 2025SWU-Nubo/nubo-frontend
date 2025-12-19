package com.example.nubo.ui.screen.learn

// Compose Runtime & Coroutines
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// Compose UI Layout & Foundation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints // 새로 추가됨
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset // 새로 추가됨
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape

// Compose Material3 & Icons
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

// Compose UI Graphics & Utils
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity // 새로 추가됨
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Project Specific Resources
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
    onClose: () -> Unit,
) {
    val totalPages = 4
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    // 배경 어둡게
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 140.dp)
                .height(500.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            // [변경 1] 너비를 알기 위해 BoxWithConstraints 사용
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cardWidth = maxWidth // 카드의 전체 너비
                val density = LocalDensity.current

                // [변경 2] 고정 UI(버튼 등)의 위치(Offset) 계산
                val uiTranslationX by remember {
                    derivedStateOf {
                        val currentScrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction

                        // 1페이지 기준으로 거리 계산:
                        // 0페이지일 때: (1 - 0) = 1 (너비의 100% 만큼 오른쪽으로 밀어냄 -> 안보임)
                        // 0.5페이지일 때: (1 - 0.5) = 0.5 (너비의 50% 만큼 오른쪽 -> 따라 들어오는 효과)
                        // 1페이지 이상일 때: (1 - 1.x) = 음수 -> 0으로 고정 (제자리 고정)
                        val distanceFactor = (1f - currentScrollPosition).coerceAtLeast(0f)

                        // 픽셀 단위로 변환
                        with(density) { (distanceFactor * cardWidth.toPx()).toDp() }
                    }
                }

                // --- [1] 슬라이드되는 콘텐츠 영역 (이미지, 텍스트) ---
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        // 0번 페이지: 기존 완성된 UI
                        0 -> LearnInfoPageFirst(
                            onClose = onClose,
                            onNext = { scope.launch { pagerState.animateScrollToPage(1) } }
                        )

                        // 1, 2, 3번 페이지: 내용(Content)만 있는 컴포저블
                        1 -> LearnInfoInnerContent(
                            title = "식물 키우는 법",
                            description = "카드 열람 1회마다 물방울이 1개 만들어집니다\n물방울 5개면 다음 단계로 성장해요",
                            imageRes = R.drawable.learn_popup_bg2
                        )
                        2 -> LearnInfoInnerContent(
                            title = "성장 단계",
                            description = "식물은 총 5단계로 성장하며\n마지막 단계에서 베리를 수확할 수 있어요",
                            imageRes = R.drawable.learn_popup_bg3
                        )
                        3 -> LearnInfoInnerContent(
                            title = "베리 리워드",
                            description = "베리로 성장보드의 새로운 모습을 얻을 수 있어요\n접속할 때마다 달라지는 보드를 만나보세요",
                            imageRes = R.drawable.learn_popup_bg4
                        )
                    }
                }

                // --- [2] 1번 페이지부터 고정될 UI 그룹 (닫기 버튼 + 하단 버튼) ---
                // [변경 3] AnimatedVisibility 대신 Box에 offset 적용
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = uiTranslationX) // 계산된 위치만큼 이동
                ) {
                    // (1) 우상단 고정 닫기 버튼
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 18.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Grey1000
                        )
                    }

                    // (2) 하단 고정 버튼 그룹 (이전 / 다음)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // [이전] 버튼
                        Button(
                            onClick = {
                                scope.launch {
                                    val prevPage = pagerState.currentPage - 1
                                    if (prevPage >= 0) pagerState.animateScrollToPage(prevPage)
                                }
                            },
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

                        // [다음/완료] 버튼
                        val isLastPage = pagerState.currentPage == totalPages - 1
                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onClose() // 마지막 페이지면 닫기
                                } else {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(41.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastPage) PurpleMain500 else Purple100,
                                contentColor = if (isLastPage) Grey0 else Purple700
                            )
                        ) {
                            Text(if (isLastPage) "완료" else "다음", style = label_semibold_14)
                        }
                    }
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
                .padding(start = 18.dp, end=18.dp,top = 16.dp,bottom = 24.dp),
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
// 1~3번 페이지에 들어갈 '알맹이(Content Only)' 컴포저블
// 버튼과 닫기 버튼은 상위 Box에서 고정으로 처리하므로 여기서는 제거하고 여백만 확보합니다.
@Composable
private fun LearnInfoInnerContent(
    title: String,
    description: String,
    imageRes: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 닫기 버튼 영역(Icon 24dp + Padding 16dp)과 기존 Spacer(18dp) 고려하여 여백 확보
        Spacer(modifier = Modifier.height(58.dp))

        // 이미지 카드
        Card(
            modifier = Modifier.fillMaxWidth()
            .height(153.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 타이틀
        Text(
            text = title,
            style = title_semibold_22,
            color = Grey1000
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 설명
        Text(
            text = description,
            style = b3_regular_14,
            color = Grey500,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 남은 공간 채우기
        Spacer(modifier = Modifier.weight(1f))

        // 하단 버튼 영역(Button 41dp + Padding 24dp)
        // 버튼은 고정(Overlay)되어 있지만, 텍스트가 가려지지 않게 하기 위함
        Spacer(modifier = Modifier.height(65.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickyAfterPageOneScreen() {
    val pagerState = rememberPagerState(pageCount = { 4 })

    // 화면 너비를 알기 위해 BoxWithConstraints 사용 (혹은 LocalConfiguration 사용)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth // 화면 전체 너비

        // 1. 페이저 (배경)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // 페이지별 콘텐츠
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Page $page Content", style = label_semibold_14)
                // 1번 페이지에도 버튼을 넣지 않습니다. (밖에 있는 버튼이 대신함)
            }
        }

        // 2. 버튼 오프셋 계산 (핵심)
        val buttonTranslationX by remember {
            derivedStateOf {
                // 현재 스크롤 위치 (예: 0.5는 0페이지와 1페이지 중간)
                val currentScrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction

                // 1번 페이지가 기준점(0)으로부터 얼마나 떨어져 있는지 계산
                // 0페이지에 있을 때: (1 - 0) = 1 (화면 너비만큼 이동해야 함)
                // 1페이지에 있을 때: (1 - 1) = 0 (이동 없음, 고정)
                // 2페이지에 있을 때: (1 - 2) = -1 (음수이므로 0으로 처리하여 고정)
                val distanceRequest = (1f - currentScrollPosition)

                // 음수면(이미 1페이지를 지났으면) 0으로 고정, 양수면 그만큼 밀어줌
                val offsetFraction = distanceRequest.coerceAtLeast(0f)

                // 픽셀 단위로 변환
                offsetFraction * screenWidth.value
            }
        }

        // 3. 고정 버튼 (Box 위에 띄움)
        Button(
            onClick = { /* 클릭 동작 */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .fillMaxWidth(0.8f)
                // 계산된 위치만큼 X축 이동 (dp 변환 필요)
                .offset(x = with(LocalDensity.current) { buttonTranslationX.toDp() })
        ) {
            Text("시작하기 (1번부터 고정)")
        }
    }
}
