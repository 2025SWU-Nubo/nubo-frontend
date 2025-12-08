package com.example.nubo.ui.screen.myBoard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.nubo.ui.component.cardHeightForIndex
import com.example.nubo.ui.component.sheet.InviteSheet
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.example.nubo.ui.screen.add.SheetTopToast
import com.example.nubo.ui.theme.AppTextStyles.headline_regular_26
import com.example.nubo.ui.theme.AppTextStyles.subtitle_medium_16
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.model.card.CardItem
import com.example.nubo.utils.postRefreshTick
import getDisplayDate
import java.net.URLDecoder
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.components.toast.AppToastLayout
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.utils.REFRESH_TICK_KEY
import kotlinx.coroutines.launch
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles.b2_medium_16

// 어떤 다이얼로그를 띄울지 구분하기 위한 sealed class
sealed class InputDialogMode {
    data object CreateSection : InputDialogMode()
}

@Composable
fun BoardDetailScreen(
    boardId: Int,
    boardTitle: String,
    navController: NavController,
    source: String?,
    viewModel: BoardDetailViewModel = hiltViewModel(),
    // MyBoardScreen과 공유할 BoardViewModel 주입
    // Hilt Navigation Compose 라이브러리를 통해 이전 백스택의 ViewModel 인스턴스를 가져옴
    boardViewModel: BoardViewModel = hiltViewModel(remember { navController.previousBackStackEntry!! }),
    modifier: Modifier = Modifier
) {

    // 뷰모델 상태 올바르게 구독
    val ui by viewModel.ui.collectAsState()
    val boardState = ui.board

    // --- 선택 모드 관리를 위한 상태 변수 ---
    // 1. 선택 모드 활성화 여부
    var isSelectionMode by remember { mutableStateOf(false) }

    // 2. 선택된 섹션들의 ID를 저장하는 Set
    var selectedSections by remember { mutableStateOf(emptySet<Int>()) }

    // 3. 선택된 카드들의 ID를 저장하는 Set
    var selectedCards by remember { mutableStateOf(emptySet<Int>()) }
    // -----------------------------------------

    // --- 바텀 시트 상태 관리 ---
    var bottomSheetType by remember { mutableStateOf(BottomSheetType.NONE) }

    // --- 선택 모드 바텀바 관련 변수 ---
    var showBoardSelector by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<BoardAction?>(null) }
    val boardsState by viewModel.boards.collectAsState()

    // 선택 모드 진입 방법
    var selectionFromMenu by remember { mutableStateOf(false) }

    val resetSelectionState = {
        isSelectionMode = false
        showBoardSelector = false
        currentAction = null
        selectedSections = emptySet()
        selectedCards = emptySet()
        //바텀 시트 상태도 초기화
        bottomSheetType = BottomSheetType.NONE
        selectionFromMenu = false
    }
    // -----------------------------------------

    // 뒤로가기 버튼으로 선택 모드를 종료할 수 있도록 핸들러 추가
    BackHandler(enabled = isSelectionMode) {
        resetSelectionState()
    }

    //  다이얼로그 모드 상태
    var dialogMode by remember { mutableStateOf<InputDialogMode?>(null) }
    // 섹션 및 카드 삭제 다이얼로그
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 진입 시 한 번 초기 로드
    LaunchedEffect(boardId) {
        viewModel.init(boardId)
    }

    // 바텀바 상단 토스트
    var showShareWarning by remember { mutableStateOf(false) }
    var sheetHeight by remember { mutableStateOf(0) }   // 바텀시트 실제 높이(px)

    LaunchedEffect(bottomSheetType) {
        if (bottomSheetType != BottomSheetType.BOARD_MEMBERS) {
            showShareWarning = false
        }
    }

    // 토스트 상태 및 코루틴 스코프 선언
    val toastHost = LocalAppToastHostState.current
    val scope = rememberCoroutineScope()

    val toastMessage by viewModel.toastMessage.collectAsState()

    // --- ViewModel의 삭제 완료 이벤트를 구독하여 '개수'를 받아 액션 토스트 호출 ---
    LaunchedEffect(viewModel) {
        viewModel.deleteCompleteEvent.collect { count ->
            // 스낵바 대신 액션 토스트 사용 (섹션 생성 시 사용한 스타일 활용)
            scope.launch {
                toastHost.show(
                    title = AnnotatedString("${count}개의 항목이 삭제되었어요."),
                    layout = AppToastLayout.TitleWithAction,
                    type = AppToastType.NORMAL, // 삭제 알림은 Normal (또는 Positive 체크 아이콘 없이) 사용
                    actionLabel = "실행 취소",
                    onAction = {
                        scope.launch {
                            viewModel.undoLastDeletion()
                        }
                    }
                )
            }
            resetSelectionState()
        }
    }

    val toastEvent by viewModel.toastEvent.collectAsState()

    // 섹션 화면과 토스트 분기처리
    LaunchedEffect(toastEvent) {
        toastEvent?.let { (message, source) ->
            if (source != "section") {   //  섹션 관련 토스트는 무시
                scope.launch {
                    toastHost.show(
                        title = AnnotatedString(message),
                        layout = AppToastLayout.TitleOnly,
                        type = if (message.contains("실패")) AppToastType.NEGATIVE else AppToastType.POSITIVE
                    )
                }
            }
            viewModel.clearToastEvent()
        }
    }
    // view 모델 결과에 따른 토스트 출력
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->

            // 1) 섹션 생성 성공일 때만: 액션 버튼 토스트 + 섹션으로 이동
            if (message == "섹션 생성이 완료되었어요.") {

                // 현재 보드의 섹션 목록을 BoardItem 으로 변환 후, id 가 가장 큰 섹션 선택
                val latestSectionItem = ui.board
                    ?.sections
                    ?.map { it.toBoardItem() }          // ← SectionDto -> BoardItem
                    ?.maxByOrNull { it.id }

                scope.launch {
                    toastHost.show(
                        title = AnnotatedString(message),
                        layout = AppToastLayout.TitleWithAction,   // 액션 버튼 있는 레이아웃
                        actionLabel = "섹션으로 이동",
                        onAction = {
                            latestSectionItem?.let { section ->
                                // onSectionClick 의 else 분기 로직 그대로 사용
                                val encodedTitle =
                                    java.net.URLEncoder.encode(section.title, "utf-8")

                                val currentBoardTitle = ui.board?.name ?: boardTitle
                                val encodedBoardTitle =
                                    java.net.URLEncoder.encode(currentBoardTitle, "utf-8")

                                navController.navigate(
                                    "section_detail/${section.id}/$encodedTitle/$encodedBoardTitle"
                                )
                            }
                        }
                    )
                }
            } else {
                // 나머지 토스트는 기존 로직 그대로 유지
                val toastType = when {
                    message.contains("실패했어요") -> AppToastType.NEGATIVE
                    message.contains("즐겨찾기가 완료되었어요.") -> AppToastType.FAVORITE
                    message.contains("완료되었어요") ||
                        message.contains("취소되었어요.") ||
                        message.contains("취소가 완료되었어요.") ->
                        AppToastType.POSITIVE

                    else -> AppToastType.NORMAL
                }

                scope.launch {
                    toastHost.show(
                        title = AnnotatedString(message),
                        layout = AppToastLayout.TitleOnly,
                        type = toastType,
                    )
                }
            }

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
    // 섹션 상세에서 기능 사용 시 새로고침 신호 수신
    LaunchedEffect(Unit) {
        // 현재 화면의 SavedStateHandle을 가져옴
        val handle = navController.currentBackStackEntry?.savedStateHandle

        // "refresh_tick" (REFRESH_TICK_KEY) 키를 구독
        handle?.getStateFlow(REFRESH_TICK_KEY, 0L)
            ?.collect { tick ->
                // 0L (초기값)이 아닌 새로운 틱이 들어오면
                if (tick != 0L) {

                    // 강제 새로고침(forceRefresh = true)으로 init 호출
                    viewModel.init(boardId, forceRefresh = true)

                    // 신호를 처리한 후, 틱을 0L로 리셋하여 중복 새로고침 방지
                    handle.set(REFRESH_TICK_KEY, 0L)
                }
            }
    }
    // 공유 보드 초대 ViewModel 상태 구독
    val currentBoardMembers by viewModel.currentBoardMembers.collectAsState()
    val inviteResetSignal by viewModel.inviteResetSignal.collectAsState()

    // 공유 보드 멤버 상태 구독
    val activeMembers by viewModel.activeMembers.collectAsState()
    val pendingMembers by viewModel.pendingMembers.collectAsState()

    // 페이징 리스트 상태
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = Color.White,
            topBar = {
                DetailTopBar(
                    onBack = {
                        // 현재 보드의 최신 이름 전달
                        val latestName = ui.board?.name ?: boardTitle
                        navController.previousBackStackEntry?.savedStateHandle?.set("renamed_board_id", boardId)
                        navController.previousBackStackEntry?.savedStateHandle?.set("renamed_board_name", latestName)
                        // MyBoardScreen에 새로고침이 필요하다는 신호를 보냄
                        navController.postRefreshTick("myboard")

                        navController.popBackStack()

                    }, // 메뉴 버튼 클릭 시 메뉴 바텀바
                    onMenuClick = {
                        bottomSheetType = BottomSheetType.MENU
                    },
                    isSelectionMode = isSelectionMode,
                )
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    BoardTitleBar(
                        title = ui.board?.name ?: boardTitle,
                    )
                }
                stickyHeader {
                    Box(
                        Modifier
                            .background(Color.White)  // 중요 (안 하면 투명해짐)
                    ) {
                        // 즐겨찾기 필터만 뷰모델과 연결 (정렬 버튼은 UI만 유지, 서버 쿼리는 LATEST 고정)
                        BoardFilterButton(
                            favoriteSelected = ui.favoriteOnly,
                            onToggleFavorite = { enabled -> viewModel.setFavoriteFilter(enabled) },
                            onRequestSort = { sortKey -> viewModel.setSort(sortKey) },
                            isSelectionMode = isSelectionMode, // 선택 상태 변수 전달
                        )
                    }
                }
                item {
                    if (boardState != null) {
                        val boardItems = boardState.sections?.map { it.toBoardItem() } ?: emptyList()
                        // 페이징 래퍼에서 실제 리스트 꺼내기
                        val cardItems = boardState.cards.content.map { it.toCardItem() }
                        // 카드 배열 길이가 바뀌면 index 기반으로 높이 재생성
                        val cardHeights by remember(boardId, cardItems.size) {
                            mutableStateOf(
                                cardItems.mapIndexed { index, _ ->
                                    cardHeightForIndex(index)
                                }
                            )
                        }
                        // 공유 보드 여부
                        val isSharedBoard = boardState.shared

                        // 공유 보드인 경우에만 mine == true 인 카드들만 선택 가능하도록 ID 집합 구성
                        val selectableCardIds: Set<Int> =
                            if (isSharedBoard) {
                                boardState.cards.content
                                    .filter { it.mine == true } // mine 이 true 인 카드만
                                    .map { it.id }              // id(Int) 리스트로 변환
                                    .toSet()
                            } else {
                                emptySet()
                            }

                        // 보드에 섹션/카드가 아무것도 없을 때 빈 상태 UI 표시
                        if (boardItems.isEmpty() && cardItems.isEmpty()) {

                            val emptyMessage = if (ui.favoriteOnly) {
                                "즐겨찾기한 항목이 없어요."
                            } else {
                                "이 보드에는 아직 저장된 카드가 없어요!"
                            }

                            Box(
                                modifier = Modifier
                                    .padding(top = 190.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emptyMessage,
                                    style = b2_medium_16,
                                    color = GreyMain300,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                )
                            }
                        } else {
                            BoardDetailContent(
                                boardItems = boardItems,
                                cardItems = cardItems,
                                cardHeights = cardHeights,
                                onCardClick = { cardId ->
                                    // 카드 클릭 시 선택/해제 로직 추가
                                    if (isSelectionMode) {
                                        // 공유 보드 + mine == false 카드면 선택 불가
                                        if (isSharedBoard && !selectableCardIds.contains(cardId)) {
                                            // 선택 모드에서 남의 카드는 그냥 무시
                                            return@BoardDetailContent
                                        }
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

                                        // 현재 보드의 타이틀 가져오기
                                        val currentBoardTitle = ui.board?.name ?: boardTitle
                                        val encodedBoardTitle = java.net.URLEncoder.encode(currentBoardTitle, "utf-8")

                                        // 라우트에 boardTitle을 추가하여 전달
                                        navController.navigate("section_detail/${section.id}/$encodedTitle/$encodedBoardTitle")
                                    }
                                },
                                onCardLongClick = { cardId ->
                                    if (!isSelectionMode) {
                                        selectionFromMenu = false
                                        // 공유 보드에서 mine == false 카드면 선택 모드 진입 자체를 막음
                                        if (isSharedBoard && !selectableCardIds.contains(cardId)) {
                                            // 아무 동작도 하지 않음 (롱클릭 무시)
                                            return@BoardDetailContent
                                        }
                                        isSelectionMode = true
                                        bottomSheetType = BottomSheetType.SELECTION
                                        selectedCards = setOf(cardId) // 롱클릭한 카드를 첫 선택 항목으로 지정
                                    }
                                },
                                onSectionLongClick = { section ->
                                    if (!isSelectionMode) {
                                        selectionFromMenu = false
                                        isSelectionMode = true
                                        bottomSheetType = BottomSheetType.SELECTION
                                        selectedSections = setOf(section.id) // 롱클릭한 섹션을 첫 선택 항목으로 지정
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
                        }
                    } else {
                        // 로딩 인디케이터를 가운데 정렬하기 위해 Box 사용
                        Box(
                            modifier = Modifier.fillMaxSize(), // 1. 남은 공간을 모두 채움
                            contentAlignment = Alignment.Center // 2. 자식을 가운데 정렬
                        ) {
                            CircularProgressIndicator() // 3. 로딩 인디케이터
                        }
                    }
                }
            }
        }
        // 바텀바 뒤에 dim 표시
        val shouldShowDim =
            when (bottomSheetType) {
                BottomSheetType.NONE -> false

                BottomSheetType.SELECTION ->
                    showBoardSelector   // 선택 모드에서는 boardSelector가 열릴 때만 dim 표시

                else -> true           // 나머지 시트는 dim 표시
            }
        if (shouldShowDim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable {
                        // dim 클릭 시 닫기
                        if (bottomSheetType == BottomSheetType.SELECTION && showBoardSelector) {
                            // board selector만 닫기
                            showBoardSelector = false
                        } else {
                            bottomSheetType = BottomSheetType.NONE
                        }
                    }
            )
        }
        // 바텀 시트 로직을 bottomSheetType에 따라 분기하여 표시
        BottomSheetContainer(
            visible = bottomSheetType != BottomSheetType.NONE,
            onDismiss = {
                if (bottomSheetType == BottomSheetType.SELECTION) {
                    // 선택 모드 종료
                    resetSelectionState()
                    isSelectionMode = false
                }
                bottomSheetType = BottomSheetType.NONE
                showBoardSelector = false
            }
        ) {
            when (bottomSheetType) {
                BottomSheetType.NONE -> { /* empty */
                }

                BottomSheetType.SELECTION -> {
                    // 기존 선택 모드 바텀 시트
                    SelectionBottomBar(
                        isVisible = true, // AnimatedVisibility가 제어하므로 항상 true
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
                                },
                                onCancelClick = { resetSelectionState() },
                                onBack = {
                                    bottomSheetType = BottomSheetType.MENU
                                    resetSelectionState()
                                    isSelectionMode = false
                                },
                                // 메뉴에서 들어왔을 때만 뒤로가기 버튼 표시
                                showBackButton = selectionFromMenu
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
                }
                // INVITE 상태일 때 공유 보드 초대 화면
                BottomSheetType.INVITE -> {
                    InviteSheet(
                        onClose = {
                            // 아예 닫기 (또는 BOARD_EDIT로 갈지 결정)
                            bottomSheetType = BottomSheetType.NONE
                        },
                        onBack = { bottomSheetType = BottomSheetType.BOARD_MEMBERS },
                        onInvite = { /* 필요시 단일 초대 로직 */ },
                        onComplete = { emails, users ->
                            // 1) 초대 대상 임시 저장
                            viewModel.updateBoardMembers(emails, users)

                            // 2) 공유보드 전환이 필요한지 계산
                            //    → 현재 보드가 개인보드(shared == false)라면 공유 전환해야 함
                            val needShare = ui.board?.shared == false

                            // 3) 공유 전환 + 초대 API 통합 처리
                            viewModel.inviteAndShareIfNeeded(
                                draftIsShared = needShare,
                                onFinished = {
                                    bottomSheetType = BottomSheetType.BOARD_MEMBERS
                                }
                            )
                        },
                        resetSignal = inviteResetSignal,
                        // 이미 선택된 이메일들을 넘겨줌 (체크 상태 유지)
                        initialSelected = currentBoardMembers.map { it.email },
                        useTopPadding = true
                    )
                }
                // 참여자 목록 화면 연결
                BottomSheetType.BOARD_MEMBERS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { layoutCoordinates ->
                                sheetHeight = layoutCoordinates.size.height
                            }
                    ) {
                        BoardMembersSheet(
                            activeMembers = activeMembers,
                            pendingMembers = pendingMembers,
                            onBack = { bottomSheetType = BottomSheetType.MENU },
                            onCancelInvite = { invitationId ->
                                viewModel.cancelInvitation(invitationId)
                            },
                            isOwner = ui.board?.owner ?: false,
                            onInviteClick = { bottomSheetType = BottomSheetType.INVITE }
                        )
                        // 개인보드일 때만 바텀바 상단 토스트 표시
                        if (ui.board?.shared == false) {
                            LaunchedEffect(Unit) {
                                showShareWarning = true      // 토스트 보여주기
                            }
                        }
                    }
                }
                // 메뉴 버튼 클릭 시 메뉴 바텀바
                BottomSheetType.MENU -> {
                    ui.board?.let { currentBoard ->
                        MenuContent(
                            isSectionScreen = false,
                            source = source,
                            isShared = currentBoard.shared,
                            isOwner = currentBoard.owner,
                            isMine = ui.board?.mine ?: false,
                            onRenameClick = {
                                bottomSheetType = BottomSheetType.BOARD_RENAME
                            },
                            onMembersClick = {
                                viewModel.loadBoardMembers()
                                bottomSheetType = BottomSheetType.BOARD_MEMBERS
                            },
                            onAddSectionClick = {
                                bottomSheetType = BottomSheetType.SECTION_ADD
                            },
                            onSelectCardClick = {
                                isSelectionMode = true
                                bottomSheetType = BottomSheetType.SELECTION
                                selectionFromMenu = true
                            },
                            onSelectSectionClick = {
                                isSelectionMode = true
                                bottomSheetType = BottomSheetType.SELECTION
                                selectionFromMenu = true
                            },
                            onDismiss = { bottomSheetType = BottomSheetType.NONE }
                        )
                    }
                }
                // 섹션 추가 바텀바
                BottomSheetType.SECTION_ADD -> {
                    AddSection(
                        onBack = { bottomSheetType = BottomSheetType.MENU },
                        onDismiss = { bottomSheetType = BottomSheetType.NONE },
                        onConfirm = { newName ->
                            viewModel.createSection(newName)
                            bottomSheetType = BottomSheetType.NONE
                        }
                    )
                }
                // 보드 이름 변경 바텀바
                BottomSheetType.BOARD_RENAME -> {
                    ui.board?.let { currentBoard ->
                        BoardRename(
                            currentName = currentBoard.name,
                            isCurrentlyShared = currentBoard.shared,
                            onBack = { bottomSheetType = BottomSheetType.MENU },  // 메뉴로 돌아가기
                            onDismiss = { bottomSheetType = BottomSheetType.NONE }, // 시트 닫기
                            onConfirm = { newName, isShared ->
                                if (newName != currentBoard.name) {
                                    viewModel.renameCurrentBoard(newName) // 실제 보드 이름 변경 API
                                }
                                bottomSheetType = BottomSheetType.NONE
                            }
                        )
                    }
                }

                BottomSheetType.BOARD_SELECTION -> { // 전체 보드 탭 화면에서만 사용
                }

                BottomSheetType.SECTION_RENAME -> { // 섹션 내부에서 사용
                }
            }
        }
        LaunchedEffect(listState, ui.isLast, ui.isLoading) {
            snapshotFlow {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                Triple(lastVisible, total, ui.isLast)
            }.collect { (lastVisible, total, isLast) ->
                val reachedBottom = total > 0 && lastVisible >= total - 2 // 끝에서 2개 남았을 때
                if (reachedBottom && !ui.isLoading && !isLast) {
                    viewModel.loadNextPage()
                }
            }
        }
    }
    // --- 섹션 및 카드 삭제 다이얼로그 호출 코드 추가 ---
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            visible = true,
            selectedCardCount = selectedCards.size,
            selectedSectionCount = selectedSections.size,
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                scope.launch {
                    viewModel.deleteItems(selectedSections, selectedCards)
                    showDeleteDialog = false
                }
            }
        )
    }
    val density = LocalDensity.current
    val toastOffsetDp = with(density) { sheetHeight.toDp() + 16.dp }   // ③ 바텀시트 위로 띄우기

    if (showShareWarning) {
        SheetTopToast(
            title = buildAnnotatedString {
                append("공유 보드로 바꾸면 ")
                withStyle(SpanStyle(color = PurpleMain500)) {
                    append("개인 보드로 되돌릴 수 없어요.")
                }
            },
            message = "참여자를 초대하면 나중에 개인 보드로 바꿀 수 없어요.",
            visible = showShareWarning,
            onDismiss = { showShareWarning = false },
            durationMillis = 100000L,          // 토스트 지속 시간
            bottomOffset = toastOffsetDp       // 바텀시트 실제 높이 기반 offset
        )
    }
}

