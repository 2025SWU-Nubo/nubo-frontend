package com.example.nubo.ui.screen.myBoard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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

/**
 * MyBoardScreen과 관련된 모든 상태와 로직을 관리하는 컨테이너 컴포저블.
 * MainScreen은 이제 이 컴포저블만 호출하면 됩니다.
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

    // MyBoardRoute
    // BottomBar 충돌 문제 해결
    Scaffold(
        bottomBar = {
            if (isCardSelectionMode) {
                SelectionBottomBar(
                    isVisible = true,
                    showBoardSelector = showBoardSelector,
                    actionsContent = {
                        ActionsContent(
                            selectedSectionCount = 0,
                            selectedCardCount = selectedCardIds.size,
                            onDeleteClick = { showDeleteDialog = true },
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
                                        BoardAction.COPY -> boardDetailViewModel.copySelectedItems(
                                            targetBoardId = targetId.toLong(),
                                            selectedSectionIds = emptySet(),
                                            selectedCardIds = selectedCardIds
                                        )
                                        BoardAction.MOVE -> boardDetailViewModel.moveSelectedItems(
                                            targetBoardId = targetId.toLong(),
                                            selectedSectionIds = emptySet(),
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
            modifier = modifier.padding(innerPadding), // 부모의 패딩과 자신의 패딩을 모두 적용
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
