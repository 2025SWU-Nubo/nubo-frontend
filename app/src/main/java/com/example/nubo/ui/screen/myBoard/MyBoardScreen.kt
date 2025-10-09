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
import androidx.compose.foundation.clickable
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
import com.example.nubo.ui.component.randomCardHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun MyBoardScreen(
    navController: NavController,
    boardViewModel: BoardViewModel = hiltViewModel(),
    cardViewModel: MyCardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(1) }

    // 검색 결과가 바뀔 때도 높이를 다시 계산하도록 키 추가
    val randomHeights = remember(selectedTab, cardViewModel.cards.value, cardViewModel.searchResults.value) {
        (if (selectedTab == 0) cardViewModel.searchResults.value else cardViewModel.cards.value).map { randomCardHeight() }
    }

    // 아이콘 리소스를 변수로 정의합니다.
    val noResultsIcon = R.drawable.error_face
    // 검색 중 아이콘
    val searchingIcon = R.drawable.ic_board_search // 임시로 검색 아이콘 사용

    // 검색 기능 변수들
    // MyBoardScreen() 내부 remember
    var isSearchMode by remember { mutableStateOf(false) } // 검색 모드 상태
    var searchText by remember { mutableStateOf("") }        // 검색어
    var hasSearched by remember { mutableStateOf(false) } // 검색 했는지 확인

    // 뷰모델에서 검색 결과 가져오기
    // [변경] 보드 검색 결과와 카드 검색 결과를 명확히 분리
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

    // BoardDetailScreen에서 이름 변경 결과를 수신하는 부분
    LaunchedEffect(navController.currentBackStackEntry) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        val id = handle?.get<Int>("renamed_board_id")
        val name = handle?.get<String>("renamed_board_name")

        // ID와 이름이 모두 정상적으로 전달되었다면
        if (id != null && name != null) {
            // 1. ViewModel의 함수를 호출하여 보드 목록의 데이터를 업데이트
            boardViewModel.applyRename(id, name)

            // 2. 한 번 사용한 데이터는 핸들에서 제거하여, 화면이 다시 그려질 때
            //    중복으로 적용되는 것을 방지
            handle.remove<Int>("renamed_board_id")
            handle.remove<String>("renamed_board_name")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
    ) {
        TabHeader(
            selectedTabIndex = selectedTab,
            onTabSelected = { newTab ->
                // 탭 변경 시 검색모드라면 닫기
                if (isSearchMode) {
                    closeSearchMode()
                }
                selectedTab = newTab
            }
        )
        TitleBar(
            selectedTab = selectedTab,
            isSearchMode = isSearchMode,
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            onSearchOpen = { isSearchMode = true },
            onSearchClose = { closeSearchMode() }, // 검색 닫는 공통 로직 추가
            onSearchSubmit = {
                focusManager.clearFocus()
                keyboard?.hide()
                val q = searchText.trim()
                if (q.isNotBlank()) {
                    hasSearched = true
                    // 선택된 탭에 따라 다른 ViewModel의 함수 호출
                    when (selectedTab) {
                        0 -> cardViewModel.searchCards(q) // 카드 탭
                        1 -> boardViewModel.searchBoards(q) // 보드 탭
                    }
                }
            },
            focusRequester = focusRequester
        )
        FilterButtons(
            onRequestFilter = { f ->
                if (selectedTab == 1) boardViewModel.setFilter(f)   // 보드 탭
                else cardViewModel.setFilter(f)       // 카드 탭
            },
            onRequestSort = { s ->
                if (selectedTab == 1) boardViewModel.setSort(s)
                else cardViewModel.setSort(s)
            }
        )
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> { // 카드 탭 UI 로직
                    val isSearching by cardViewModel.isSearching

                    if (isSearchMode) {
                        // --- 검색 모드일 때의 UI ---
                        if (isSearching) {
                            // 1. API 통신 중일 때 (로딩)
                            EmptyStateUI(iconRes = searchingIcon, message = "검색중..")
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
                                        cardHeights = randomHeights,
                                        onCardClick = { id ->
                                            navController.navigate("card_detail/$id")
                                        }
                                    )
                                }
                            }
                            // 'else'가 없음: 검색을 아직 실행 안 했다면 아무것도 표시하지 않음 (빈 화면)
                        }
                    } else {
                        // --- 검색 모드가 아닐 때의 UI (기본 목록) ---
                        ScrollableCardContent(
                            cards = cardViewModel.cards.value,
                            cardHeights = randomHeights,
                            onCardClick = { //없음
                            }
                        )
                    }
                }

                1 -> { // 보드 탭 UI 로직
                    val isSearching by boardViewModel.isSearching

                    if (isSearchMode) {
                        // --- 검색 모드일 때의 UI ---
                        if (isSearching) {
                            EmptyStateUI(iconRes = searchingIcon, message = "검색중..")
                        } else {
                            if (hasSearched) {
                                if (boardSearchResults.isEmpty()) {
                                    EmptyStateUI(iconRes = noResultsIcon, message = "검색결과가 없습니다.")
                                } else {
                                    // [수정] 검색 결과 목록에 클릭 로직 다시 추가
                                    BoardContent(
                                        boards = boardSearchResults,
                                        onCardClick = { boardItem ->
                                            navController.navigate(
                                                "board_detail/${boardItem.serverBoardId}/${
                                                    java.net.URLEncoder.encode(boardItem.title, "utf-8")
                                                }"
                                            )
                                        },
                                        onFavoriteClick = { item ->
                                            boardViewModel.toggleFavorite(
                                                boardId = item.serverBoardId,
                                                currentFavorite = item.isBookmarked
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // --- 검색 모드가 아닐 때의 UI (기본 목록) ---
                        val defaultBoards = boardViewModel.boards.value.filter {
                            val parts = it.subtitle.split(" ")
                            val cardCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
                            val hasCards = parts.getOrNull(3)?.contains("카드") == true && cardCount > 0

                            // 카드가 있거나, 사용자 보드면 표시
                            hasCards || it.source.equals("USER", ignoreCase = true)
                        }
                        // 기본 목록에 클릭 로직 다시 추가
                        BoardContent(
                            boards = defaultBoards,
                            onCardClick = { boardItem ->
                                navController.navigate(
                                    "board_detail/${boardItem.serverBoardId}/${
                                        java.net.URLEncoder.encode(boardItem.title, "utf-8")
                                    }"
                                )
                            },
                            onFavoriteClick = { item ->
                                boardViewModel.toggleFavorite(
                                    boardId = item.serverBoardId,
                                    currentFavorite = item.isBookmarked
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// 상단 카드 / 보드 탭바
@Composable
fun TabHeader(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("카드", "보드")

    Column(modifier = Modifier.padding(top = 35.dp)) {
        // 탭 전체 중앙 정렬
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(25.dp), // 탭 간 간격
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = index == selectedTabIndex
                    val textColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onTabSelected(index) } // 탭 클릭 처리
                    ) {
                        // 텍스트 설정
                        Text(
                            text = title,
                            style = b1_semibold_18,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(14.dp)) // 텍스트-인디케이터  간격

                        // 인디케이터 설정 (선택여부에 따라 색상 다르게)
                        Box(
                            modifier = Modifier
                                .width(83.dp)
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
    selectedTab: Int,
    isSearchMode: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchSubmit: () -> Unit,
    focusRequester: FocusRequester
) {
    val titleText = if (selectedTab == 0) "나의 카드" else "나의 보드"

    Column(modifier = Modifier.padding(top = 27.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSearchMode) {
                Text(text = titleText, style = AppTextStyles.headline_regular_26)
                Icon(
                    painter = painterResource(id = R.drawable.ic_board_search),
                    contentDescription = "검색",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onSearchOpen() }
                )
            } // 검색 모드
            else {
                // 탭별 플레이스홀더
                val placeholderText =
                    if (selectedTab == 0) "카드명 또는 키워드로 카드 검색"
                    else "보드명 또는 키워드로 보드 검색"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 입력 박스
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White)
                            .border(0.8.dp, Grey50, RoundedCornerShape(7.dp))
                            .focusRequester(focusRequester)
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 좌측 검색 아이콘 (선택)
                            Icon(
                                painter = painterResource(id = R.drawable.ic_board_search),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))

                            // 입력 필드
                            BasicTextField(
                                value = searchText,
                                onValueChange = onSearchTextChange,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = AppTextStyles.b3_medium_14.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                                //플레이스홀더
                                decorationBox = { inner ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchText.isEmpty()) {
                                            Text(
                                                text = placeholderText,        // ← 여기만 교체
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
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_board_close),
                        contentDescription = "닫기",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSearchClose() }
                    )
                }
            }
        }
    }
}


@Composable
fun FilterButtons(
    onRequestFilter: (String) -> Unit,
    onRequestSort: (String) -> Unit
) {
    val filters = listOf( "즐겨찾기", "공유됨")
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
    ) {
        // 정렬 버튼
        SortFilterButton(
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
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) Purple50 else Color.Transparent,
                    contentColor = if (isSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (isSelected) PurpleMain500 else Grey200),
                modifier = Modifier.height(35.dp),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = label,
                        style = AppTextStyles.label_medium_12,
                        color = if (isSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                    )
                    when (label) {
                        "즐겨찾기" -> {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_filter_star),
                                contentDescription = "즐겨찾기",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        "공유됨" -> {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                }
            }
        }
    }
}


// 나의 카드 탭 선택 시 -> 카드 콘텐츠 영역 스크롤 가능하도록
@Composable
fun ScrollableCardContent(
    cards: List<MyCardItem>,
    onCardClick: (Int) -> Unit,
    cardHeights: List<Dp>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        contentPadding = PaddingValues(bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            MyCardContent(
                cards = cards,
                cardHeights = cardHeights,
                onCardClick = onCardClick
            )
        }
    }
}

// 검색결과 없을 때 UI
@Composable
fun EmptyStateUI(modifier: Modifier = Modifier, iconRes: Int, message: String) {
    // 가운데 정렬된 심플한 빈 상태 UI
    Column(
        modifier = modifier
            .fillMaxSize() // 화면 전체를 차지하도록 변경
            .padding(horizontal = 24.dp, vertical = 40.dp),
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
    onSortSelected: (String) -> Unit
) {
    // 버튼에 표시될 텍스트와 팝업 표시 여부를 관리하는 내부 상태
    var currentSortText by remember { mutableStateOf("최근 저장순") }
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
            expanded = isPopupExpanded,
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
                DropdownMenuItem(
                    text = { Text(optionText, style = AppTextStyles.label_medium_12) },
                    onClick = {
                        currentSortText = optionText
                        isPopupExpanded = false
                        onSortSelected(sortOptions[optionText]!!)
                    },
                    //  현재 선택된 메뉴에 체크 아이콘 추가
                    trailingIcon = {
                        if (currentSortText == optionText) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_board_check_small), // 체크 아이콘 리소스
                                contentDescription = "Selected"
                            )
                        }
                    }
                )
            }
        }
    }
}
