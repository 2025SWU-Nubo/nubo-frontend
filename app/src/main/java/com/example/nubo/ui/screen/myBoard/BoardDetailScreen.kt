package com.example.nubo.ui.screen.myBoard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nubo.R
import com.example.nubo.data.model.CardItemDto
import com.example.nubo.data.model.SectionDto
import com.example.nubo.model.myBoard.BoardItem
import com.example.nubo.ui.component.BoardDetailContent
import com.example.nubo.ui.theme.AppTextStyles.label_medium_12
import com.example.nubo.ui.theme.AppTextStyles.headline_regular_26
import com.example.nubo.ui.theme.AppTextStyles.subtitle_medium_16
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.model.card.CardItem
import com.example.nubo.ui.component.randomCardHeight
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b3_medium_14
import getDisplayDate
import java.net.URLDecoder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.components.toast.AppToastHost
import com.example.components.toast.AppToastLayout
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.Purple50
import kotlinx.coroutines.launch

// 어떤 다이얼로그를 띄울지 구분하기 위한 sealed class
sealed class InputDialogMode {
    data object CreateSection : InputDialogMode()
    data class Rename(val sectionId: Int, val currentName: String) : InputDialogMode()
}

@Composable
fun BoardDetailScreen(
    boardId: Int,
    boardTitle: String,
    navController: NavController,
    viewModel: BoardDetailViewModel = hiltViewModel(),
    myCardViewModel: MyCardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {

    // --- 선택 모드 관리를 위한 상태 변수 ---
    // 1. 선택 모드 활성화 여부
    var isSelectionMode by remember { mutableStateOf(false) }

    // 2. 선택된 섹션들의 ID를 저장하는 Set
    var selectedSections by remember { mutableStateOf(emptySet<Int>()) }

    // 3. 선택된 카드들의 ID를 저장하는 Set
    var selectedCards by remember { mutableStateOf(emptySet<Int>()) }
    // -----------------------------------------

    // --- 선택 모드 바텀바 관련 변수 ---
    var showBoardSelector by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<BoardAction?>(null) }
    val boardsState by viewModel.boards.collectAsState()

    val resetSelectionState = {
        isSelectionMode = false
        showBoardSelector = false
        currentAction = null
        selectedSections = emptySet()
        selectedCards = emptySet()
    }
    // -----------------------------------------

    //  다이얼로그 모드 상태
    var dialogMode by remember { mutableStateOf<InputDialogMode?>(null) }

    // 삭제 확인 다이얼로그 표시 상태
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 진입 시 한 번 초기 로드
    LaunchedEffect(boardId) {
        viewModel.init(boardId)
    }

    // 1. '실행 취소'용 Snackbar 상태
    val snackbarHostState = remember { SnackbarHostState() }
    // 토스트 상태 및 코루틴 스코프 선언
    val toastHostState = rememberAppToastHostState()
    val scope = rememberCoroutineScope()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // '실행 취소' 스낵바를 띄우는 함수
    fun showUndoSnackbar() {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "삭제가 완료되었습니다.",
                actionLabel = "실행 취소",
                duration = SnackbarDuration.Long // 길게 표시 (약 10초)
            )
            if (result == SnackbarResult.ActionPerformed) {
                // TODO: "실행 취소" 클릭 시 서버 연동 로직
            }
        }
    }

    // ViewModel의 삭제 완료 이벤트를 구독하여 스낵바 띄우고 상태 초기화
    LaunchedEffect(Unit) {
        viewModel.deleteCompleteEvent.collect {
            showUndoSnackbar()
            resetSelectionState()
        }
    }

    // ViewModel의 toastMessage 변경을 감지하여 토스트 표시
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            scope.launch {
                toastHostState.show(
                    title = buildAnnotatedString { append(message) },
                    layout = AppToastLayout.TitleOnly // 제목만 있는 레이아웃 사용
                )
            }
            // 토스트를 띄운 후에는 상태를 다시 null로 초기화하여 중복 표시 방지
            viewModel.clearToastMessage()
        }
    }

    // SectionDetailScreen에서 이름 변경 결과를 수신하는 부분
    LaunchedEffect(navController.currentBackStackEntry) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        val id = handle?.get<Int>("renamed_section_id")
        val name = handle?.get<String>("renamed_section_name")

        if (id != null && name != null) {
            viewModel.renameSection(sectionId = id, newName = name)
            handle.remove<Int>("renamed_section_id")
            handle.remove<String>("renamed_section_name")
        }
    }

    // 뷰모델 상태 올바르게 구독
    val ui by viewModel.ui.collectAsState()
    val boardState = ui.board

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                    UndoSnackbar(
                        message = snackbarData.visuals.message,
                        onUndo = { snackbarData.performAction() }
                    )
                }
            },
            containerColor = Color.White
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                DetailTopBar(onBack = {
                    // 현재 보드의 최신 이름 전달
                    val latestName = ui.board?.name ?: boardTitle
                    navController.previousBackStackEntry?.savedStateHandle?.set("renamed_board_id", boardId)
                    navController.previousBackStackEntry?.savedStateHandle?.set("renamed_board_name", latestName)
                    navController.popBackStack()
                })
                BoardTitleBar(
                    title = ui.board?.name ?: boardTitle,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        dialogMode = InputDialogMode.Rename(
                            sectionId = boardId, // 현재 보드 id
                            currentName = ui.board?.name ?: boardTitle
                        )
                    })
                // 즐겨찾기 필터만 뷰모델과 연결 (정렬 버튼은 UI만 유지, 서버 쿼리는 LATEST 고정)
                BoardFilterButton(
                    favoriteSelected = ui.favoriteOnly,
                    onToggleFavorite = { enabled -> viewModel.setFavoriteFilter(enabled) },
                    onAddClick = { dialogMode = InputDialogMode.CreateSection },
                    onSelectClick = {// 선택/취소 버튼 클릭 시 로직
                        if (isSelectionMode) {
                            resetSelectionState() // '취소' 시 모든 선택 상태 초기화
                        } else {
                            isSelectionMode = true // '선택' 시 선택 모드 시작
                        }
                    },
                    onRequestSort = { sortKey -> viewModel.setSort(sortKey) },
                    isSelectionMode = isSelectionMode, // 선택 상태 변수 전달
                )

                if (boardState != null) {
                    val boardItems = boardState.sections?.map { it.toBoardItem() } ?: emptyList()
                    // 페이징 래퍼에서 실제 리스트 꺼내기
                    val cardItems = boardState.cards.content.map { it.toCardItem() }
                    // 카드 배열 길이가 바뀌면 높이도 재생성
                    val cardHeights by remember(boardId, cardItems.size) {
                        mutableStateOf(cardItems.map { randomCardHeight() })
                    }

                    BoardDetailContent(
                        boardItems = boardItems,
                        cardItems = cardItems,
                        cardHeights = cardHeights,
                        onCardClick = { cardId ->
                            // 카드 클릭 시 선택/해제 로직 추가
                            if (isSelectionMode) {
                                selectedCards = if (selectedCards.contains(cardId)) {
                                    selectedCards - cardId
                                } else {
                                    selectedCards + cardId
                                }
                            } else {
                                navController.navigate("card_detail/$cardId")
                            }
                        },
                        onSectionClick = { section ->
                            if (isSelectionMode) {
                                selectedSections = if (selectedSections.contains(section.id)) {
                                    selectedSections - section.id
                                } else {
                                    selectedSections + section.id
                                }
                            } else {
                                val encodedTitle = java.net.URLEncoder.encode(section.title, "utf-8")
                                navController.navigate("section_detail/${section.id}/$encodedTitle")
                            }
                        },
                        onFavoriteClick = { section: BoardItem ->
                            viewModel.toggleSectionFavorite(
                                sectionId = section.id,
                                currentFavorite = section.isBookmarked
                            )
                        },
                        isSelectionMode = isSelectionMode,
                        selectedSections = selectedSections,
                        selectedCards = selectedCards
                    )
                } else {
                    Text("Loading...")
                }
            }
        }
        // 선택 모드일 때 화면 하단에 바텀 시트 표시
        SelectionBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = isSelectionMode,
            showBoardSelector = showBoardSelector,
            actionsContent = {
                ActionsContent(
                    selectedSectionCount = selectedSections.size,
                    selectedCardCount = selectedCards.size,
                    onDeleteClick = { showDeleteDialog = true },
                    onCopyClick = {
                        currentAction = BoardAction.COPY
                        showBoardSelector = true
                        viewModel.loadBoards()
                    },
                    onMoveClick = {
                        currentAction = BoardAction.MOVE
                        showBoardSelector = true
                        viewModel.loadBoards()
                    }
                )
            },
            boardSelectorContent = {
                BoardSelectionSheetContent(
                    action = currentAction ?: BoardAction.COPY,
                    boardsState = boardsState,
                    onBack = { showBoardSelector = false },
                    onConfirm = { selectedId -> //selectedTargetIds -> selectedId (타입: String?)
                        // selectedId가 null이 아닐 때만 로직 실행
                        selectedId?.let { targetId ->
                            when (currentAction) {
                                BoardAction.COPY -> {
                                    viewModel.copySelectedItems(
                                        targetBoardId = targetId.toLong(),
                                        selectedSectionIds = selectedSections,
                                        selectedCardIds = selectedCards
                                    )
                                }

                                BoardAction.MOVE -> {
                                    viewModel.moveSelectedItems(
                                        targetBoardId = targetId.toLong(),
                                        selectedSectionIds = selectedSections,
                                        selectedCardIds = selectedCards
                                    )
                                }

                                null -> {}
                            }
                        }
                        // 작업 완료 후 선택 모드 초기화
                        resetSelectionState()
                    }
                )
            }
        )
        // ==== 다이얼로그 표시 ====
        when (val m = dialogMode) {
            InputDialogMode.CreateSection -> NuboInputDialog(
                visible = true,
                title = "섹션 추가하기",
                confirmText = "생성",
                placeholder = "섹션 이름",
                onConfirm = { name -> viewModel.createSection(name) },
                onDismiss = { dialogMode = null },
                // 섹션 생성 시에도 유효성 검사 메시지 추가
                validationContent = {
                    Text(
                        text = "섹션 이름을 2자 이상 입력해주세요.",
                        style = b3_regular_14,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, start = 16.dp)
                    )
                }
            )

            is InputDialogMode.Rename -> NuboInputDialog(
                visible = true,
                title = "보드 이름 변경",
                confirmText = "완료",
                placeholder = "새 이름",
                initialValue = m.currentName,
                onConfirm = { newName ->
                    // 보드 ID와 다이얼로그의 ID를 비교하여 올바른 함수를 호출
                    if (m.sectionId == boardId) {
                        // ID가 현재 보드 ID와 같으면 보드 이름 변경 함수를 호출
                        viewModel.renameCurrentBoard(newName = newName)
                    } else {
                        // 다르다면 섹션 이름 변경 함수를 호출
                        viewModel.renameSection(sectionId = m.sectionId, newName = newName)
                    }
                },
                onDismiss = { dialogMode = null },
                // 유효성 검사 실패 시 보여줄 메시지 UI
                validationContent = {
                    Text(
                        text = "보드 이름을 2자 이상 입력해주세요.",
                        style = b3_regular_14,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, start = 16.dp)
                    )
                }
            )

            null -> Unit
        }
    }
    // 삭제 다이얼로그
    DeleteConfirmationDialog(
        visible = showDeleteDialog,
        selectedCardCount = selectedCards.size,
        selectedSectionCount = selectedSections.size,
        onDismiss = { showDeleteDialog = false },
        onRemove = {
            showDeleteDialog = false
            // ViewModel 함수 호출
            scope.launch {
                viewModel.removeItemsFromBoard(selectedSections, selectedCards)
            }
        },
        onDelete = {
            showDeleteDialog = false
            // ViewModel 함수 호출
            scope.launch {
                viewModel.deleteItems(selectedSections, selectedCards)
            }
        }
    )
    // 토스트 UI를 화면에 배치
    AppToastHost(hostState = toastHostState)
}


