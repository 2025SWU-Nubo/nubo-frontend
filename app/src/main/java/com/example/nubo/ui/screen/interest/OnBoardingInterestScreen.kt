package com.example.nubo.ui.screen.interest

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
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
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnBoardingInterestScreen(
    accessToken: String, // 현재는 ViewModel에서 토큰을 다시 수집함. 남겨두되 사용은 안 함.
    onBack: () -> Unit,
    onHome: () -> Unit,
    thumbnailsRes: Map<Long, Int> = emptyMap(),
    viewModel: OnBoardingInterestViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current

    // 스낵바 / snackbar
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val maxSelect = 5
    val selectedIds = state.selectedIds
    val limitReached = selectedIds.size >= maxSelect

    // ViewModel에서 accessToken 수집 / collect accessToken from VM
    val tokenFromVm by viewModel.accessToken.collectAsStateWithLifecycle()

    // 토큰 준비 시 보드 목록 조회 / load boards once token is ready
    LaunchedEffect(tokenFromVm) {
        tokenFromVm?.let { viewModel.loadBoards(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
//                    IconButton(onClick = {
//                        focusManager.clearFocus(force = true)
//                        onBack()
//                    }) {
//                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
//                    }
                },
                actions = {
                    TextButton(onClick = {
                        tokenFromVm?.let { token ->
                            viewModel.submitSkip(
                                accessToken = token,
                                onCompleted = { _ -> onHome() },
                                onError = { /* TODO: 에러 표시 */ }
                            )
                        }
                    }) {
                        Text("건너뛰기", color = Grey200, style = AppTextStyles.b2_semibold_16)
                    }
                },
                modifier = Modifier.drawBehind {
                    // 하단 1dp 구분선 / 1dp divider below the app bar
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
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        tokenFromVm?.let { token ->
                            viewModel.submitSelected(
                                accessToken = token,
                                onCompleted = { _ -> onHome() },
                                onError = { /* TODO: 에러 표시 */ }
                            )
                        }
                    },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        // 활성화 상태
                        containerColor = PurpleMain500,
                        contentColor = Color.White,
                        // 비활성화 상태
                        disabledContainerColor = PurpleMain500.copy(alpha = 0.4f),
                        disabledContentColor = Color.White
                    ),
                ) {
                    Text("시작하기", style = AppTextStyles.b1_bold_18)
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
            Spacer(Modifier.height(50.dp))
            Text("어떤 영상을 자주 보시나요?", style = AppTextStyles.headline_bold_28, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Text(
                "선택한 관심사를 기반으로 추천 카드를 보여드려요.",
                style = AppTextStyles.b1_semibold_18,
                color = PurpleMain500,
                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error ?: "오류", style = AppTextStyles.b3_regular_14, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { tokenFromVm?.let { viewModel.loadBoards(it) } }) {
                            Text("다시 시도")
                        }
                    }
                }
                else -> {
                    // 3열 원형 그리드 / 3-column circular grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        items(state.boards, key = { it.boardId }) { item ->
                            // 각 보드 이름으로 이미지 연결하여 칩 배경으로 사용
                            val imageRes = thumbnailsRes[item.boardId] ?: InterestAssets.of(item.boardId, item.boardName)
                            val isSelected = item.boardId in state.selectedIds
                            val enabled = isSelected || !limitReached // 선택 해제는 항상 가능 / allow deselect anytime

                            InterestCircleChip(
                                item = item,
                                imageRes = imageRes,
                                enabled = enabled,
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected && limitReached) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("최대 5개까지 선택할 수 있어요")
                                        }
                                    } else {
                                        val next = if (isSelected) selectedIds - item.boardId else selectedIds + item.boardId
                                        viewModel.updateSelected(next)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "최대 5개까지 선택 가능  •  현재 ${selectedIds.size}개 선택",
                style = AppTextStyles.label_medium_14,
                color = GreyMain300,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * 관심사 원형 칩
 * - 이미지가 원형 내부를 빈틈 없이 채우도록 Clip + Crop 조합
 * - 선택 시 보랏빛 틴트와 체크 배지 표시
 * - 선택 불가 상태일 때 반투명 처리
 *
 * Circular interest chip
 * - Clip + ContentScale.Crop to fill circle without gaps
 * - Purple tint & check badge when selected
 * - Dim when disabled (over selection cap)
 */
@Composable
private fun InterestCircleChip(
    item: DefaultBoardItemResponse,
    @DrawableRes imageRes: Int,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipSize = 105.dp

    // 바깥 컨테이너는 clip 하지 않음(배지가 살짝 밖으로 나가도록)
    // Do not clip the outer box so the badge can protrude slightly
    Box(
        modifier = Modifier
            .size(chipSize)
            .alpha(if (enabled || selected) 1f else 0.5f)
            .clickable(enabled = enabled || selected) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 내부 원형 컨테이너 / inner circular container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            // 배경 이미지(원형을 꽉 채움) / background image fills the circle
            Image(
                painter = painterResource(imageRes),
                contentDescription = item.boardName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (selected)
                    ColorFilter.tint(
                        PurpleMain500.copy(alpha = 0.6f),
                        BlendMode.SrcAtop // 이미지 위에 색상을 얹음 / tint above image
                    )
                else null
            )

            // 타이틀(가독성 향상을 위한 얕은 그림자) / title with subtle shadow
            Text(
                text = item.boardName,
                style = AppTextStyles.b2_bold_16.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                        blurRadius = 12f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            )
        }

        // 선택 배지(좌상단, 살짝 바깥) / selection badge at top-left slightly outside
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (8).dp, y = (3).dp) // 살짝 밖으로 / slightly outside
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PurpleMain500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = "선택됨",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}


