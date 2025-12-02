package com.example.nubo.ui.screen.myBoard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nubo.R
import com.example.nubo.ui.component.BoardContent
import com.example.nubo.ui.theme.Grey200
import androidx.navigation.NavController
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.component.MyCardContent
import com.example.nubo.ui.component.cardHeightForIndex
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.AnnotatedString
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.label_SemiBold_12
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MyBoardScreen(
    navController: NavController,
    boardViewModel: BoardViewModel = hiltViewModel(),
    cardViewModel: MyCardViewModel = hiltViewModel(),
    boardDetailViewModel: BoardDetailViewModel = hiltViewModel(),
    //탭 선택 상태 전달 받음
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // 카드 선택
    isCardSelectionMode: Boolean,
    selectedCardIds: Set<Int>,
    onCardClick: (Int) -> Unit,
    onCardLongClick: (Int) -> Unit,
    // 보드 선택
    isBoardSelectionMode: Boolean,
    selectedBoardIds: Set<Int>,
    onBoardClick: (com.example.nubo.model.myBoard.BoardItem) -> Unit,
    onBoardLongClick: (com.example.nubo.model.myBoard.BoardItem) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // 전역 토스트 사용
    val toastHostState = LocalAppToastHostState.current  //  전역 호스트
    val toastMessage by boardViewModel.toastMessage.collectAsState()

    // --- 삭제 완료 액션 토스트 구독 ---
    LaunchedEffect(boardViewModel) {
        boardViewModel.deleteToastEvent.collect { message ->
            scope.launch {
                toastHostState.show(
                    title = AnnotatedString(message),
                    layout = AppToastLayout.TitleWithAction,
                    type = AppToastType.NORMAL, // 삭제 완료는 Normal 타입
                    actionLabel = "실행 취소",
                    onAction = {
                        scope.launch {
                            boardViewModel.undoLastDeletion()
                        }
                    }
                )
            }
        }
    }

    // --- ViewModel의 toastMessage 변경을 감지하여 토스트 표시 ---
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->

            // 네트워크/보드 로딩 에러 전용 플래그
            val isBoardLoadError = (message == "정보를 불러오지 못했어요")

            // 메시지 내용에 따라 토스트 타입 결정
            val toastType = when {
                isBoardLoadError -> AppToastType.NEGATIVE
                message.contains("실패했어요.") -> AppToastType.NEGATIVE
                message.contains("취소되었어요.") -> AppToastType.POSITIVE
                message.contains("즐겨찾기가") -> AppToastType.FAVORITE
                else -> AppToastType.NORMAL
            }

            scope.launch {
                if (isBoardLoadError) {
                    // 보드 목록 불러오기 실패 시: 제목 + 요약 두 줄 토스트
                    toastHostState.show(
                        title = AnnotatedString("정보를 불러오지 못했어요"),
                        summary = "네트워크 확인 후 다시 시도해주세요.",
                        type = toastType, // NEGATIVE
                        layout = AppToastLayout.TitleWithSummary
                    )
                } else {
                    // 기존 한 줄 토스트
                    toastHostState.show(
                        title = buildAnnotatedString { append(message) },
                        layout = AppToastLayout.TitleOnly,
                        type = toastType
                    )
                }
            }

            // 한 번 표시한 뒤에는 다시 뜨지 않도록 메시지 초기화
            boardViewModel.clearToastMessage()
        }
    }

    // --- BoardDetailViewModel의 토스트 메시지 처리  ---
    val detailToastMessage by boardDetailViewModel.toastMessage.collectAsState()
    LaunchedEffect(detailToastMessage) {
        detailToastMessage?.let { message ->
            val toastType = when {
                message.contains("실패했어요.") -> AppToastType.NEGATIVE
                message.contains("완료되었어요.") || message.contains("취소되었어요.") -> AppToastType.POSITIVE
                else -> AppToastType.NORMAL
            }

            scope.launch {
                toastHostState.show(
                    title = buildAnnotatedString { append(message) },
                    layout = AppToastLayout.TitleOnly,
                    type = toastType
                )
            }
            if (message.contains("취소되었어요.")) {
                cardViewModel.refresh()
            }
            boardDetailViewModel.clearToastMessage()
        }
    }

    // 아이콘 리소스를 변수로 정의
    val noResultsIcon = R.drawable.error_face

    // 검색 기능 변수들
    // MyBoardScreen() 내부 remember
    var isSearchMode by remember { mutableStateOf(false) } // 검색 모드 상태
    var searchText by remember { mutableStateOf("") }        // 검색어
    var hasSearched by remember { mutableStateOf(false) } // 검색 했는지 확인

    // 뷰모델에서 검색 결과 가져오기
    // 보드 검색 결과와 카드 검색 결과를 명확히 분리
    val boardSearchResults by boardViewModel.searchResults
    val cardSearchResults by cardViewModel.searchResults

    val focusRequester = remember { FocusRequester() }          // 포커스 요청자
    val focusManager = LocalFocusManager.current                // 포커스 매니저
    val keyboard = LocalSoftwareKeyboardController.current      // 키보드 컨트롤러

    // 검색 모드 닫는 로직
    val closeSearchMode = {
        isSearchMode = false
        searchText = ""
        hasSearched = false
        boardViewModel.clearSearch()
        cardViewModel.clearSearch() // cardViewModel 검색 결과도 초기화
        focusManager.clearFocus()
        keyboard?.hide()
    }

    // 검색 모드 진입 시 키보드 자동 표시
    LaunchedEffect(isSearchMode) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        if (isSearchMode) {
            focusRequester.requestFocus()   // 검색창 포커스
            keyboard?.show()                // 키보드 올림
        }
    }

    LaunchedEffect(searchText, selectedTab, isSearchMode) {
        if (!isSearchMode) return@LaunchedEffect // 검색 모드가 아닐 땐 동작 X
        val q = searchText.trim()

        // 빈 문자열이면 즉시 초기화(서버 호출 안 함)
        if (q.isEmpty()) {
            hasSearched = false
            boardViewModel.clearSearch()
            cardViewModel.clearSearch()
            return@LaunchedEffect
        }

        // 타이핑 멈춘 뒤 350ms 후 서버 호출
        delay(350)

        // 최소 1자 이상일 때만 검색(원하면 2자로 변경 가능)
        if (q.length >= 1) {
            hasSearched = true
            when (selectedTab) {
                0 -> cardViewModel.searchCards(q)   // 카드 탭
                1 -> boardViewModel.searchBoards(q) // 보드 탭
            }
        }
    }

    // 뒤 화면에서 변경 사항 적용 시 바로 적용
    val handle = navController.currentBackStackEntry?.savedStateHandle

    // Boolean 기본값 false
    val needsRefresh by (
        handle
            ?.getLiveData<Boolean>("needs_refresh")
            ?.observeAsState(initial = false)     // LiveData -> State
            ?: remember { mutableStateOf(false) } // default state, remembered
        )
    // Int? 기본값 null
    val renamedBoardId by (
        handle
            ?.getLiveData<Int>("renamed_board_id")
            ?.observeAsState()                    // State<Int?>
            ?: remember { mutableStateOf<Int?>(null) }
        )
    // String? 기본값 null
    val renamedBoardName by (
        handle
            ?.getLiveData<String>("renamed_board_name")
            ?.observeAsState()                    // State<String?>
            ?: remember { mutableStateOf<String?>(null) }
        )

    LaunchedEffect(needsRefresh) {
        if (needsRefresh == true) {
            boardViewModel.refresh()
            handle?.remove<Boolean>("needs_refresh")
        }
    }

    // BoardDetailScreen에서 이름 변경 결과를 수신하는 부분
    LaunchedEffect(renamedBoardId, renamedBoardName) {
        val id = renamedBoardId
        val name = renamedBoardName
        if (id != null && name != null) {
            boardViewModel.applyRename(id, name)
            handle?.remove<Int>("renamed_board_id")
            handle?.remove<String>("renamed_board_name")
        }
    }

    // 현재 화면에 표시될 카드 리스트를 결정
    val currentCardList = if (isSearchMode && hasSearched) cardSearchResults else cardViewModel.cards.value
    // 'currentCardList'가 변경될 때만 카드 높이를 다시 계산하도록 키를 수정합니다.
    val cardHeights = remember(currentCardList.size) {
        currentCardList.mapIndexed { index, _ ->
            cardHeightForIndex(index)
        }
    }

    // 필터 + 검색 헤더에서 사용
    val onRequestFilter: (String) -> Unit = { filter ->
        if (selectedTab == 1) boardViewModel.setFilter(filter)
        else cardViewModel.setFilter(filter)
    }

    val onRequestSort: (String) -> Unit = { sortKey ->
        if (selectedTab == 1) boardViewModel.setSort(sortKey)
        else cardViewModel.setSort(sortKey)
    }

    // 리스트 스크롤 상태
    val listState = rememberLazyListState()

    // 카드 페이징 상태 (무한 스크롤 조건 체크용)
    val cardIsLoading by cardViewModel.isLoading
    val cardIsLastPage by cardViewModel.isLast

    data class BoardScrollState(
        val canScroll: Boolean,
        val count: Int,
        val isLast: Boolean
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        //1) 탭헤더 (고정)
        TabHeader(
            selectedTabIndex = selectedTab,
            onTabSelected = { newTab ->
                // 탭 변경 시 검색모드라면 닫기
                if (isSearchMode) {
                    closeSearchMode()
                }
                // 부모(Route)가 전달해준 람다를 호출
                // (새로고침 로직은 MyBoardRoute로 이동됨)
                onTabSelected(newTab)
            },
            isSelectionMode = isCardSelectionMode || isBoardSelectionMode
        )

        // 2) 전체 스크롤은 LazyColumn 하나로 담당
        LazyColumn(
            state = listState, // 스크롤 상태 연결
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp) // 바텀네비 고려
        ) {
            // 상단 타이틀 - 검색 모드일 때 숨김
            if (!isSearchMode) {
                item {
                    TitleBar(
                        selectedTab = selectedTab,
                    )
                }
            }

            //  Sticky Header — 필터 + 검색 / 검색창
            stickyHeader {
                FilterSearchStickyHeader(
                    isSearchMode = isSearchMode,
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onSearchOpen = { isSearchMode = true },
                    onSearchClose = { closeSearchMode() },
                    selectedTab = selectedTab,
                    onRequestFilter = onRequestFilter,
                    onRequestSort = onRequestSort,
                    enabled = !isCardSelectionMode,
                    focusRequester = focusRequester
                )
            }

            // 3) 콘텐츠 영역
            item {
                when (selectedTab) {
                    0 -> { // 카드 탭 UI 로직
                        val isSearching by cardViewModel.isSearching

                        //  ViewModel에서 isLoading, isLast 상태 가져오기
                        val isLoading by cardViewModel.isLoading
                        val isLastPage by cardViewModel.isLast

                        if (isSearchMode) {
                            // --- 검색 모드일 때의 UI ---
                            if (isSearching) {
                                // 1. API 통신 중일 때 (로딩)
                                SearchingIndicator()
                            } else {
                                // 2. API 통신이 끝났을 때
                                if (hasSearched) {
                                    // 검색을 한 번이라도 실행했다면, 결과를 바탕으로 UI 표시
                                    if (cardSearchResults.isEmpty()) {
                                        // 결과가 없을 때
                                        EmptyStateUI(iconRes = noResultsIcon, message = "검색결과가 없습니다.")
                                    } else {
                                        // 결과가 있을 때
                                        ScrollableCardContent(
                                            cards = cardSearchResults,
                                            cardHeights = cardHeights,
                                            onCardClick = onCardClick, // 부모가 넘겨준 함수를 그대로 전달
                                            onCardLongClick = onCardLongClick, // 부모가 넘겨준 함수를 그대로 전달
                                            isSelectionMode = isCardSelectionMode,
                                            selectedCardIds = selectedCardIds,
                                            // 검색 상태는 무한 스크롤 미적용
                                            onLoadMore = { },
                                            isLoading = false,
                                            isLastPage = true
                                        )
                                    }
                                }
                                // 'else'가 없음: 검색을 아직 실행 안 했다면 아무것도 표시하지 않음 (빈 화면)
                            }
                        } else {
                            // --- 검색 모드가 아닐 때의 UI (기본 목록) ---
                            ScrollableCardContent(
                                cards = cardViewModel.cards.value,
                                cardHeights = cardHeights,
                                onCardClick = onCardClick, // 부모가 넘겨준 함수를 그대로 전달
                                onCardLongClick = onCardLongClick, // 부모가 넘겨준 함수를 그대로 전달
                                isSelectionMode = isCardSelectionMode,
                                selectedCardIds = selectedCardIds,
                                // 무한 스크롤 콜백 및 상태 전달
                                onLoadMore = { cardViewModel.loadMore() },
                                isLoading = isLoading,
                                isLastPage = isLastPage
                            )
                        }
                    }

                    1 -> { // 보드 탭 UI 로직
                        val isSearching by boardViewModel.isSearching

                        if (isSearchMode) {
                            // --- 검색 모드일 때의 UI ---
                            if (isSearching) {
                                SearchingIndicator()
                            } else {
                                if (hasSearched) {
                                    if (boardSearchResults.isEmpty()) {
                                        EmptyStateUI(iconRes = noResultsIcon, message = "검색결과가 없습니다.")
                                    } else {
                                        // --- BoardContent에 선택모드 관련 파라미터 전달 ---
                                        BoardContent(
                                            boards = boardSearchResults,
                                            onBoardClick = onBoardClick, // 클릭 이벤트 전달
                                            onBoardLongClick = onBoardLongClick, // 롱클릭 이벤트 전달
                                            onFavoriteClick = { item ->
                                                boardViewModel.toggleFavorite(
                                                    boardId = item.serverBoardId,
                                                    currentFavorite = item.isBookmarked
                                                )
                                            },
                                            isSelectionMode = isBoardSelectionMode, // 선택모드 상태 전달
                                            selectedBoardIds = selectedBoardIds // 선택된 ID 전달
                                        )
                                    }
                                }
                            }
                        } else {
                            // 기존 필터 로직을 제거하고, 서버에서 받은 모든 보드를 보여줌.
                            BoardContent(
                                boards = boardViewModel.boards.value, // <-- 필터 제거
                                onBoardClick = onBoardClick, // Route가 정의한 클릭 동작을 전달
                                onBoardLongClick = onBoardLongClick, // Route가 정의한 롱클릭 동작을 전달
                                onFavoriteClick = { item ->
                                    boardViewModel.toggleFavorite(
                                        boardId = item.serverBoardId,
                                        currentFavorite = item.isBookmarked
                                    )
                                },
                                isSelectionMode = isBoardSelectionMode,
                                selectedBoardIds = selectedBoardIds
                            )
                        }
                    }
                }
            }
        }
        // 카드 탭에서만 동작하는 수동 페이징 로직
        LaunchedEffect(listState, selectedTab, isSearchMode) {

            // 카드 탭이 아니거나 검색 모드일 때는 작동시키지 않음
            if (selectedTab != 0 || isSearchMode) return@LaunchedEffect

            snapshotFlow {
                // canScrollForward: 아래로 더 스크롤할 수 있는지 여부
                val canScroll = listState.canScrollForward

                // 현재 로드된 카드 개수
                val cardCount = cardViewModel.cards.value.size

                // 마지막 페이지인지 여부
                Triple(canScroll, cardCount, cardIsLastPage)
            }.collect { (canScroll, cardCount, isLastPage) ->

                // 다음 조건이 모두 충족될 때 loadMore()를 호출함
                //
                // 1) 화면을 아래로 더 스크롤할 수 없음
                //    → 카드가 화면에 다 들어왔거나, 화면 끝까지 도달한 상태
                //
                // 2) 현재 로딩 중이 아님
                //
                // 3) 아직 마지막 페이지가 아님
                //
                // 4) 카드가 최소 1개 이상 로드되어 있는 상황
                if (!canScroll &&
                    !cardIsLoading &&
                    !isLastPage &&
                    cardCount > 0
                ) {
                    // 다음 페이지 불러오기
                    cardViewModel.loadMore()
                }
            }
        }
        // 보드 탭에서만 동작하는 수동 페이징 로직
        LaunchedEffect(listState, selectedTab, isSearchMode) {

            // 보드 탭이 아닌 경우 또는 검색 모드일 때는 페이징 동작시키지 않음
            if (selectedTab != 1 || isSearchMode) return@LaunchedEffect

            snapshotFlow {
                BoardScrollState(
                    canScroll = listState.canScrollForward,
                    count = boardViewModel.boards.value.size,
                    isLast = boardViewModel.isLast.value
                )
            }.collect { state ->

                if (!state.canScroll &&
                    !boardViewModel.isLoading.value &&
                    !state.isLast &&
                    state.count > 0
                ) {
                    boardViewModel.loadMore()
                }
            }

        }
    }
}

