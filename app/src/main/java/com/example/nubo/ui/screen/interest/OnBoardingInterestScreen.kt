package com.example.nubo.ui.screen.interest

import android.R.attr.scaleX
import android.R.attr.scaleY
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.nubo.ui.theme.PurpleMain500

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


    // ViewModel에서 accessToken을 가져옴
    val accessToken by viewModel.accessToken.collectAsStateWithLifecycle()

    // accessToken이 준비되면 보드 목록 로드
    LaunchedEffect(accessToken) {
        accessToken?.let { token ->
            viewModel.loadBoards(token)
        }
    }

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
                        accessToken?.let { token ->
                            viewModel.submitSkip(
                                accessToken = token,
                                onCompleted = { _ -> onHome() },
                                onError = { /* TODO: 에러 표시 */ }
                            )
                        }
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
                        accessToken?.let { token ->
                            viewModel.submitSelected(
                                accessToken = token,
                                onCompleted = { _ -> onHome() },
                                onError = { /* TODO: 에러 표시 */ }
                            )
                        }
                    },
                    enabled = state.selectedIds.isNotEmpty(), // ← ViewModel 상태와 연동
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
            Text("어떤 영상을 자주 보시나요?", style = AppTextStyles.headline_bold_28)
            Spacer(Modifier.height(8.dp))
            Text(
                "선택한 관심사를 기반으로 추천 카드를 보여드려요.",
                style = AppTextStyles.b1_semibold_18,
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
                        OutlinedButton(onClick = {
                            accessToken?.let { viewModel.loadBoards(it) }
                        }) {
                            Text("다시 시도")
                        }
                    }
                }
                else -> {
                    // 3열 원형 카드 그리드
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.boards, key = { it.boardId }) { item ->
                            // ▼ 여기서 보드ID → drawable 리소스 ID를 찾아서 imageRes 로 전달해야 함
                            val imageRes = thumbnailsRes[item.boardId] ?: R.drawable.education
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
//@Composable
//private fun InterestCircleChip(
//    item: DefaultBoardItemResponse,
//    @DrawableRes imageRes: Int,   // ← 호출부에서 반드시 넘겨줘야 함
//    selected: Boolean,
//    onClick: () -> Unit
//) {
//    val chipSize = 100.dp
//
//    Box(
//        modifier = Modifier
//            .size(chipSize)
//            .aspectRatio(1f) // 가로세로 비율 1:1 유지 (동그라미 형태 유지)
//            .clip(CircleShape)
//            .clickable { onClick() },
//        contentAlignment = Alignment.Center
//    ) {
//        // 2) Inner circle: clip content to circle
//        Box(
//            modifier = Modifier
//                .matchParentSize()
//                .clip(CircleShape)
//        ) {
//            // 배경 이미지
//            Image(
//                painter = painterResource(imageRes),
//                contentDescription = item.boardName,
//                contentScale = ContentScale.Crop,
//                modifier = Modifier
//                    .fillMaxSize()
//            )
//
//            if (selected) {
//                // soft purple tint
//                Box(
//                    modifier = Modifier
//                        .matchParentSize()
//                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
//                )
//                // purple ring
//                Box(
//                    modifier = Modifier
//                        .matchParentSize()
//                        .border(1.dp, Purple300, CircleShape)
//                )
//            }
//
//            Text(
//                text = item.boardName,
//                style = AppTextStyles.b1_semibold_18.copy(
//                    // subtle shadow to keep contrast on any background
//                    shadow = Shadow(
//                        color = Color(0x66000000),
//                        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
//                        blurRadius = 4f
//                    )
//                ),
//                color = Color.White,
//                textAlign = TextAlign.Center,
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .padding(horizontal = 8.dp)
//            )
//
//            if (selected) {
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.TopStart)
//                        .offset(x = (-8).dp, y = (-8).dp) // outside a bit
//                        .size(24.dp)
//                        .clip(CircleShape)
//                        .background(Color.White),          // white badge background
//                    contentAlignment = Alignment.Center
//                ) {
//                    // inner colored circle with check
//                    Box(
//                        modifier = Modifier
//                            .size(20.dp)
//                            .clip(CircleShape)
//                            .background(MaterialTheme.colorScheme.primary),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.check),
//                            contentDescription = "선택됨",
//                            tint = Color.White,
//                            modifier = Modifier.size(14.dp)
//                        )
//                    }
//                }
//            }
//        }
//
//        // 중앙 보드 이름
//        Text(
//            text = item.boardName,
//            style = AppTextStyles.b1_semibold_18,
//            color = Color.White,
//            textAlign = TextAlign.Center,
//            modifier = Modifier
//                .align(Alignment.Center)
//                .padding(horizontal = 8.dp)
//        )
//    }
//}

@Composable
private fun InterestCircleChip(
    item: DefaultBoardItemResponse,
    @DrawableRes imageRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipSize = 105.dp
    val imageScale = 1.08f

    // Outer container: no clip (badge can protrude)
    Box(
        modifier = Modifier
            .size(chipSize)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Inner circle: clip only the circle content
        Box(
            modifier = Modifier
                .matchParentSize()
                .aspectRatio(1f)
                .clip(CircleShape)
        ) {
            // Background image
            Image(
                painter = painterResource(imageRes),
                contentDescription = item.boardName,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
                .graphicsLayer {
                scaleX = imageScale
                scaleY = imageScale
            },
                colorFilter = if (selected)
                    ColorFilter.tint(PurpleMain500.copy(alpha = 0.8f), BlendMode.SrcAtop)
                else null
            )

            // Title on top
            Text(
                text = item.boardName,
                style = AppTextStyles.b2_bold_16.copy(
                    shadow = Shadow(
                        color = Color(0x66000000),
                        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
            )
        }

        // Check badge: place on OUTER box so it's not clipped
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (3).dp, y = (1).dp) // slightly outside
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = "선택됨",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
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
                imageRes = R.drawable.interest_education,
                selected = true, // 선택 상태
                onClick = {}
            )
        }
    }
}