// ===============================
// Interactive Preview (최대 5개 선택 테스트용)
// - ViewModel/Hilt 없이 로컬 상태로 동작
// - 선택 5개 초과 시 스낵바 안내
// ===============================
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Interest Screen – Interactive", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun Preview_InterestScreen_Interactive() {
    // 더미 보드 데이터(12개)
    val dummyBoards = listOf(
        DefaultBoardItemResponse(1,  "교육"),
        DefaultBoardItemResponse(2,  "테크 & 프로그래밍"),
        DefaultBoardItemResponse(3,  "비즈니스 & 생산성"),
        DefaultBoardItemResponse(4,  "뷰티 패션"),
        DefaultBoardItemResponse(5,  "요리 라이프스타일"),
        DefaultBoardItemResponse(6,  "운동 건강"),
        DefaultBoardItemResponse(7,  "여행 브이로그"),
        DefaultBoardItemResponse(8,  "게임"),
        DefaultBoardItemResponse(9,  "취미 공예"),
        DefaultBoardItemResponse(10, "음악"),
        DefaultBoardItemResponse(11, "예술 디자인"),
        DefaultBoardItemResponse(12, "엔터테인먼트"),
    )

    // 썸네일 리소스 매핑(없으면 공통 이미지로)
    val thumbnailsRes = remember {
        dummyBoards.associate { it.boardId to R.drawable.education }
    }

    // 선택 상태(프리뷰 전용 로컬 상태)
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val maxSelect = 5
    val limitReached = selectedIds.size >= maxSelect

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(title = { /* 프리뷰라 타이틀 생략 */ })
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { /* 프리뷰라 내비게이션 생략 */ },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            // 활성화 상태
                            containerColor = PurpleMain500,
                            contentColor = Color.White,
                            // 비활성화 상태
                            disabledContainerColor = PurpleMain500.copy(alpha = 0.4f), // 원하는 색으로
                            disabledContentColor = Color.White // 원하는 색으로
                        )
                    ) {
                        Text("시작하기", style = AppTextStyles.b1_bold_18)
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
                Text("어떤 영상을 자주 보시나요?", style = AppTextStyles.headline_bold_28, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(8.dp))
                Text(
                    "선택한 관심사를 기반으로 추천 카드를 보여드려요.",
                    style = AppTextStyles.b1_semibold_18,
                    color = PurpleMain500,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(20.dp))

                // 3열 원형 그리드
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dummyBoards, key = { it.boardId }) { item ->
                        val imageRes = thumbnailsRes[item.boardId] ?: R.drawable.interest_education
                        val isSelected = item.boardId in selectedIds
                        val enabled = isSelected || !limitReached

                        InterestCircleChip(
                            item = item,
                            imageRes = imageRes,
                            enabled = enabled,
                            selected = isSelected,
                            onClick = {
                                if (!isSelected && limitReached) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("최대 5개까지 선택할 수 있어요")
                                    }
                                } else {
                                    selectedIds = if (isSelected) {
                                        selectedIds - item.boardId
                                    } else {
                                        selectedIds + item.boardId
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "최대 5개까지 선택 가능  •  현재 ${selectedIds.size}개 선택",
                    style = AppTextStyles.label_medium_14,
                    color = GreyMain300,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

