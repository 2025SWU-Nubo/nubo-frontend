package com.example.nubo.ui.screen.myBoard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState
import com.example.nubo.R
import com.example.nubo.data.model.CardItemDto
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.component.cardHeightForIndex
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.component.MyCardContent
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.AppTextStyles.subtitle_medium_16
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.screen.add.SheetTopToast
import com.example.nubo.utils.REFRESH_TICK_KEY
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun SectionDetailScreen(
    sectionId: Int,
    sectionTitle: String,
    navController: NavController,
    boardTitle: String,
    // BoardDetailViewModel을 재사용
    viewModel: BoardDetailViewModel = hiltViewModel(),
) {
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

    // 선택 모드 진입 방법
    var selectionFromMenu by remember { mutableStateOf(false) }

    val resetSelectionState = {
        isSelectionMode = false
        showBoardSelector = false
        currentAction = null
        selectedCards = emptySet()
        bottomSheetType = BottomSheetType.NONE
        selectionFromMenu = false
    }
    // 카드 선택 안내 토스트 변수
    var showSelectWarning by remember { mutableStateOf(false) }
    var selectWarningType by remember { mutableStateOf(SelectWarningType.CARD) }

    val triggerSelectWarning: (SelectWarningType) -> Unit = { type ->
        selectWarningType = type
        showSelectWarning = true
    }
    val toastOffsetDp = 204.dp

    // -----------------------------------------\

    // 뒤로가기 버튼으로 선택 모드를 종료할 수 있도록 핸들러 추가
    BackHandler(enabled = isSelectionMode) {
        resetSelectionState()
    }
    // 삭제 다이얼로그
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 토스트 상태 및 코루틴 스코프 선언
    val scope = rememberCoroutineScope()

    // 전역 토스트 사용
    val toastHost = LocalAppToastHostState.current
    val toastMessage by viewModel.toastMessage.collectAsState()

    val toastEvent by viewModel.toastEvent.collectAsState()

    // 보드 상세 화면과 토스트 분기 처리
    LaunchedEffect(toastEvent) {
        toastEvent?.let { (message, source) ->
            if (source == "section") {   // 섹션 관련 메시지만 표시
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
    // ViewModel의 toastMessage 변경을 감지하여 토스트 표시
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            val toastType = when {
                message.contains("실패했어요") -> AppToastType.NEGATIVE
                message.contains("완료되었어요") -> AppToastType.POSITIVE
                else -> AppToastType.NORMAL
            }
            scope.launch {
                toastHost.show(
                    title = AnnotatedString(message),
                    layout = AppToastLayout.TitleOnly,
                    type = toastType,
                )
            }
            // 토스트를 띄운 후에는 상태를 다시 null로 초기화하여 중복 표시 방지
            viewModel.clearToastMessage()
        }
    }

    // --- ViewModel의 삭제 완료 이벤트를 구독하여 액션 토스트 호출 및 상태 초기화 ---
    LaunchedEffect(viewModel) {
        viewModel.deleteCompleteEvent.collect { count ->
            scope.launch {
                // 삭제 알림은 Action Toast 사용 (스낵바 대체)
                toastHost.show(
                    title = AnnotatedString("${count}개의 항목이 삭제되었어요."),
                    layout = AppToastLayout.TitleWithAction,
                    type = AppToastType.NORMAL,
                    actionLabel = "실행 취소",
                    onAction = {
                        scope.launch {
                            viewModel.undoLastDeletion()
                        }
                    }
                )
            }
            // 토스트가 뜬 후 선택 모드를 해제
            resetSelectionState()
        }
    }

    // viewModel.init() 함수에 sectionId를 전달
    LaunchedEffect(sectionId) {
        viewModel.init(sectionId)
    }

    // ui.board 상태를 사용하여 섹션 이름과 카드 목록을 표시
    val ui by viewModel.ui.collectAsState()
    val detailState = ui.board

    val lazyListState = rememberLazyListState()

    // 카드 무한 스크롤 감지 로직
    LaunchedEffect(lazyListState, ui.isLoading, ui.isLast) {
        snapshotFlow { lazyListState.canScrollForward }
            .distinctUntilChanged()
            .collect { canScrollForward ->
                if (!canScrollForward && !ui.isLoading && !ui.isLast) {
                    viewModel.loadNextPage()
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                SectionDetailTopBar(
                    title = boardTitle, // 전달받은 boardTitle 사용
                    onBack = {
                        val latestName = ui.board?.name ?: sectionTitle
                        navController.previousBackStackEntry?.savedStateHandle?.set("renamed_section_id", sectionId)
                        navController.previousBackStackEntry?.savedStateHandle?.set("renamed_section_name", latestName)

                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(REFRESH_TICK_KEY, System.currentTimeMillis())

                        navController.popBackStack()
                    },// 메뉴 버튼 클릭 시 메뉴 바텀 시트 표시
                    onMenuClick = { bottomSheetType = BottomSheetType.MENU },
                    isSelectionMode = isSelectionMode
                )
            }

        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1) 타이틀바 (스크롤되면 위로 사라짐)
                item {
                    BoardTitleBar(
                        title = detailState?.name ?: sectionTitle,
                    )
                }

                // 2) 필터 영역 (sticky header)
                stickyHeader {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        SectionFilterButton(
                            favoriteSelected = ui.favoriteOnly,
                            onToggleFavorite = { enabled -> viewModel.setFavoriteFilter(enabled) },
                            onRequestSort = { sortKey -> viewModel.setSort(sortKey) },
                            isSelectionMode = isSelectionMode
                        )
                    }
                }

                // 3) 카드 Masonry 전체 스크롤
                item {
                    if (ui.isLoading && detailState == null) {
                        // 로딩 인디케이터를 가운데 정렬하기 위해 Box 사용
                        Box(
                            modifier = Modifier.fillMaxSize(), // 1. 남은 공간을 모두 채움
                            contentAlignment = Alignment.Center // 2. 자식을 가운데 정렬
                        ) {
                            CircularProgressIndicator() // 3. 로딩 인디케이터
                        }
                    } else if (detailState != null) {
                        val cardDtos = detailState.cards.content
                        val cardItems = cardDtos.map { it.toMyCardItem() }

                        val isShared = ui.board?.shared == true

                        // 카드별 mine 기반으로 선택 가능 ID 계산
                        val selectableCardIds: Set<Int>? =
                            if (isShared) {
                                cardDtos
                                    .filter { it.mine == true } // 서버에서 내려주는 카드 mine
                                    .map { it.id }
                                    .toSet()
                            } else {
                                null // 개인 보드면 모두 선택 가능
                            }

                        val cardHeights by remember(sectionId, cardItems.size) {
                            mutableStateOf(
                                cardItems.mapIndexed { index, _ ->
                                    cardHeightForIndex(index)
                                }
                            )
                        }
                        // 섹션에 카드가 아무것도 없을 때 빈 상태 UI 표시
                        if (cardItems.isEmpty()) {

                            val emptyMessage = if (ui.favoriteOnly) {
                                "즐겨찾기한 항목이 없어요."
                            } else {
                                "이 섹션에는 아직 저장된 카드가 없어요!"
                            }

                            Box(
                                modifier = Modifier
                                    .padding(top = 190.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emptyMessage,
                                    style = AppTextStyles.b2_medium_16,
                                    color = GreyMain300,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                )
                            }
                        } else {
                            MyCardContent(
                                cards = cardItems,
                                cardHeights = cardHeights,
                                onCardClick = { cardId ->
                                    if (isSelectionMode) {
                                        if (isShared && selectableCardIds?.contains(cardId) == false) {
                                            triggerSelectWarning(SelectWarningType.CARD)
                                            return@MyCardContent         // 선택 토글 금지
                                        }

                                        selectedCards =
                                            if (selectedCards.contains(cardId)) selectedCards - cardId
                                            else selectedCards + cardId
                                    } else {
                                        navController.navigate("card_detail/$cardId")
                                    }
                                },
                                onCardLongClick = null,
                                isSelectionMode = isSelectionMode,
                                selectedCardIds = selectedCards,
                                selectableCardIds = selectableCardIds
                            )
                        }
                    }
                }
                // 4) 로딩 인디케이터
                if (ui.isLoading && detailState != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
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
                        if (bottomSheetType == BottomSheetType.SELECTION && showBoardSelector) {
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
                    resetSelectionState()
                }
                showBoardSelector = false
                bottomSheetType = BottomSheetType.NONE
            }
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
                                },
                                onCancelClick = { resetSelectionState() },
                                onBack = {  // 선택모드 해제
                                    isSelectionMode = false
                                    selectedCards = emptySet()

                                    // 메뉴 바텀바로 이동
                                    bottomSheetType = BottomSheetType.MENU },
                                // 메뉴에서 들어왔을 때만 뒤로가기 버튼 표시
                                showBackButton = selectionFromMenu
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
                BottomSheetType.SECTION_RENAME -> {
                    ui.board?.let { currentBoard ->
                        // 새로 추가된 섹션 설정 바텀 시트
                        SectionRename(
                            modifier = Modifier.imePadding(),
                            currentName = currentBoard.name,
                            isCurrentlyShared = currentBoard.shared,
                            onDismiss = {
                                // 바텀시트 닫기
                                bottomSheetType = BottomSheetType.NONE
                            },
                            onBack = { bottomSheetType = BottomSheetType.MENU },
                            onConfirm = { newName, isShared ->
                                // 이름이 변경되었을 때만 API 호출
                                if (newName != currentBoard.name) {
                                    viewModel.renameCurrentBoard(newName)
                                }

                                // 완료 후 바텀시트 전체 닫기
                                bottomSheetType = BottomSheetType.NONE
                            }
                        )
                    }
                }
                //메뉴 버튼 클릭 시 메뉴 바텀바 표시
                BottomSheetType.MENU -> {
                    MenuContent(
                        isSectionScreen = true,
                        source = null,
                        isShared = false,
                        isOwner = ui.board?.owner ?: false,
                        isMine = ui.board?.mine ?:false,
                        onRenameClick = {
                            bottomSheetType = BottomSheetType.SECTION_RENAME
                        },
                        onMembersClick = { /* 섹션에는 참여자 없음 → 비활성 처리 */ },
                        onAddSectionClick = { /* 섹션에서 섹션 추가는 없음 → 무효 */ },
                        onSelectCardClick = {
                            isSelectionMode = true
                            bottomSheetType = BottomSheetType.SELECTION
                            selectionFromMenu = true
                        },
                        onSelectSectionClick = { /* 섹션 상세에서 섹션 선택 없음 */ },
                        onDismiss = { bottomSheetType = BottomSheetType.NONE }
                    )
                }
                else -> {}
            }
        }
    }

    // --- 다이얼로그의 삭제/제거 버튼에 올바른 ViewModel 함수 연결 ---
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            visible = showDeleteDialog,
            selectedCardCount = selectedCards.size,
            selectedSectionCount = 0,
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                scope.launch {
                    viewModel.deleteItems(emptySet(), selectedCards)
                    showDeleteDialog = false
                }
            }
        )
    }
    if (showSelectWarning) {
        when (selectWarningType) {
            SelectWarningType.CARD -> {
                SheetTopToast(
                    title = buildAnnotatedString {
                        append("카드 삭제ㆍ복제ㆍ이동은 ")
                        withStyle(SpanStyle(color = PurpleMain500)) { append("생성자만 ") }
                        append("가능해요.")
                    },
                    message = "다른 참여자가 생성한 카드는 선택할 수 없어요.",
                    visible = showSelectWarning,
                    onDismiss = { showSelectWarning = false },
                    durationMillis = 3500L,
                    bottomOffset = toastOffsetDp
                )
            }
            SelectWarningType.SECTION -> {}
            SelectWarningType.SECTIONCARD -> {}
            SelectWarningType.CARDSECTION -> {}
        }
    }
}

