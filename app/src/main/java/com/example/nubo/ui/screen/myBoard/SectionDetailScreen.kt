package com.example.nubo.ui.screen.myBoard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.components.toast.AppToastHost
import com.example.components.toast.AppToastLayout
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.R
import com.example.nubo.data.model.CardItemDto
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.component.randomCardHeight
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.ui.theme.RedError
import kotlinx.coroutines.launch

@Composable
fun SectionDetailScreen(
    sectionId: Int,
    sectionTitle: String,
    navController: NavController,
    // BoardDetailViewModel을 재사용
    viewModel: BoardDetailViewModel = hiltViewModel(),
) {
    // 이름 변경 내용 기록
    var dialogMode by remember { mutableStateOf<InputDialogMode?>(null) }

    // ---  선택 모드 관리를 위한 상태 변수 ---
    var isSelectionMode by remember { mutableStateOf(false) }
    // 섹션 상세 화면에서는 카드만 선택 가능
    var selectedCards by remember { mutableStateOf(emptySet<Int>()) }

    // 바텀 시트 타입을 관리할 상태 추가
    var bottomSheetType by remember { mutableStateOf(BottomSheetType.NONE) }

    // --- 선택 모드 바텀바 관련 변수 ---
    var showBoardSelector by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<BoardAction?>(null) }
    val boardsState by viewModel.boards.collectAsState()

    val resetSelectionState = {
        isSelectionMode = false
        showBoardSelector = false
        currentAction = null
        selectedCards = emptySet()
        bottomSheetType = BottomSheetType.NONE
    }
    // -----------------------------------------\

    // 삭제 다이얼로그
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- 스낵바 및 토스트 상태 추가 ---
    val snackbarHostState = remember { SnackbarHostState() }
    // 토스트 상태 및 코루틴 스코프 선언
    val toastHostState = rememberAppToastHostState()
    val scope = rememberCoroutineScope()
    val toastMessage by viewModel.toastMessage.collectAsState()

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

    // '실행 취소' 스낵바를 띄우는 함수 추가
    fun showUndoSnackbar() {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "삭제가 완료되었습니다.",
                actionLabel = "실행 취소",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                // TODO: "실행 취소" 클릭 시 서버 연동 로직
            }
        }
    }

    // viewModel.init() 함수에 sectionId를 전달
    LaunchedEffect(sectionId) {
        viewModel.init(sectionId)
    }

    // ui.board 상태를 사용하여 섹션 이름과 카드 목록을 표시
    val ui by viewModel.ui.collectAsState()
    val detailState = ui.board

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
                // 패딩 조절된 TopBar 사용
                DetailTopBar(onBack = {
                    val latestName = ui.board?.name ?: sectionTitle
                    navController.previousBackStackEntry?.savedStateHandle?.set("renamed_section_id", sectionId)
                    navController.previousBackStackEntry?.savedStateHandle?.set("renamed_section_name", latestName)
                    navController.popBackStack()
                },// 메뉴 버튼 클릭 시 보드 설정 바텀 시트 표시
                    onMenuClick = { bottomSheetType = BottomSheetType.SECTION_SETTINGS },
                    isSelectionMode = isSelectionMode)
                // 패딩 조절된 TitleBar 사용
                BoardTitleBar(
                    title = detailState?.name ?: sectionTitle,)

                SectionFilterButton(
                    favoriteSelected = ui.favoriteOnly,
                    onToggleFavorite = { enabled -> viewModel.setFavoriteFilter(enabled) },
                    onSelectClick = {
                        if (isSelectionMode) resetSelectionState()
                        else {isSelectionMode = true
                              bottomSheetType = BottomSheetType.SELECTION}
                    },
                    onRequestSort = { sortKey -> viewModel.setSort(sortKey) },
                    isSelectionMode = isSelectionMode
                )

                if (ui.isLoading && detailState == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading...")
                    }
                } else if (detailState != null) {
                    val cardItems = detailState.cards.content.map { it.toMyCardItem() }
                    val cardHeights by remember(sectionId, cardItems.size) {
                        mutableStateOf(cardItems.map { randomCardHeight() })
                    }
                    ScrollableCardContent(
                        cards = cardItems,
                        cardHeights = cardHeights,
                        onCardClick = { cardId ->
                            if (isSelectionMode) {
                                selectedCards =
                                    if (selectedCards.contains(cardId)) selectedCards - cardId else selectedCards + cardId
                            } else {
                                navController.navigate("card_detail/$cardId")
                            }
                        },
                        isSelectionMode = isSelectionMode,
                        selectedCardIds = selectedCards
                    )
                }
            }
        }
        // 바텀 시트 로직을 bottomSheetType에 따라 분기하여 표시
        AnimatedVisibility(
            visible = bottomSheetType != BottomSheetType.NONE,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            when (bottomSheetType) {
                BottomSheetType.SELECTION -> {
                    // 기존 선택 모드 바텀 시트
                    SelectionBottomBar(
                        isVisible = true,
                        showBoardSelector = showBoardSelector,
                        actionsContent = {
                            ActionsContent(
                                selectedSectionCount = 0, // 섹션 상세에서는 카드만 선택
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
                                onConfirm = { selectedId ->
                                    selectedId?.let { targetId ->
                                        when (currentAction) {
                                            BoardAction.COPY -> {
                                                viewModel.copySelectedItems(
                                                    targetBoardId = targetId.toLong(),
                                                    selectedSectionIds = emptySet(),
                                                    selectedCardIds = selectedCards
                                                )
                                            }
                                            BoardAction.MOVE -> {
                                                viewModel.moveSelectedItems(
                                                    targetBoardId = targetId.toLong(),
                                                    selectedSectionIds = emptySet(),
                                                    selectedCardIds = selectedCards
                                                )
                                            }
                                            null -> {}
                                        }
                                    }
                                    resetSelectionState()
                                }
                            )
                        }
                    )
                }

                BottomSheetType.SECTION_SETTINGS -> {
                    // 새로 추가된 섹션 설정 바텀 시트
                    SectionSettingsContent(
                        onRenameClick = {
                            // 이름 변경 버튼 클릭 시 다이얼로그 띄우기
                            dialogMode = InputDialogMode.Rename(
                                sectionId = sectionId,
                                currentName = ui.board?.name ?: sectionTitle
                            )
                            // 바텀 시트 닫기
                            bottomSheetType = BottomSheetType.NONE
                        },
                        onDismiss = { bottomSheetType = BottomSheetType.NONE }
                    )
                }
                else -> {}
            }
        }
        // 이름 변경 다이얼로그
        when (val m = dialogMode) {
            is InputDialogMode.Rename -> NuboInputDialog(
                visible = true,
                title = "섹션 이름 변경",
                confirmText = "완료",
                placeholder = "새 이름",
                initialValue = m.currentName,
                // 이름 변경 시 viewModel.renameCurrentBoard(newName)를 호출
                onConfirm = { newName -> viewModel.renameCurrentBoard(newName = newName) },
                onDismiss = { dialogMode = null },
                // 유효성 검사 실패 시 보여줄 메시지 UI
                validationContent = {
                    Text(
                        text = "섹션 이름을 2자 이상 입력해주세요.",
                        style = AppTextStyles.b3_regular_14,
                        color = RedError,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp)
                    )
                }
            )

            else -> Unit // 섹션 생성 다이얼로그는 없음
        }
    }
    // 삭제 확인 다이얼로그
    DeleteConfirmationDialog(
        visible = showDeleteDialog,
        selectedCardCount = selectedCards.size,
        selectedSectionCount = 0,
        onDismiss = { showDeleteDialog = false },
        onRemove = {
            showDeleteDialog = false
            showUndoSnackbar()
        },
        onDelete = {
            showDeleteDialog = false
            showUndoSnackbar()
        }
    )
    // 토스트 UI를 화면에 배치
    AppToastHost(hostState = toastHostState)
}


