package com.example.nubo.ui.screen.myBoard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.components.toast.AppToastHost
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.R
import com.example.nubo.data.model.CardItemDto
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.component.cardHeightForIndex
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.AppTextStyles.subtitle_medium_16
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.RedError
import com.example.nubo.utils.REFRESH_TICK_KEY
import kotlinx.coroutines.launch
import com.example.nubo.utils.postRefreshTick

@Composable
fun SectionDetailScreen(
    sectionId: Int,
    sectionTitle: String,
    navController: NavController,
    boardTitle: String,
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

    // 뒤로가기 버튼으로 선택 모드를 종료할 수 있도록 핸들러 추가
    BackHandler(enabled = isSelectionMode) {
        resetSelectionState()
    }

    // 삭제 다이얼로그
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 토스트 상태 및 코루틴 스코프 선언
    val toastHostState = rememberAppToastHostState()
    val scope = rememberCoroutineScope()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val toastEvent by viewModel.toastEvent.collectAsState()

    // 보드 상세 화면과 토스트 분기 처리
    LaunchedEffect(toastEvent) {
        toastEvent?.let { (message, source) ->
            if (source == "section") {   // 섹션 관련 메시지만 표시
                scope.launch {
                    toastHostState.show(
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
                toastHostState.show(
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
                toastHostState.show(
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                },// 메뉴 버튼 클릭 시 보드 설정 바텀 시트 표시
                    onMenuClick = { bottomSheetType = BottomSheetType.SECTION_SETTINGS},
                    isSelectionMode = isSelectionMode
                )
                // 패딩 조절된 TitleBar 사용
                BoardTitleBar(
                    title = detailState?.name ?: sectionTitle,)

                SectionFilterButton(
                    favoriteSelected = ui.favoriteOnly,
                    onToggleFavorite = { enabled -> viewModel.setFavoriteFilter(enabled) },
                    onRequestSort = { sortKey -> viewModel.setSort(sortKey) },
                    isSelectionMode = isSelectionMode
                )

                if (ui.isLoading && detailState == null) {
                    // 로딩 인디케이터를 가운데 정렬하기 위해 Box 사용
                    Box(
                        modifier = Modifier.fillMaxSize(), // 1. 남은 공간을 모두 채움
                        contentAlignment = Alignment.Center // 2. 자식을 가운데 정렬
                    ) {
                        CircularProgressIndicator() // 3. 로딩 인디케이터
                    }
                } else if (detailState != null) {
                    val cardItems = detailState.cards.content.map { it.toMyCardItem() }
                    val cardHeights by remember(sectionId, cardItems.size) {
                        mutableStateOf(
                            cardItems.mapIndexed { index, _ ->
                                cardHeightForIndex(index)
                            }
                        )
                    }
                    // 섹션에 카드가 아무것도 없을 때 빈 상태 UI 표시
                    if (cardItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(), // 스크린 전체 사용
                            contentAlignment = Alignment.Center // 가운데 정렬
                        ) {
                            Text(
                                text = "이 섹션에는 아직 저장된 카드가 없어요!",
                                style = AppTextStyles.b2_medium_16,
                                color = GreyMain300,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            )
                        }
                    } else {
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
                            onCardLongClick = { cardId ->
                                // 롱클릭 시 선택 모드로 진입하고, 현재 카드 선택
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    bottomSheetType = BottomSheetType.SELECTION
                                    selectedCards = setOf(cardId) // 새 Set으로 첫 항목 선택
                                }
                            },
                            isSelectionMode = isSelectionMode,
                            selectedCardIds = selectedCards,
                            onLoadMore = { viewModel.loadNextPage() },
                            isLoading = ui.isLoading,
                            isLastPage = ui.isLast

                        )
                    }
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
                                },
                                onCancelClick = { resetSelectionState() }
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
                    ui.board?.let { currentBoard ->
                        // 새로 추가된 섹션 설정 바텀 시트
                        SectionSettingsContent(
                            modifier = Modifier.imePadding(),
                            currentName = currentBoard.name,
                            isCurrentlyShared = currentBoard.shared,
                            onDismiss = {
                                // 바텀시트 닫기
                                bottomSheetType = BottomSheetType.NONE
                            },
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
    // 토스트 UI를 화면에 배치
    AppToastHost(hostState = toastHostState,
        modifier = Modifier.padding(bottom = 40.dp))
}

/**
 * 섹션 상세 화면 전용 상단바.
 * 이전 화면(보드)의 타이틀을 받아와 표시합니다.
 */
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
            .padding(start = 14.dp, end = 16.dp, top = 30.dp, bottom = 10.dp),
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
            painter = painterResource(id = R.drawable.ic_board_setting),
            contentDescription = "보드 설정",
            tint = GreyMain300,
            // 선택 모드일 때 비활성화하고, 투명도를 조절합니다.
            modifier = Modifier
                .size(20.dp)
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
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
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
                        painter = painterResource(if(isFavoriteSelected) R.drawable.selected_star else R.drawable.ic_filter_star),
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