@Composable
fun DetailTopBar(onBack: () -> Unit) {
    val titleText = "나의 보드"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 16.dp, top = 13.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back), // ← ← 아이콘 파일 필요
            contentDescription = "뒤로가기",
            tint = GreyMain300,
            modifier = Modifier
                .clickable { onBack() }
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = titleText,
            style = subtitle_medium_16,
            color = GreyMain300
        )
    }
}


@Composable
fun BoardTitleBar(title: String, isSelectionMode: Boolean, onClick: () -> Unit) {
    val decodedTitle = URLDecoder.decode(title, "utf-8")

    Column(modifier = Modifier.padding(top = 27.dp)) {
        Row(
            modifier = Modifier
                .noRippleClickable(enabled = !isSelectionMode) { onClick() } //선택 모드일 때 버튼 비활성화
                .padding(start = 18.dp, end = 16.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = decodedTitle,
                style = headline_regular_26,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BoardFilterButton(
    favoriteSelected: Boolean,
    onToggleFavorite: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onSelectClick: () -> Unit,
    onRequestSort: (String) -> Unit,
    isSelectionMode: Boolean
) {
    // '즐겨찾기' 버튼의 선택 상태를 관리
    var selected by remember(favoriteSelected) {
        mutableStateOf(if (favoriteSelected) "즐겨찾기" else null)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 정렬/필터 버튼들
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 정렬 버튼
            SortFilterButton(
                enabled = !isSelectionMode, //선택 모드일 때 버튼 비활성화
                onSortSelected = { sortKey -> onRequestSort(sortKey) }
            )

            // 즐겨찾기 버튼
            val isFavoriteSelected = selected == "즐겨찾기"
            OutlinedButton(
                onClick = {
                    val nextOn = !isFavoriteSelected
                    selected = if (nextOn) "즐겨찾기" else null
                    onToggleFavorite(nextOn) // 서버 필터 동기화
                },
                enabled = !isSelectionMode, //선택 모드일 때 버튼 비활성화
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFavoriteSelected) Purple50 else Color.Transparent,
                    contentColor = if (isFavoriteSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (isFavoriteSelected) PurpleMain500 else Grey200),
                modifier = Modifier.height(35.dp),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "즐겨찾기", // "label" 변수 대신 텍스트를 직접 사용
                        style = label_medium_12,
                        color = if (isFavoriteSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_star),
                        contentDescription = "즐겨찾기",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // 오른쪽 버튼들(기존 그대로)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddClick,
                enabled = !isSelectionMode,
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple100.copy(alpha = 0.3f),
                    contentColor = PurpleMain500,
                    // 비활성화 상태에서도 활성화 색상과 동일하게 유지
                    disabledContainerColor = Purple100.copy(alpha = 0.3f),
                    disabledContentColor = PurpleMain500
                ),
                border = BorderStroke(0.5.dp, PurpleMain500),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter_add),
                    contentDescription = "섹션 추가",
                    tint = PurpleMain500
                )
            }

            // 선택 버튼 UI
            // '선택' 또는 '취소' 버튼
            val buttonText = if (isSelectionMode) "취소" else "선택"
            val containerColor = if (isSelectionMode) PurpleMain500 else Purple100.copy(alpha = 0.3f)
            val contentColor = if (isSelectionMode) Color.White else PurpleMain500

            Button(
                onClick = onSelectClick, // 클릭 시 isSelectionMode 상태를 토글하는 람다 연결 예정
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                border = if (!isSelectionMode) BorderStroke(0.5.dp, PurpleMain500) else null,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = buttonText, style = label_medium_12)
            }
        }
    }
}


