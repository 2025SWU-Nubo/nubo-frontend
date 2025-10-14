package com.example.nubo.ui.screen.myBoard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp

/**
 * MyBoardScreen과 관련된 모든 상태와 로직을 관리하는 컨테이너 컴포저블.
 * MainScreen에서 호출
 */
@Composable
fun MyBoardRoute(
    navController: NavController,
    // MainScreen의 Scaffold가 제공하는 innerPadding을 받아서 내부 Scaffold에 적용
    modifier: Modifier = Modifier,
    // MainScreen의 기본 BottomNavBar를 숨길지 여부를 알리기 위한 콜백
    onSelectionModeChange: (Boolean) -> Unit
) {

    val boardDetailViewModel: BoardDetailViewModel = hiltViewModel()
    val cardViewModel: MyCardViewModel = hiltViewModel() // MyBoardScreen에 필요
    val boardViewModel: BoardViewModel = hiltViewModel() // MyBoardScreen에 필요

    // --- 카드 선택 모드 상태 ---
    var isCardSelectionMode by remember { mutableStateOf(false) }
    var selectedCardIds by remember { mutableStateOf(emptySet<Int>()) }

    // --- 보드 선택 모드 상태 ---
    var isBoardSelectionMode by remember { mutableStateOf(false) }
    var selectedBoardIds by remember { mutableStateOf(emptySet<Int>()) }

    // --- 보드 옵션 바텀 시트 상태 ---
    var boardBottomSheetType by remember { mutableStateOf(BottomSheetType.NONE) }
    var boardForEditing by remember { mutableStateOf<com.example.nubo.model.myBoard.BoardItem?>(null) }


    // --- 카드 선택용 바텀바 상태 ---
    val boardsState by boardDetailViewModel.boards.collectAsState()
    var showBoardSelector by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<BoardAction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- 보드 삭제 다이얼로그 상태 ---
    var showBoardDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // --- [추가] ModalBottomSheet 상태 관리 ---
    @OptIn(ExperimentalMaterial3Api::class)
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // 부분적으로 확장되는 상태 스킵 (항상 전체 확장)
    )

    // --- 카드 선택 상태 초기화 ---
    val resetCardSelectionState = {
        isCardSelectionMode = false
        selectedCardIds = emptySet()
        showBoardSelector = false
        currentAction = null
        onSelectionModeChange(false) // 선택모드 종료를 부모에게 알림
    }

    // --- 보드 선택 상태 초기화 ---
    val resetBoardSelectionState = {
        isBoardSelectionMode = false
        selectedBoardIds = emptySet()
        boardBottomSheetType = BottomSheetType.NONE
        boardForEditing = null
        onSelectionModeChange(false)
    }

    // 뒤로가기 핸들러
    BackHandler(enabled = isCardSelectionMode) {
        resetCardSelectionState()
    }

    // --- 다른 화면에서 돌아왔을 때 새로고침을 처리하는 로직 ---
    LaunchedEffect(Unit) {
        // MyBoardRoute의 SavedStateHandle에서 "needs_refresh" 값을 관찰
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getLiveData<Boolean>("needs_refresh")?.observeForever { needsRefresh ->
            if (needsRefresh) {
                // 나의 보드 탭과 나의 카드 탭의 데이터를 모두 새로고침
                boardViewModel.refresh()
                cardViewModel.refresh()

                // 신호를 처리한 후에는 반드시 제거하여 중복 새로고침 방지
                handle.remove<Boolean>("needs_refresh")
            }
        }
    }


    // --- 실행 취소 스낵바 상태를 Route에서 관리 ---
    val snackbarHostState = remember { SnackbarHostState() }

    // --- SavedStateHandle을 감시하여 스낵바 표시 ---
    LaunchedEffect(Unit) {
        // 현재 화면(MyBoardRoute)의 SavedStateHandle에서 "deleted_board_count" 값을 관찰
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("deleted_board_count")?.observeForever { count ->
                if (count > 0) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "${count}개의 보드가 삭제되었습니다.",
                            actionLabel = "실행 취소",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            boardViewModel.undoLastDeletion()
                        }
                    }
                    // [중요] 스낵바를 띄운 후에는 반드시 값을 제거하여
                    // 화면이 다시 그려질 때(예: 화면 회전) 스낵바가 또 뜨는 것을 방지
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Int>("deleted_board_count")
                }
            }
    }

    // --- 삭제 이벤트 처리 로직을 하나로 통합 및 개선 ---
    LaunchedEffect(boardDetailViewModel, cardViewModel) {
        boardDetailViewModel.deleteCompleteEvent.collect { count ->
            resetCardSelectionState()
            cardViewModel.refresh()

            val result = snackbarHostState.showSnackbar(
                message = "${count}개의 카드가 삭제되었습니다.",
                actionLabel = "실행 취소",
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                // undoLastDeletion이 suspend 함수이므로, 이 작업이 끝날 때까지 대기
                boardDetailViewModel.undoLastDeletion()
                // 복구가 완료된 후, 목록을 새로고침
                cardViewModel.refresh()
            }
        }
    }

    // MyBoardRoute
    // BottomBar 충돌 문제 해결
    // --- Scaffold에는 modifier를 적용하지 않고, SnackbarHost에만 적용 ---
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 60.dp)

            ) { snackbarData ->
                UndoSnackbar(
                    message = snackbarData.visuals.message,
                    onUndo = {
                        snackbarData.performAction()
                        snackbarData.dismiss()
                    }
                )
            }
        },
        bottomBar = {
            if (isCardSelectionMode) {
                SelectionBottomBar(
                    isVisible = true,
                    showBoardSelector = showBoardSelector,
                    actionsContent = {
                        ActionsContent(
                            selectedSectionCount = 0,
                            selectedCardCount = selectedCardIds.size,
                            onDeleteClick = {
                                scope.launch {
                                    boardDetailViewModel.deleteItems(emptySet(), selectedCardIds)
                                }
                            },
                            onCopyClick = {
                                currentAction = BoardAction.COPY
                                showBoardSelector = true
                                boardDetailViewModel.loadBoards()
                            },
                            onMoveClick = {
                                currentAction = BoardAction.MOVE
                                showBoardSelector = true
                                boardDetailViewModel.loadBoards()
                            },
                            onCancelClick = { resetCardSelectionState() }
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
                                        // --- boardViewModel의 함수를 호출 ---
                                        BoardAction.COPY -> boardViewModel.copyCardsFromGlobal(
                                            targetBoardId = targetId.toLong(),
                                            selectedCardIds = selectedCardIds
                                        )
                                        BoardAction.MOVE -> boardViewModel.moveCardsFromGlobal(
                                            targetBoardId = targetId.toLong(),
                                            selectedCardIds = selectedCardIds
                                        )
                                        null -> {}
                                    }
                                }
                                resetCardSelectionState()
                            }
                        )
                    }
                )
            }
            // --- 보드 선택 모드 바텀바 ---
            if (isBoardSelectionMode) {
                when (boardBottomSheetType) {
                    BottomSheetType.BOARD_SETTINGS -> {
                        BoardSettingsContent(
                            onDeleteClick = {
                                showBoardDeleteDialog = true
                                boardBottomSheetType = BottomSheetType.NONE
                            },
                            onSettingsClick = {
                                boardBottomSheetType = BottomSheetType.BOARD_EDIT
                            },
                            onDismiss = { resetBoardSelectionState() }
                        )
                    }
                    BottomSheetType.BOARD_EDIT -> {
                        boardForEditing?.let { board ->
                            BoardEditSheet(
                                source = board.source,
                                currentName = board.title,
                                isCurrentlyShared = false, // MyBoard에서는 공유 여부 알 수 없으므로 false로 고정
                                onBack = {
                                    boardBottomSheetType = BottomSheetType.BOARD_SETTINGS
                                },
                                onInviteClick = { /* TODO */ },
                                onConfirm = { newName, isShared ->
                                    if (newName != board.title) {
                                        boardViewModel.renameBoard(board.serverBoardId, newName)
                                        boardViewModel.applyRename(board.serverBoardId, newName) // UI 즉시 반영
                                    }
                                    resetBoardSelectionState()
                                }
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    ) { innerPadding ->
        MyBoardScreen(
            // --- MyBoardScreen에는 내부 Scaffold의 innerPadding만 전달 ---
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            cardViewModel = cardViewModel,
            boardViewModel = boardViewModel,
            boardDetailViewModel = boardDetailViewModel,
            isCardSelectionMode = isCardSelectionMode,
            selectedCardIds = selectedCardIds,
            onCardClick = { cardId ->
                if (isCardSelectionMode) {
                    selectedCardIds =
                        if (selectedCardIds.contains(cardId)) selectedCardIds - cardId
                        else selectedCardIds + cardId
                } else {
                    navController.navigate("card_detail/$cardId")
                }
            },
            onCardLongClick = { cardId ->
                if (!isCardSelectionMode) {
                    isCardSelectionMode = true
                    selectedCardIds = setOf(cardId)
                    onSelectionModeChange(true) // 선택모드 시작을 부모에게 알림
                }
            },
            // --- [추가] 보드 선택 모드 관련 파라미터 전달 ---
            isBoardSelectionMode = isBoardSelectionMode,
            selectedBoardIds = selectedBoardIds,
            onBoardClick = { board ->
                if (isBoardSelectionMode) {
                    // 선택모드에서는 클릭으로 선택/해제
                    val id = board.serverBoardId
                    selectedBoardIds = if (selectedBoardIds.contains(id)) selectedBoardIds - id else selectedBoardIds + id
                } else {
                    // 일반 모드에서는 상세 화면으로 이동
                    navController.navigate(
                        "board_detail/${board.serverBoardId}/${
                            java.net.URLEncoder.encode(board.title, "utf-8")
                        }/${board.source}"
                    )
                }
            },
            onBoardLongClick = { board ->
                if (!isBoardSelectionMode) {
                    isBoardSelectionMode = true
                    selectedBoardIds = setOf(board.serverBoardId)
                    boardForEditing = board
                    boardBottomSheetType = BottomSheetType.BOARD_SETTINGS
                    onSelectionModeChange(true)
                }
            }
        )
    }
    // 카드 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            visible = true,
            selectedCardCount = selectedCardIds.size,
            selectedSectionCount = 0,
            onDismiss = { showDeleteDialog = false },
            onRemove = {
                scope.launch {
                    boardDetailViewModel.removeItemsFromBoard(emptySet(), selectedCardIds)
                    showDeleteDialog = false
                    resetCardSelectionState()
                }
            },
            onDelete = {
                scope.launch {
                    boardDetailViewModel.deleteItems(emptySet(), selectedCardIds)
                    showDeleteDialog = false
                    resetCardSelectionState()
                }
            }
        )
    }
    // --- 보드 삭제 확인 다이얼로그 ---
    if (showBoardDeleteDialog) {
        BoardDeleteConfirmationDialog(
            visible = true,
            onDismiss = {
                showBoardDeleteDialog = false
                resetBoardSelectionState() // 다이얼로그 닫을 때 선택모드 해제
            },
            onDelete = {
                scope.launch {
                    val deletedCount = boardViewModel.deleteBoards(selectedBoardIds)
                    if (deletedCount != null && deletedCount > 0) {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("deleted_board_count", deletedCount)
                    }
                }
                showBoardDeleteDialog = false
                resetBoardSelectionState() // 삭제 후 선택모드 해제
            }
        )
    }
}