// 상단 (카드 / 보드) 탭바
@Composable
fun TabHeader(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    isSelectionMode: Boolean
) {
    val tabs = listOf("보드", "카드")

    Column(modifier = Modifier.padding(top = 52.dp)) {
        // 탭 전체 중앙 정렬
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp), // 탭 간 간격
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { title ->
                    // 로직 매핑: "보드"는 실제 인덱스 1, "카드"는 실제 인덱스 0
                    // 화면 순서는 바뀌었지만, 데이터 로직(ViewModel)은 그대로 유지
                    val targetIndex = if (title == "보드") 1 else 0
                    val isSelected = selectedTabIndex == targetIndex

                    val textColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .noRippleClickable {
                                if (!isSelectionMode) onTabSelected(targetIndex)
                            }
                    ) {
                        // 텍스트 설정
                        Text(
                            text = title,
                            style = b1_semibold_18,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 인디케이터 설정
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(1.5.dp)
                                .background(textColor)
                        )
                    }
                }
            }
        }
    }
}

// 타이틀 & 검색
@Composable
fun TitleBar(
    selectedTab: Int
) {
    Text(
        text = if (selectedTab == 0) "나의 카드" else "나의 보드",
        style = AppTextStyles.title_semibold_24,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 18.dp, end = 18.dp, bottom = 8.dp)
    )
}

