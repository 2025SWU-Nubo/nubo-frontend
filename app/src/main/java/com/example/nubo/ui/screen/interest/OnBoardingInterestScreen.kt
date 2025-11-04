package com.example.nubo.ui.screen.interest

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // ← 반드시 필요
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nubo.R
import com.example.nubo.data.model.DefaultBoardItemResponse
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.Purple300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnBoardingInterestScreen(
    accessToken: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    thumbnailsRes: Map<Long, Int> = emptyMap(), // ← 보드ID → drawable 리소스 매핑
    viewModel: OnBoardingInterestViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 화면 진입 시 기본 보드 목록 로드
    LaunchedEffect(Unit) { viewModel.loadBoards(accessToken) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus(force = true)
                        onBack()
                    }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.submitSkip(
                            accessToken = accessToken,
                            onCompleted = { _ -> onHome() },
                            onError = { /* TODO: 에러 표시 */ }
                        )
                    }) {
                        Text("건너뛰기", color = Grey500, style = AppTextStyles.b2_semibold_16)
                    }
                },
                modifier = Modifier.drawBehind {
                    // 상단바 하단 1dp 구분선
                    val y = size.height
                    drawLine(
                        color = Grey50,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            // 하단 고정 '시작하기' 버튼 — 1개 이상 선택 시 활성화
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.submitSelected(
                            accessToken = accessToken,
                            onCompleted = { _ -> onHome() },
                            onError = { /* TODO: 에러 표시 */ }
                        )
                    },
                    enabled = state.canSubmit, // ← ViewModel 상태와 연동
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("시작하기", style = AppTextStyles.b2_semibold_16)
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text("어떤 영상을 자주 보시나요?", style = AppTextStyles.subtitle_semibold_20)
            Spacer(Modifier.height(8.dp))
            Text(
                "선택한 관심사를 기반으로 추천 카드를 보여드려요.",
                style = AppTextStyles.b3_regular_14,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(20.dp))

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error ?: "오류", style = AppTextStyles.b3_regular_14, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.loadBoards(accessToken) }) {
                            Text("다시 시도")
                        }
                    }
                }
                else -> {
                    // 3열 원형 카드 그리드
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.boards, key = { it.boardId }) { item ->
                            // ▼ 여기서 보드ID → drawable 리소스 ID를 찾아서 imageRes 로 전달해야 함
                            val imageRes = thumbnailsRes[item.boardId] ?: R.drawable.basic_profile_image
                            InterestCircleChip(
                                item = item,
                                imageRes = imageRes,                     // ← 누락됐던 파라미터
                                selected = item.boardId in state.selectedIds,
                                onClick = { viewModel.toggle(item.boardId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 관심 보드 원형 카드(버튼)
 * - 배경: PNG/VectorDrawable 이미지를 꽉 차게 표시(ContentScale.Crop)
 * - 오버레이: 미선택(검정 35%), 선택(보라 25%) + 보라 외곽선 + 체크 배지
 */
@Composable
private fun InterestCircleChip(
    item: DefaultBoardItemResponse,
    @DrawableRes imageRes: Int,   // ← 호출부에서 반드시 넘겨줘야 함
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipSize = 96.dp

    Box(
        modifier = Modifier
            .size(chipSize)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(imageRes),
            contentDescription = item.boardName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // 기본 어두운 스크림(가독성)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // 중앙 보드 이름
        Text(
            text = item.boardName,
            style = AppTextStyles.b1_semibold_18,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp)
        )

        if (selected) {
            // 선택 시 보라 스크림
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
            // 보라 외곽선 링
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = Purple300,
                            radius = size.minDimension / 2f - 2.dp.toPx() / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
            )
            // 체크 배지
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.check), // 리소스 이름 확인 필요
                    contentDescription = "선택됨",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ----------------------------
// ✅ CHIP 단일 프리뷰 2종 (미선택/선택)
// ----------------------------
@Preview(name = "Chip - Unselected", showBackground = true)
@Composable
private fun Preview_InterestCircleChip_Unselected() {
    // 프리뷰용 더미 데이터(실제 서버 응답 모델과 동일 필드 사용 가정)
    val dummy = DefaultBoardItemResponse(
        boardId = 60L,
        boardName = "테크 & 프로그래밍"
    )
    MaterialTheme {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            InterestCircleChip(
                item = dummy,
                imageRes = R.drawable.basic_profile_image, // PNG/SVG Vector 사용 가능
                selected = false, // 미선택 상태
                onClick = {}
            )
        }
    }
}

@Preview(name = "Chip - Selected", showBackground = true)
@Composable
private fun Preview_InterestCircleChip_Selected() {
    val dummy = DefaultBoardItemResponse(
        boardId = 61L,
        boardName = "비즈니스 & 생산성"
    )
    MaterialTheme {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            InterestCircleChip(
                item = dummy,
                imageRes = R.drawable.basic_profile_image,
                selected = true, // 선택 상태
                onClick = {}
            )
        }
    }
}
