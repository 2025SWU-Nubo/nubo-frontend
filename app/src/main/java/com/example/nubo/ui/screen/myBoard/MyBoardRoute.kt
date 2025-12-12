package com.example.nubo.ui.screen.myBoard

import android.os.Parcelable
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.nubo.ui.screen.add.SheetTopToast
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.flow.collectLatest
import kotlinx.parcelize.Parcelize
import com.example.nubo.utils.refreshTicks
import androidx.compose.foundation.layout.Box

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

    var cardtab: Boolean = false

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

    // 보드 선택 안내 토스트 상태
    var showBoardSelectWarning by remember { mutableStateOf(false) }

    //안내 토스트 관련 변수
    var boardSheetHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

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

    // --- SavedStateHandle을 감시하여 삭제 이벤트 처리 ---

    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    // 스낵바 대신 ViewModel을 통해 액션 토스트 이벤트를 발생시킴
    DisposableEffect(lifecycleOwner, savedStateHandle) {

        // Int 대신 BoardDeleteEvent를 관찰
        val observer = Observer<BoardDeleteEvent> { event ->

            if (event != null && event.count > 0) {
                // Route에서는 직접 UI를 띄우지 않고 ViewModel에 위임
                boardViewModel.triggerDeleteToast(event.count, isBoard = true)

                // 이벤트 소비
                savedStateHandle?.remove<BoardDeleteEvent>("deleted_board_event")
            }
        }

        val liveData = savedStateHandle?.getLiveData<BoardDeleteEvent>("deleted_board_event")
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

    // --- 보드 상세 화면용 삭제 이벤트 수신 ---
    LaunchedEffect(boardDetailViewModel, cardViewModel) {
        boardDetailViewModel.deleteCompleteEvent.collect { count ->
            resetCardSelectionState()
            cardViewModel.refresh()

            // 스낵바 대신 액션 토스트 이벤트 트리거
            boardViewModel.triggerDeleteToast(count, isBoard = false)
        }
    }

    // --- '나의 카드' 탭용 카드 삭제 이벤트 수신 ---
    // BoardViewModel 내부에서 deleteCardsFromGlobal 성공 시 triggerDeleteToast를 호출하므로
    // 여기서는 UI 상태 초기화와 데이터 갱신만 담당
    LaunchedEffect(boardViewModel, cardViewModel) {
        boardViewModel.cardDeleteCompleteEvent.collect {
            // 1. 선택 모드 해제
            resetCardSelectionState()
            // 2. 카드 목록 새로고침
            cardViewModel.refresh()
            // (토스트는 ViewModel -> Screen으로 전달됨)
        }
    }

    // MyBoardRoute
    // BottomBar 충돌 문제 해결
    // --- Scaffold에는 modifier를 적용하지 않고, SnackbarHost에만 적용 ---
    Scaffold() { innerPadding ->
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
                        0 -> {// 카드 탭
                            // 필터/정렬 초기화 (MyCardViewModel에도 함수 추가 필요)
                            cardViewModel.resetFilterAndSort()
                            cardViewModel.refresh()
                        }

                        1 -> { // 보드 탭으로 이동
                            // 보드 탭의 필터/정렬 초기화
                            boardViewModel.resetFilterAndSort()
                            boardViewModel.refresh()
                        }
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
                val isShared = board.shared
                val isOwner = board.owner

                if (isBoardSelectionMode) {
                    // 공유 보드는 소유자만 선택 가능
                    if (isShared && !isOwner) {
                        showBoardSelectWarning = true
                        return@MyBoardScreen
                    }

                    val id = board.serverBoardId
                    selectedBoardIds =
                        if (selectedBoardIds.contains(id)) selectedBoardIds - id else selectedBoardIds + id
                } else {
                    val encodedTitle = java.net.URLEncoder.encode(board.title, "utf-8")
                    val route = "board_detail/${board.serverBoardId}/$encodedTitle?source=${board.source}"
                    navController.navigate(route)
                }
            },
            onBoardLongClick = { board ->
                val isShared = board.shared
                val isOwner = board.owner

                if (!isBoardSelectionMode) {
                    // 공유 보드는 소유자만 선택 모드 진입 가능
                    if (isShared && !isOwner) {
                        showBoardSelectWarning = true
                        return@MyBoardScreen
                    }

                    isBoardSelectionMode = true
                    selectedBoardIds = setOf(board.serverBoardId)
                    boardForEditing = board
                    boardBottomSheetType = BottomSheetType.BOARD_SELECTION
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
                        // 여기서 바로 토스트 트리거를 호출
                        boardViewModel.triggerDeleteToast(deletedCount, isBoard = true)

                        boardViewModel.refresh()
                    }
                }
                showBoardDeleteDialog = false
                resetBoardSelectionState() // 삭제 후 선택모드 해제
            }
        )
    }
    // 선택 모드 바텀바 컨테이너
    BottomSheetContainer(
        visible = isCardSelectionMode,
        onDismiss = { resetCardSelectionState() }
    ) {
        Box(
            modifier = Modifier.onGloballyPositioned { boardSheetHeightPx = it.size.height }
        ) {
            BoardSelectionContent(
                onDeleteClick = {
                    scope.launch { boardViewModel.deleteCardsFromGlobal(selectedCardIds) }
                },
                selectedBoardCount = 0,
                selectedCardCount = selectedCardIds.size,
                selectedSectionCount = 0,
                showBackButton = false,
                onBack = {/* 여기서는 뒤로가기 없음*/ }
            )
        }
    }

    BottomSheetContainer(
        visible = isBoardSelectionMode && boardBottomSheetType == BottomSheetType.BOARD_SELECTION,
        onDismiss = { resetBoardSelectionState() }
    ) {
        Box(
            modifier = Modifier.onGloballyPositioned { boardSheetHeightPx = it.size.height }
        ) {
            BoardSelectionContent(
                onDeleteClick = {
                    boardIdsToDelete = selectedBoardIds
                    showBoardDeleteDialog = true
                    boardBottomSheetType = BottomSheetType.NONE
                },
                selectedBoardCount = selectedBoardIds.size,
                selectedCardCount = 0,
                selectedSectionCount = 0,
                showBackButton = false,
                onBack = {/* 여기서는 뒤로가기 없음*/ }
            )
        }
    }
    if (showBoardSelectWarning) {
        SheetTopToast(
            title = buildAnnotatedString {
                append("공유 보드 삭제는 ")
                withStyle(SpanStyle(color = PurpleMain500)) { append("보드 소유자만 ") }
                append("가능해요.")
            },
            message = "일반 참여자는 삭제할 수 없어요.",
            visible = showBoardSelectWarning,
            onDismiss = { showBoardSelectWarning = false },
            durationMillis = 3500L,
            bottomOffset = 184.dp
        )
    }
}

@Parcelize // 1. @Parcelize 어노테이션 추가
private data class BoardDeleteEvent(
    val count: Int,
    val id: Long = System.currentTimeMillis()
) : Parcelable // 2. : Parcelable 인터페이스 구현