// 필터 +  검색 헤더
@Composable
fun FilterSearchStickyHeader(
    isSearchMode: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    selectedTab: Int,
    onRequestFilter: (String) -> Unit,
    onRequestSort: (String) -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester
) {
    var searchFocused by remember { mutableStateOf(false) }
    val placeholderText =
        if (selectedTab == 0) "카드명 또는 키워드로 카드 검색"
        else "보드명으로 보드 검색"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        color = Color.White
    ) {

        // ─────────────────────────────
        // ① 검색 모드가 아닐 때: 필터 + 검색 아이콘 같은 줄
        // ─────────────────────────────
        if (!isSearchMode) {
            // 불필요한 상위 Column 및 패딩 제거, Row로 바로 시작
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp), // 전체 헤더 패딩
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // 양끝 정렬
            ) {
                // 필터 버튼들 (왼쪽 정렬)
                FilterButtons(
                    selectedTab = selectedTab,
                    onRequestFilter = onRequestFilter,
                    onRequestSort = onRequestSort,
                    enabled = enabled
                )

                Spacer(modifier = Modifier.weight(1f))

                // 검색 아이콘 (오른쪽 끝)
                Icon(
                    painter = painterResource(id = R.drawable.ic_board_search),
                    contentDescription = "검색",
                    modifier = Modifier
                        .size(24.dp)
                        .noRippleClickable { if (enabled) onSearchOpen() }
                )
            }
        }
        // ─────────────────────────────
        // ② 검색 모드일 때: 검색창 한 줄
        // ─────────────────────────────
        else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 입력 박스
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White)
                        .border(
                            0.8.dp,
                            if (searchFocused) PurpleMain500 else Grey50,
                            RoundedCornerShape(7.dp)
                        )
                        .focusRequester(focusRequester)
                        .padding(horizontal = 12.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            painter = painterResource(id = R.drawable.ic_board_search),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))

                        BasicTextField(
                            value = searchText,
                            onValueChange = onSearchTextChange,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { searchFocused = it.isFocused },
                            singleLine = true,
                            textStyle = AppTextStyles.b3_medium_14.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            decorationBox = { inner ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            text = placeholderText,
                                            style = AppTextStyles.label_medium_12,
                                            color = Grey200
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 닫기 버튼
                Icon(
                    painter = painterResource(id = R.drawable.ic_board_close),
                    contentDescription = "닫기",
                    modifier = Modifier
                        .size(24.dp)
                        .noRippleClickable { onSearchClose() }
                )
            }
        }
    }
}

