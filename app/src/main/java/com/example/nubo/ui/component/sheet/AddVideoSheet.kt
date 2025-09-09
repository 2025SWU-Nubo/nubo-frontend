package com.example.nubo.ui.component.sheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey0
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.PurpleMain500
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.nubo.ui.screen.add.SheetTopToast
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.PinkError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nubo.ui.screen.cardupload.CardUploadViewModel
import com.example.nubo.ui.theme.GreyMain100


@Composable
fun AddVideoSheet(
    onClose: () -> Unit,
    viewModel: AddVideoViewModel = hiltViewModel(), // ilt로 VideoService 주입된 VM 획득
    cardUploadViewModel: CardUploadViewModel = hiltViewModel(), // 업로드 재사용 (이미 존재) :contentReference[oaicite:5]{index=5}
    accessToken: String? = null // 상위에서 넘겨주면 사용(또는 VM에서 Auth 주입)
) {
    // 페이지/입력/선택 상태 (네 코드 그대로)
    var page by rememberSaveable { mutableStateOf(SheetPage.SAVE_VIDEO) }
    var input by rememberSaveable { mutableStateOf("") }
    var checkedIds by rememberSaveable { mutableStateOf(setOf<String>()) }


    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showError by rememberSaveable { mutableStateOf(false) }   // 에러 강조 여부
    val shake = remember { Animatable(0f) }                       // 좌우 흔들기

    // 영상 링크용 토스트
    var toastVisible by rememberSaveable { mutableStateOf(false) } // 링크 무효할 때 토스트
    var networkErrorToastVisible by rememberSaveable { mutableStateOf(false) } // 네트워크 에러 토스트

    // 보드용 토스트
    var boardToastVisible by rememberSaveable { mutableStateOf(false) }
    // 한 번만 자동 노출되게 플래그
    var boardToastShown by rememberSaveable { mutableStateOf(false) }


    // 보드 페이지로 진입하면 자동 토스트 + 서버 데이터(보드와 섹션 계층 구조) 로드
    LaunchedEffect(page) {
        if (page == SheetPage.PICK_BOARD && !boardToastShown) {
            boardToastVisible = true
            boardToastShown = true
            // ★ 토큰이 있으면 보드 트리 로드
            accessToken?.let { viewModel.loadBoards(it) }
        }
    }

    // 보드 토스트 위치 맞추는 시트 위치
    val density = LocalDensity.current
    var sheetHeightPx by remember { mutableStateOf(0) }   // 현재 시트 높이(px)


    // 뷰모델의 검증 상태를 Compose에서 구독
    val validateState by viewModel.state.collectAsStateWithLifecycle(AddVideoViewModel.ValidateState.Idle)
    // 보드 트리 상태 구독
    val boardsState by viewModel.boards.collectAsStateWithLifecycle(AddVideoViewModel.BoardsState.Idle)

    // 에러 시 흔들기 트리거
    LaunchedEffect(showError) {
        if (showError) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    -8f at 60 using LinearEasing
                    8f  at 120 using LinearEasing
                    -6f at 180 using LinearEasing
                    6f  at 240 using LinearEasing
                    -3f at 300 using LinearEasing
                    3f  at 360 using LinearEasing
                    0f  at 420
                }
            )
        }
    }
    // 서버 응답에 따라 화면/모션 분기
    LaunchedEffect(validateState) {
        when (validateState) {
            is AddVideoViewModel.ValidateState.Success -> {
                // 유효 → 보드 선택 화면으로 이동
                page = SheetPage.PICK_BOARD
                showError = false
                toastVisible = false
            }

            AddVideoViewModel.ValidateState.Invalid -> {
                // 무효 → 흔들기 + 토스트
                showError = true
                toastVisible = true
                // 3초 후 입력/에러 초기화 (기존 UX 유지)
                scope.launch {
                    delay(3000)
                    showError = false
                    input = ""
                }
            }

            is AddVideoViewModel.ValidateState.Error -> {
                // 네트워크/서버 에러 → 동일 모션과 네트워크 에러용 토스트
                showError = true
                networkErrorToastVisible = true // 네트워크 에러 토스트
            }

            AddVideoViewModel.ValidateState.Loading,
            AddVideoViewModel.ValidateState.Idle -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
            .onGloballyPositioned { sheetHeightPx = it.size.height },  // 시트 높이 측정
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== 공통 헤더 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp)
        ) {
            //닫기 버튼
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(id = R.drawable.close_icon),
                    contentDescription = "닫기",
                    tint = Grey500
                )
            }
            // 타이틀 텍스트
            Text(
                text = when (page) {
                    SheetPage.SAVE_VIDEO -> "영상 추가하기"
                    SheetPage.PICK_BOARD -> "보드 선택"
                },
                style = AppTextStyles.b1_semibold_18,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ===== 페이지별 본문 =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 20.dp)
        ) {
            when (page) {
                SheetPage.SAVE_VIDEO -> {
                    // 입력창 + 버튼 한 줄, 좌우 16 / 사이 8
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 캡슐형 아웃라인 텍스트필드
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                input = it
                                if (showError) showError = false // 수정 시작하면 에러 강조 해제
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(49.dp)
                                .offset(x = shake.value.dp),          // 부르르 흔들기 적용
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                // 에러면 보더 제거 + 배경을 PinkError 로
                                focusedBorderColor = if (showError) Grey50 else Grey50,
                                unfocusedBorderColor = if (showError) Grey50 else Grey50,
                                focusedContainerColor = if (showError) PinkError else Color.White,
                                unfocusedContainerColor = if (showError) PinkError else Grey10,
                            ),
                            placeholder = {
                                Text(
                                    "링크를 입력하세요",
                                    style = AppTextStyles.b3_medium_14,
                                    color = Grey200
                                )
                            },
                            textStyle = AppTextStyles.b3_medium_14,
                        )

                        // 캡슐형 버튼 (56dp, 외곽선 + 연한 배경)
                        Button(
                            onClick = {
                                // 서버에 검증 요청 (링크는 앞뒤 공백 제거)
                                viewModel.validate(input.trim())
                            },
                            enabled = input.isNotBlank() && validateState !is AddVideoViewModel.ValidateState.Loading,
                            modifier = Modifier.height(49.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = if (input.isNotBlank()) null else BorderStroke(1.dp, Grey50),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleMain500,
                                contentColor = Grey0,
                                disabledContainerColor = Grey10,
                                disabledContentColor = Grey1000
                            )
                        ) {
                            // 로딩 중이면 인디케이터, 아니면 텍스트
                            if (validateState is AddVideoViewModel.ValidateState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Grey0
                                )
                            } else {
                                Text("추가", style = AppTextStyles.b3_medium_14)
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                }

                SheetPage.PICK_BOARD -> {
                    // 바텀시트 고정 높이 (원하는 값으로 조정)
                    val fixedHeight = 340.dp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fixedHeight)   // ← 시트 고정 높이
                    ) {
                        // 서버에서 내려온 보드+섹션 상태로 바인딩
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            when (boardsState) {
                                AddVideoViewModel.BoardsState.Idle, AddVideoViewModel.BoardsState.Loading -> {
                                    item {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                                is AddVideoViewModel.BoardsState.Error -> {
                                    item {
                                        Text(
                                            text = "보드 목록을 불러오지 못했어요.",
                                            style = AppTextStyles.b3_medium_14,
                                            color = Grey500,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                is AddVideoViewModel.BoardsState.Loaded -> {
                                    val tree = (boardsState as AddVideoViewModel.BoardsState.Loaded).boards
                                    items(tree, key = { it.id }) { node ->
                                        BoardNodeItem(
                                            node = node.toUi(),            // ← 아래 B)에 추가할 변환 함수
                                            level = 0,
                                            isChecked = { id -> checkedIds.contains(id) },
                                            onCheckedChange = { id, isOn ->
                                                checkedIds = if (isOn) checkedIds + id else checkedIds - id
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { boardToastVisible = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleMain500,
                                contentColor = Grey0
                            )
                        ) {
                            Text("추가하기", style = AppTextStyles.b1_bold_18)
                        }
                    }
                }
            }
        }
    }
    // 영상 링크용 영상 무효 토스트
    if (toastVisible) {
        val annotatedTitle = buildAnnotatedString {
            append("잘못된 링크!")
        }
        SheetTopToast(
            title = annotatedTitle,
            message = "유효하지 않은 링크입니다.\n영상 URL 확인 후 다시 입력해주세요.",
            visible = toastVisible,
            onDismiss = { toastVisible = false },
            durationMillis = 3000L, // 3초
            bottomOffset = 240.dp
        )
    }

    // 영상 링크용 네트워크 에러 토스트
    if (networkErrorToastVisible) {
        val annotatedTitle = buildAnnotatedString { append("네트워크 오류") }
        SheetTopToast(
            title = annotatedTitle,
            message = "네트워크 오류로 잠시 후 다시 시도해주세요.",
            visible = networkErrorToastVisible,
            onDismiss = { networkErrorToastVisible = false },
            durationMillis = 3000L,
            bottomOffset = 240.dp
        )
    }
    // 보드 선택용 토스트
    if (boardToastVisible) {
        // 시트 고정 높이 + 100dp만큼 위에 토스트
        val fixedHeight = 340.dp
        val offsetFromBottom = fixedHeight + 150.dp

        val annotatedTitle = buildAnnotatedString {
            append("선택하지 않아도 ")
            withStyle(SpanStyle(color = PurpleMain500)) { // 강조 색상
                append("AI가 자동 분류")
            }
            append("해줘요.")
        }

        SheetTopToast(
            title = annotatedTitle,
            message = "AI가 영상 내용을 자동으로 분석해\n적절한 보드 안에 저장해요.",
            visible = boardToastVisible,
            onDismiss = { boardToastVisible = false },
            durationMillis = 180_000L,
            bottomOffset = offsetFromBottom          // 시트 상대 위치에 맞춰 토스트 위치
        )
    }
}

private enum class SheetPage { SAVE_VIDEO, PICK_BOARD }

// ------------------------- 보드 선택용 컴포넌트들 -------------------------

private data class BoardNode(
    val id: String,
    val title: String,
    val children: List<BoardNode> = emptyList(),
    val selectable: Boolean = true
)

// 서버 UI모델(UiBoardNode) -> 화면용 BoardNode 변환
// 최상위 UiBoardNode를 받도록 수정
private fun UiBoardNode.toUi(): BoardNode {
    return BoardNode(
        id = id.toString(),        // Long -> String
        title = title,
        children = children.map { it.toUi() }
    )
}

@Composable
private fun BoardNodeItem(
    node: BoardNode,
    level: Int,
    isChecked: (String) -> Boolean,
    onCheckedChange: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // 부모 카테고리
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 펼침/접힘 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = if (expanded) "접기" else "펼치기",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { expanded = !expanded }
                    .graphicsLayer { rotationZ = if (expanded) 0f else 180f } // 접힘 시 위로
            )

            Spacer(Modifier.width(8.dp))                  // 아이콘-텍스트 사이 8dp

            Text(
                text = node.title,
                style = AppTextStyles.b3_medium_14,
                color = Grey1000,
                modifier = Modifier.weight(1f)
            )

            // 부모 줄 오른쪽에는 체크박스 없음
            Spacer(Modifier.width(8.dp))
        }

        // 펼쳐진 자식 리스트
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Grey20)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color = GreyMain100,           // 원하는 색상
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                node.children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = child.title,
                            style = AppTextStyles.b3_medium_14,
                            color = Grey1000,
                            modifier = Modifier.weight(1f)
                        )

                        // 체크박스 아이콘 토글 (기본: ic_add_blank_check_box, 선택: ic_fill_checkbox)
                        val checkedChild = isChecked(child.id)
                        val iconRes = if (checkedChild) R.drawable.ic_add_fill_checkbox
                        else R.drawable.ic_add_blank_check_box

                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = if (checkedChild) "선택됨" else "선택",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onCheckedChange(child.id, !checkedChild) },
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}