fun SectionDto.toBoardItem(): BoardItem {
    return BoardItem(
        id = this.id.toInt(),              // ← Long → Int
        serverBoardId = this.id.toInt(),
        title = this.name,
        subtitle = "${this.cardCount} 카드",
        createdAt = getDisplayDate(this.updatedAt),
        isBookmarked = this.favorite,
        source = this.source,
        imageUrl = this.thumbnailUrl
    )
}

fun CardItemDto.toCardItem(): CardItem {
    return CardItem(
        id = this.id,
        height = randomCardHeight(), // 기존 randomCardHeight() 함수 사용
        title = this.title ?: "No Title", // 서버 데이터 없을 경우 기본값
        category = this.category ?: "No Category", // 마찬가지
        description = this.description ?: "No Description",
        imageUrl = this.imageUrl ?: "",
        isFavorite = this.favorite ?: false
    )
}

// 섹션 이름 입력 다이얼로그
@Composable
fun NuboInputDialog(
    // 다이얼로그 표시 여부
    visible: Boolean,
    // 상단 가운데 타이틀
    title: String,
    // 우측 텍스트 버튼 라벨
    confirmText: String,
    // 입력창 플레이스홀더
    placeholder: String,
    // 최초 값 (이름 변경 시 기존 이름)
    initialValue: String = "",
    // 확인 클릭 시 콜백 (서버 연동은 추후 이곳에서)
    onConfirm: (String) -> Unit,
    // X 또는 백드롭 클릭 시 닫기
    onDismiss: () -> Unit,
    // 유효성 검사 실패 시 보여줄 Composable
    validationContent: @Composable (() -> Unit)? = null
) {
    if (!visible) return

    // Compose Dialog는 배경을 자동으로 어둡게 처리함
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        // 카드 형태의 컨테이너
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            horizontalAlignment = Alignment.Start
        ) {
            // 내부 상태 보관
            var text by remember { mutableStateOf(initialValue) }

            // '생성'과 '이름 변경'의 활성화 조건을 분리
            val confirmEnabled = if (initialValue.isBlank()) {
                // 생성 모드: 2글자 이상이면 활성화
                text.trim().length >= 2
            } else {
                // 이름 변경 모드: 2글자 이상이면서, 이전 이름과 다를 때 활성화
                text.trim().length >= 2 && text.trim() != initialValue
            }

            // 헤더 영역: X 버튼 + 타이틀 + 우측 확인 텍스트 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close), // X 아이콘 필요
                    contentDescription = "닫기",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() } // 왼쪽 X 클릭 시 닫기
                )

                // 가운데 타이틀
                Text(
                    text = title,
                    style = b1_semibold_18,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier
                        .weight(1f),
                    textAlign = TextAlign.Center
                )

                // 우측 텍스트 버튼
                Text(
                    text = confirmText,
                    style = b1_semibold_18,
                    color = if (confirmEnabled) PurpleMain500 else Grey500,
                    modifier = Modifier
                        .clickable(enabled = confirmEnabled) {
                            onConfirm(text.trim())
                            onDismiss()
                        }
                        .padding(end = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)   // 컨테이너와의 가로 여백
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = b3_medium_14.copy(color = Grey1000),
                    cursorBrush = SolidColor(PurpleMain500),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (confirmEnabled) {
                                onConfirm(text.trim())
                                onDismiss()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFFF3F3F3), RoundedCornerShape(40.dp))
                        .border(1.dp, Color(0xFFBCBCBC), RoundedCornerShape(40.dp))
                        .padding(horizontal = 16.dp),             // 입력 내부 좌우 여백
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isBlank()) {
                                Text(
                                    text = placeholder,
                                    style = b3_medium_14,
                                    color = Color(0xFFBDBDBD)
                                )
                            }
                            inner()
                        }
                    }
                )
                // 유효성 검사 메시지
                Box(modifier = Modifier.height(24.dp)) {
                    if (text.isNotBlank() && text.trim().length < 2) {
                        validationContent?.invoke()
                    }
                }
            }
            Spacer(Modifier.height(20.dp)) // 하단 여백 약간 조정
        }
    }
}
