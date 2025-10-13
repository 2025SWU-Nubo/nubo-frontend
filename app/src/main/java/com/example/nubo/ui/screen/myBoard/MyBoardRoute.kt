package com.example.nubo.ui.screen.myBoard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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

    var isCardSelectionMode by remember { mutableStateOf(false) }
    var selectedCardIds by remember { mutableStateOf(emptySet<Int>()) }

    val boardsState by boardDetailViewModel.boards.collectAsState()
    var showBoardSelector by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<BoardAction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val resetCardSelectionState = {
        isCardSelectionMode = false
        selectedCardIds = emptySet()
        showBoardSelector = false
        currentAction = null
        onSelectionModeChange(false) // 선택모드 종료를 부모에게 알림
    }

    // 뒤로가기 핸들러
    BackHandler(enabled = isCardSelectionMode) {
        resetCardSelectionState()
    }

    // --- 실행 취소 스낵바 상태를 Route에서 관리 ---
    val snackbarHostState = remember { SnackbarHostState() }

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
            }
        )
    }

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
}
