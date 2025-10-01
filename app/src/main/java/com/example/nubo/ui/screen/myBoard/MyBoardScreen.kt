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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.example.nubo.ui.theme.Purple200
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
import com.example.nubo.model.myBoard.BoardItem
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.component.MyCardContent
import com.example.nubo.ui.component.randomCardHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun MyBoardScreen(
    navController: NavController,
    boardViewModel: BoardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(1) }

    val cardViewModel: MyCardViewModel = hiltViewModel()

    // 카드 / 보드 탭이 바뀔 때만 높이를 새로 생성
    val randomHeights = remember(selectedTab) {
        cardViewModel.cards.value.map { randomCardHeight() }
    }

    // 검색 기능 변수들
    // MyBoardScreen() 내부 remember
    var isSearchMode by remember { mutableStateOf(false) }      // 검색 모드 상태
    var searchText by remember { mutableStateOf("") }           // 검색어
    var searchResults by remember { mutableStateOf<List<BoardItem>?>(null) } // 검색 결과

    val focusRequester = remember { FocusRequester() }          // 포커스 요청자
    val focusManager = LocalFocusManager.current                // 포커스 매니저
    val keyboard = LocalSoftwareKeyboardController.current      // 키보드 컨트롤러

    // 검색 모드 진입 시 키보드 자동 표시
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            focusRequester.requestFocus()   // 검색창 포커스
            keyboard?.show()                // 키보드 올림
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
    ) {
        TabHeader(
            selectedTabIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )
        TitleBar(
            selectedTab = selectedTab,
            isSearchMode = isSearchMode,
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            onSearchOpen = { isSearchMode = true },
            onSearchClose = {
                isSearchMode = false
                searchText = ""
                searchResults = null
                focusManager.clearFocus()
                keyboard?.hide()
                // 필요 시 전체 재조회
                // boardViewModel.refresh()
            },
            onSearchSubmit = {
                focusManager.clearFocus()
                keyboard?.hide()
                val q = searchText.trim()
                searchResults =
                    if (q.isBlank()) null
                    else boardViewModel.boards.value.filter {
                        it.title.contains(q, true) || it.subtitle.contains(q, true)
                    }
                // 서버 연동 자리
            },
            focusRequester = focusRequester
        )
        FilterButtons(
            onRequestFilter = { f ->
                if (selectedTab == 1) boardViewModel.setFilter(f)   // 보드 탭
                else                cardViewModel.setFilter(f)       // 카드 탭
            },
            onRequestSort = { s ->
                if (selectedTab == 1) boardViewModel.setSort(s)
                else                cardViewModel.setSort(s)
            }
        )
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ScrollableCardContent(
                    cards = cardViewModel.cards.value,
                    cardHeights = randomHeights,
                    onCardClick = { id ->
                        // 카드 상세 화면으로 이동
                        navController.navigate("card_detail/$id")
                    })
                1 -> {
                    // 기본 리스트 계산
                    val defaultBoards = boardViewModel.boards.value.filter {
                        val parts = it.subtitle.split(" ")
                        val cardCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
                        val hasCards = parts.getOrNull(3)?.contains("카드") == true && cardCount > 0

                        // 카드가 있거나, 사용자 보드면 표시
                        hasCards || it.source.equals("USER", ignoreCase = true)
                    }

                    // 검색 결과가 있으면 그걸 사용, 없으면 기본 리스트
                    val visibleBoards = searchResults ?: defaultBoards

                    BoardContent(
                        boards = visibleBoards,
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

// 상단 카드 / 보드 탭바
@Composable
fun TabHeader(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("카드", "보드")

    Column(modifier = Modifier.padding(top = 30.dp)) {
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
    val filters = listOf("최근 저장순", "즐겨찾기", "공유됨")
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
    ) {
        filters.forEach { label ->
            val isSelected = selected == label
            OutlinedButton(
                onClick = {
                    when (label) {
                        "최근 저장순" -> onRequestSort("LATEST")
                        "즐겨찾기"   -> {
                            val target = if (isSelected) "ALL" else "FAVORITE"
                            selected = if (isSelected) null else "즐겨찾기"
                            onRequestFilter(target)
                        }
                        "공유됨"     -> {
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
                        "최근 저장순" -> {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_filter_arrow_down),
                                contentDescription = "정렬 옵션",
                                modifier = Modifier.size(22.dp)
                            )
                        }

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
