package com.example.nubo.ui.screen.myBoard

import android.os.Parcelable
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.parcelize.Parcelize
import com.example.nubo.utils.refreshTicks


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

    // selectedTab 상태를 MyBoardRoute에서 관리
    var selectedTab by rememberSaveable { mutableStateOf(1) } // 1 = 보드 탭

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

    // 보드 삭제 시 삭제할 ID를 임시 저장할 변수
    var boardIdsToDelete by remember { mutableStateOf(emptySet<Int>()) }

    val scope = rememberCoroutineScope()

    // --- ModalBottomSheet 상태 관리 ---
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

    // --- 실행 취소 스낵바 상태를 Route에서 관리 ---
    val snackbarHostState = remember { SnackbarHostState() }

    // --- SavedStateHandle을 감시하여 스낵바 표시 (수정된 버전) ---

    // 1. 현재 컴포저블의 생명주기(LifecycleOwner)를 가져옴.
    val lifecycleOwner = LocalLifecycleOwner.current
    // 2. SavedStateHandle을 한 번만 가져옴
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    // 3. LaunchedEffect 대신 DisposableEffect를 사용
    //    (lifecycleOwner나 savedStateHandle이 변경될 때마다 이펙트를 재실행)
    DisposableEffect(lifecycleOwner, savedStateHandle) {

        // Int 대신 BoardDeleteEvent를 관찰
        val observer = Observer<BoardDeleteEvent> { event ->

            // event가 null이 아니고 count가 0보다 클 때만 실행
            if (event != null && event.count > 0) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "${event.count}개의 보드가 삭제되었습니다.", // <--- event.count 사용
                        actionLabel = "실행 취소",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        boardViewModel.undoLastDeletion()
                    }

                    // 스낵바 처리가 끝난 후, 코루틴 '안'에서 이벤트를 제거 ( 소비)
                    savedStateHandle?.remove<BoardDeleteEvent>("deleted_board_event") // <---
                }
            }
        }

        // 새로운 키와 타입으로 LiveData를 구독
        val liveData = savedStateHandle?.getLiveData<BoardDeleteEvent>("deleted_board_event") // <---
        liveData?.observe(lifecycleOwner, observer)

        onDispose {
            liveData?.removeObserver(observer)
        }
    }

    // --- 다른 화면에서 돌아왔을 때 새로고침을 처리하는 로직 ---
    // 본인 라우트("myboard")의 SavedStateHandle
    val handle = remember(navController) {
        navController.getBackStackEntry("myboard").savedStateHandle
    }

    // tick 기반 새로고침(이 Route 한 곳에서만 소비)
    LaunchedEffect(Unit) {
        handle.refreshTicks().collectLatest { tick ->
            if (tick != 0L) {
                boardViewModel.refresh()
                cardViewModel.refresh()

                //  초기화는 선택사항(다음 tick은 항상 새로운 값이라 중복 트리거 없음)
                // handle[REFRESH_TICK_KEY] = 0L
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
                                // 현재 ID 목록을 새 변수에 캡처
                                boardIdsToDelete = selectedBoardIds

                                showBoardDeleteDialog = true
                                // 바텀시트를 닫도록 명시 (이래야 onDismiss가 호출됨)
                                boardBottomSheetType = BottomSheetType.NONE
                            },
                            onDismiss = { resetBoardSelectionState() }
                        )
                    }

                    BottomSheetType.BOARD_EDIT -> {
                        boardForEditing?.let { board ->
                            BoardEditSheet(
                                modifier = Modifier.imePadding(),
                                source = board.source,
                                currentName = board.title,
                                isCurrentlyShared = false, // MyBoard에서는 공유 여부 알 수 없으므로 false로 고정
                                onDismiss = {
                                    boardBottomSheetType = BottomSheetType.NONE
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
            // 상태와 람다 전달
            selectedTab = selectedTab,
            onTabSelected = { newTab ->
                // 탭이 실제로 변경될 때만 새로고침
                if (selectedTab != newTab) {
                    when (newTab) {
                        0 -> cardViewModel.refresh() // 카드 탭
                        1 -> boardViewModel.refresh() // 보드 탭
                    }
                }
                selectedTab = newTab
            },
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
            // --- 보드 선택 모드 관련 파라미터 전달 ---
            isBoardSelectionMode = isBoardSelectionMode,
            selectedBoardIds = selectedBoardIds,
            onBoardClick = { board ->
                if (isBoardSelectionMode) {
                    // 선택모드에서는 클릭으로 선택/해제
                    val id = board.serverBoardId
                    selectedBoardIds =
                        if (selectedBoardIds.contains(id)) selectedBoardIds - id else selectedBoardIds + id
                } else {
                    // 일반 모드에서 상세 화면으로 이동 시, source를 쿼리 파라미터로 전달
                    val encodedTitle = java.net.URLEncoder.encode(board.title, "utf-8")
                    val route = "board_detail/${board.serverBoardId}/$encodedTitle?source=${board.source}"
                    navController.navigate(route)
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
                Log.d("MyBoardRouteDebug", "onBoardLongClick triggered: Board ID = ${board.serverBoardId}, Title = ${board.title}")
            }
        )
    }
    // 카드 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            visible = true,
            selectedCardCount = selectedCardIds.size,
            selectedSectionCount = 0,
            onDismiss = {
                showBoardDeleteDialog = false
                resetBoardSelectionState() // 다이얼로그 닫을 때 선택모드 해제
                boardIdsToDelete = emptySet() // 임시 변수 초기화
            },
            onDelete = {
                scope.launch {
                    // selectedBoardIds 대신 캡처해둔 boardIdsToDelete 사용
                    val deletedCount = boardViewModel.deleteBoards(boardIdsToDelete)
                    if (deletedCount != null && deletedCount > 0) {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("deleted_board_count", deletedCount)
                    }
                }
                showBoardDeleteDialog = false
                resetBoardSelectionState() // 삭제 후 선택모드 해제
                boardIdsToDelete = emptySet() // 임시 변수 초기화
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
                    val deletedCount = boardViewModel.deleteBoards(boardIdsToDelete)
                    if (deletedCount != null && deletedCount > 0) {
                        // Int 대신 고유한 ID를 가진 Event 객체를 set
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("deleted_board_event", BoardDeleteEvent(count = deletedCount))

                        // 스낵바가 뜨는 것과 별개로 목록 새로고침 시작
                        boardViewModel.refresh()
                    }
                }
                showBoardDeleteDialog = false
                resetBoardSelectionState() // 삭제 후 선택모드 해제
            }
        )
    }
}

@Parcelize // 1. @Parcelize 어노테이션 추가
private data class BoardDeleteEvent(
    val count: Int,
    val id: Long = System.currentTimeMillis()
) : Parcelable // 2. : Parcelable 인터페이스 구현