// 정렬, 필터 버튼
@Composable
fun FilterButtons(
    selectedTab: Int,
    onRequestFilter: (String) -> Unit,
    onRequestSort: (String) -> Unit,
    enabled: Boolean
) {
    val filters = listOf("즐겨찾기", "공유됨")

    // 탭이 바뀔 때마다 selected 를 새로 초기화
    var selected by remember(selectedTab) { mutableStateOf<String?>(null) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
    ) {
        // 정렬 버튼
        SortFilterButton(
            selectedTab = selectedTab,
            onSortSelected = { sortKey -> onRequestSort(sortKey) }
        )
        // 즐겨찾기, 공유됨 필터 버튼
        filters.forEach { label ->
            val isSelected = selected == label
            OutlinedButton(
                onClick = {
                    when (label) {
                        "즐겨찾기" -> {
                            val target = if (isSelected) "ALL" else "FAVORITE"
                            selected = if (isSelected) null else "즐겨찾기"
                            onRequestFilter(target)
                        }

                        "공유됨" -> {
                            val target = if (isSelected) "ALL" else "SHARED"
                            selected = if (isSelected) null else "공유됨"
                            onRequestFilter(target)
                        }
                    }
                },
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) Purple50 else Color.Transparent,
                    contentColor = if (isSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (isSelected) PurpleMain500 else Grey200),
                // --- '즐겨찾기' 버튼일 때 크기와 패딩을 다르게 적용 ---
                modifier = Modifier.height(35.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
            ) {

                // --- '즐겨찾기'일 때는 아이콘만, 아닐 때는 기존 UI를 보여주도록 분기 ---
                if (label == "즐겨찾기") {
                    Icon(
                        painter = painterResource(if (isSelected) R.drawable.selected_star else R.drawable.ic_filter_star),
                        contentDescription = "즐겨찾기",
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    // '공유됨' 버튼은 기존 UI 유지
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = label,
                            style = AppTextStyles.label_medium_12,
                            color = if (isSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(2.dp))

                    }
                }
            }
        }
    }
}