// 탑바
@Composable
fun DetailTopBar(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    isSelectionMode: Boolean
) {
    val titleText = "나의 보드"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 16.dp, top = 60.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽 (뒤로가기 버튼 + 타이틀)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "뒤로가기",
                tint = GreyMain300,
                modifier = Modifier.noRippleClickable { onBack() }
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = titleText,
                style = subtitle_medium_16,
                color = GreyMain300
            )
        }
        // 중간을 채우는 빈 공간
        Spacer(modifier = Modifier.weight(1f))

        // 오른쪽 (메뉴 버튼)
        Icon(
            painter = painterResource(id = R.drawable.ic_board_menu),
            contentDescription = "보드 메뉴",
            tint = GreyMain300,
            modifier = Modifier
                .noRippleClickable(enabled = !isSelectionMode) { onMenuClick() }
        )

    }
}

// 타이틀 바
@Composable
fun BoardTitleBar(title: String) {
    val decodedTitle = URLDecoder.decode(title, "utf-8")

    Column(modifier = Modifier.padding(top = 29.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = decodedTitle,
                style = headline_regular_26,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis, // 말줄임표 추가
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false) // 남는 공간만 차지
            )
        }
    }
}

// 정렬, 필터 버튼
@Composable
fun BoardFilterButton(
    favoriteSelected: Boolean,
    onToggleFavorite: (Boolean) -> Unit,
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
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 정렬/필터 버튼들
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 정렬 버튼
            SortFilterButton(
                selectedTab = 0,
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
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(if (isFavoriteSelected) R.drawable.selected_star else R.drawable.ic_filter_star),
                        contentDescription = "즐겨찾기",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// 섹션 아이템 변환
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

// 카드 아이템 변환
fun CardItemDto.toCardItem(): CardItem {
    return CardItem(
        id = this.id,
        height = 300.dp, // 기존 randomCardHeight() 함수 사용
        title = this.title ?: "No Title", // 서버 데이터 없을 경우 기본값
        category = this.category ?: "No Category", // 마찬가지
        description = this.description ?: "No Description",
        imageUrl = this.imageUrl ?: "",
        isFavorite = this.favorite ?: false
    )
}