// 필터 버튼
@Composable
fun SectionFilterButton(
    favoriteSelected: Boolean,
    onToggleFavorite: (Boolean) -> Unit,
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
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 정렬/필터 버튼들
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 정렬 버튼
            SortFilterButton(
                enabled = !isSelectionMode,//선택 모드일 때 버튼 비활성화
                onSortSelected = { sortKey -> onRequestSort(sortKey) }
            )

            // 즐겨찾기 버튼
            val isFavoriteSelected = selected == "즐겨찾기"
            OutlinedButton(
                onClick = {
                    val nextOn = !isFavoriteSelected
                    selected = if (nextOn) "즐겨찾기" else null
                    onToggleFavorite(nextOn)
                },
                enabled = !isSelectionMode, //선택 모드일 때 버튼 비활성화
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFavoriteSelected) Purple50 else Color.Transparent,
                    contentColor = if (isFavoriteSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (isFavoriteSelected) PurpleMain500 else Grey200),
                modifier = Modifier.height(35.dp),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "즐겨찾기", style = AppTextStyles.label_medium_12)
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_star),
                        contentDescription = "즐겨찾기",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // 오른쪽: '선택'/'취소' 버튼 UI
        val buttonText = if (isSelectionMode) "취소" else "선택"
        val containerColor = if (isSelectionMode) PurpleMain500 else Purple100.copy(alpha = 0.3f)
        val contentColor = if (isSelectionMode) Color.White else PurpleMain500

        Button(
            onClick = onSelectClick,
            shape = RoundedCornerShape(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                // 비활성화 상태에서도 활성화 색상과 동일하게 유지
                disabledContainerColor = Purple100.copy(alpha = 0.3f),
                disabledContentColor = PurpleMain500
            ),
            border = if (!isSelectionMode) BorderStroke(0.5.dp, PurpleMain500) else null,
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(text = buttonText, style = AppTextStyles.label_medium_12)
        }
    }
}

// CardItemDto를 MyCardItem으로 변환하는 확장 함수
fun CardItemDto.toMyCardItem(): MyCardItem {
    return MyCardItem(
        id = this.id,
        imageUrl = this.imageUrl ?: "",
        isFavorite = this.favorite ?: false
    )
}