// 섹션 상세 화면 탑바
@Composable
fun SectionDetailTopBar(
    title: String,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    isSelectionMode: Boolean
) {
    val decodedTitle = try {
        // URL 디코딩
        java.net.URLDecoder.decode(title, "utf-8")
    } catch (e: Exception) {
        title // 디코딩 실패 시 원본 사용
    }
    // 6자가 넘으면 말줄임표 처리
    val displayTitle = if (decodedTitle.length > 10) {
        "${decodedTitle.take(10)}..."
    } else {
        decodedTitle
    }
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
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = displayTitle, // 6자 이상 시 말줄임표 적용된 텍스트
                style = subtitle_medium_16,
                color = GreyMain300
            )
        }

        // 중간을 채우는 빈 공간
        Spacer(modifier = Modifier.weight(1f))

        // 오른쪽 (설정 버튼)
        Icon(
            painter = painterResource(id = R.drawable.ic_board_menu),
            contentDescription = "보드 메뉴",
            tint = GreyMain300,
            // 선택 모드일 때 비활성화하고, 투명도를 조절합니다.
            modifier = Modifier
                .noRippleClickable(enabled = !isSelectionMode) { onMenuClick() }
        )
    }
}

// 필터 버튼
@Composable
fun SectionFilterButton(
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
            .padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 정렬/필터 버튼들
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 정렬 버튼
            SortFilterButton(
                selectedTab = 0,
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

// CardItemDto를 MyCardItem으로 변환하는 확장 함수
fun CardItemDto.toMyCardItem(): MyCardItem {
    return MyCardItem(
        id = this.id,
        imageUrl = this.imageUrl ?: "",
        isFavorite = this.favorite ?: false
    )
}