// 나의 카드 탭 : 카드 콘텐츠 스크롤 영역
@Composable
fun ScrollableCardContent(
    cards: List<MyCardItem>,
    onCardClick: (Int) -> Unit,
    cardHeights: List<Dp>,
    onCardLongClick: (Int) -> Unit,
    isSelectionMode: Boolean = false,
    selectedCardIds: Set<Int> = emptySet(),
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    isLastPage: Boolean,
) {

    // 이 함수는 스크롤러가 아니므로 조용히 UI만 구성해야 함
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // Masonry 카드 목록 바로 출력
        MyCardContent(
            cards = cards,
            cardHeights = cardHeights,
            onCardClick = onCardClick,
            onCardLongClick = onCardLongClick,
            isSelectionMode = isSelectionMode,
            selectedCardIds = selectedCardIds
        )

        // 로딩 인디케이터 (부모 LazyColumn에서 공간 확보됨)
        if (isLoading) {
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

// 검색 결과 없을 때 UI
@Composable
fun EmptyStateUI(modifier: Modifier = Modifier, iconRes: Int, message: String) {
    // 가운데 정렬된 심플한 빈 상태 UI
    Column(
        modifier = modifier
            .fillMaxSize() // 화면 전체를 차지하도록 변경
            .padding(horizontal = 24.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // 세로 방향에서도 중앙 정렬
    ) {
        // 이모지 영역
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = message, // 접근성을 위해 contentDescription에 메시지 사용
            modifier = Modifier.size(48.dp), // 아이콘 크기 지정
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(12.dp)) // 아이콘과 텍스트 간 간격

        // 제목
        Text(
            text = message,
            style = AppTextStyles.b2_bold_16,   // 가지고 계신 Text Style 사용
            color = Grey1000,
            textAlign = TextAlign.Center
        )
    }
}

// 공통 정렬 UI
@Composable
fun SortFilterButton(
    selectedTab: Int,
    enabled: Boolean = true,
    onSortSelected: (String) -> Unit
) {
    // 탭이 바뀔 때마다 기본값으로 리셋
    var currentSortText by remember(selectedTab) { mutableStateOf("최근 저장순") }
    var isPopupExpanded by remember { mutableStateOf(false) }

    // 화면에 표시될 텍스트와 서버에 보낼 값을 매핑
    val sortOptions = mapOf(
        "최근 저장순" to "LATEST",
        "오래된순" to "OLDEST",
        "가나다순" to "ALPHABET"
    )

    // Box를 사용해 버튼 위에 팝업 메뉴를 띄울 위치를 지정
    Box {
        OutlinedButton(
            enabled = enabled, //선택 모드일 때 정렬 숨기기 위한 enabled
            onClick = { isPopupExpanded = true }, // 버튼 클릭 시 팝업 펼치기
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, Grey200),
            modifier = Modifier.height(35.dp),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = currentSortText, // 현재 선택된 정렬 텍스트를 표시
                    style = AppTextStyles.label_medium_12,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter_arrow_down),
                    contentDescription = "정렬 옵션",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // DropdownMenu를 사용한 정렬 팝업 UI
        DropdownMenu(
            expanded = isPopupExpanded && enabled, // enabled가 true일 때만 메뉴가 보이도록
            onDismissRequest = { isPopupExpanded = false },
            modifier = Modifier
                // 그림자 적용
                .shadow(
                    elevation = 4.5.dp,
                    spotColor = Color(0x4D000000),
                    ambientColor = Color(0x4D000000)
                )
                // 배경 및 모양 적용
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(size = 8.dp)
                )
        ) {
            sortOptions.keys.forEach { optionText ->
                val isSelected = currentSortText == optionText
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionText,
                            style = if (isSelected) label_SemiBold_12 else AppTextStyles.label_medium_12,
                            color = if (isSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        currentSortText = optionText
                        isPopupExpanded = false
                        onSortSelected(sortOptions[optionText]!!)
                    }
                )
            }
        }
    }
}

// 검색 로딩 인디케이터
@Composable
private fun SearchingIndicator(
    message: String = "검색 중..."
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = AppTextStyles.b2_bold_16,
            color = Grey1000,
            textAlign = TextAlign.Center
        )
    }
}
