package com.example.nubo.ui.screen.myBoard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.nubo.ui.theme.AppTextStyles.label_medium_12
import com.example.nubo.ui.theme.AppTextStyles.headline_regular_26
import com.example.nubo.ui.theme.AppTextStyles.subtitle_medium_16
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple200
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.model.card.CardItem
import com.example.nubo.ui.component.randomCardHeight
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b3_medium_14
import getDisplayDate
import java.net.URLDecoder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.Purple50

// 어떤 다이얼로그를 띄울지 구분하기 위한 sealed class
sealed class InputDialogMode {
    data object CreateSection : InputDialogMode()
    data class Rename(val sectionId: Int, val currentName: String) : InputDialogMode()
}

@Composable
fun BoardDetailScreen(
    boardId: Int,
    boardTitle: String,
    navController: NavController,
    viewModel: BoardDetailViewModel = hiltViewModel(),
    myCardViewModel: MyCardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {

    //  다이얼로그 모드 상태
    var dialogMode by remember { mutableStateOf<InputDialogMode?>(null) }

    // 진입 시 한 번 초기 로드
    LaunchedEffect(boardId) {
        viewModel.init(boardId)
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

    // 뷰모델 상태 올바르게 구독
    val ui by viewModel.ui.collectAsState()
    val boardState = ui.board


    Column(modifier = Modifier.fillMaxSize()) {
        DetailTopBar(onBack = {
            // 현재 보드의 최신 이름 전달
            val latestName = ui.board?.name ?: boardTitle
            navController.previousBackStackEntry?.savedStateHandle?.set("renamed_board_id", boardId)
            navController.previousBackStackEntry?.savedStateHandle?.set("renamed_board_name", latestName)
            navController.popBackStack()
        })
        BoardTitleBar(title = ui.board?.name ?: boardTitle)
        // 즐겨찾기 필터만 뷰모델과 연결 (정렬 버튼은 UI만 유지, 서버 쿼리는 LATEST 고정)
        BoardFilterButton(
            favoriteSelected = ui.favoriteOnly,
            onToggleFavorite = { enabled -> viewModel.setFavoriteFilter(enabled) },
            onAddClick = { dialogMode = InputDialogMode.CreateSection },
            onSelectClick = {
                dialogMode = InputDialogMode.Rename(
                    sectionId = boardId, // 현재 보드 id
                    currentName = ui.board?.name ?: boardTitle
                )
            }
        )

        if (boardState != null) {
            val boardItems = boardState?.sections?.map { it.toBoardItem() } ?: emptyList()
            // 페이징 래퍼에서 실제 리스트 꺼내기
            val cardItems = boardState.cards.content.map { it.toCardItem() }

            // 카드 배열 길이가 바뀌면 높이도 재생성
            val cardHeights by remember(boardId, cardItems.size) {
                mutableStateOf(cardItems.map { randomCardHeight() })
            }


            if (boardItems.isNotEmpty()) {
                BoardDetailContent(
                    boardItems = boardItems,
                    cardItems = cardItems,
                    cardHeights = cardHeights,
                    onCardClick = { cardId ->
                        // MyBoardScreen과 동일한 패턴으로 카드 상세 화면 이동
                        navController.navigate("card_detail/$cardId")
                    },onSectionClick = { section ->
                        val encodedTitle = java.net.URLEncoder.encode(section.title, "utf-8")
                        navController.navigate("section_detail/${section.id}/$encodedTitle")
                    },
                    onFavoriteClick = { section: BoardItem ->
                        viewModel.toggleSectionFavorite(
                            sectionId = section.id,
                            currentFavorite = section.isBookmarked
                        )
                    }
                )
            } else {
                // 보드가 없고 카드만 있을 경우에도 동일하게 처리
                BoardDetailContent(
                    boardItems = boardItems,
                    cardItems = cardItems,
                    cardHeights = cardHeights,
                    onCardClick = { cardId ->
                        // MyBoardScreen과 동일한 패턴으로 카드 상세 화면 이동
                        navController.navigate("card_detail/$cardId")
                    },
                    onSectionClick = {/*카드만 있을 경우 섹션 동작 없음*/},
                    onFavoriteClick = { /* 섹션이 없을 때는 동작 없음 */ }
                )
            }
        } else {
            Text("Loading...")
        }
        // ==== 다이얼로그 표시 ====
        when (val m = dialogMode) {
            InputDialogMode.CreateSection -> NuboInputDialog(
                visible = true,
                title = "섹션 추가하기",
                confirmText = "생성",
                placeholder = "섹션 이름",
                onConfirm = { name -> viewModel.createSection(name) },
                onDismiss = { dialogMode = null }
            )

            is InputDialogMode.Rename -> NuboInputDialog(
                visible = true,
                title = "이름 변경",
                confirmText = "완료",
                placeholder = "새 이름",
                initialValue = m.currentName,
                onConfirm = { newName ->
                    // 보드 ID와 다이얼로그의 ID를 비교하여 올바른 함수를 호출합니다.
                    if (m.sectionId == boardId) {
                        // ID가 현재 보드 ID와 같으면 보드 이름 변경 함수를 호출합니다.
                        viewModel.renameCurrentBoard(newName = newName)
                    } else {
                        // 다르다면 섹션 이름 변경 함수를 호출합니다.
                        viewModel.renameSection(sectionId = m.sectionId, newName = newName)
                    }
                },
                onDismiss = { dialogMode = null }
            )

            null -> Unit
        }
    }
}


@Composable
fun DetailTopBar(onBack: () -> Unit) {
    val titleText = "나의 보드"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 65.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back), // ← ← 아이콘 파일 필요
            contentDescription = "뒤로가기",
            tint = GreyMain300,
            modifier = Modifier
                .clickable { onBack() }
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = titleText,
            style = subtitle_medium_16,
            color = GreyMain300
        )
    }
}


@Composable
fun BoardTitleBar(title: String) {
    val decodedTitle = URLDecoder.decode(title, "utf-8")

    Column(modifier = Modifier.padding(top = 27.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 16.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = decodedTitle,
                style = headline_regular_26,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BoardFilterButton(
    favoriteSelected: Boolean,
    onToggleFavorite: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onSelectClick: () -> Unit,
) {
    val filters = listOf("최근 저장순", "즐겨찾기")
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
            filters.forEach { label ->
                val isSelected = selected == label
                OutlinedButton(
                    onClick = {
                        when (label) {
                            "즐겨찾기" -> {
                                val nextOn = !isSelected
                                selected = if (nextOn) "즐겨찾기" else null
                                onToggleFavorite(nextOn) // 서버 필터 동기화
                            }
                            "최근 저장순" -> {
                                // 정렬은 UI만 표시 (서버 쿼리 LATEST 고정이면 추가 로직 불필요)
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
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = label,
                            style = label_medium_12,
                            color = if (isSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                        )
                        when (label) {
                            "최근 저장순" -> {
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter_arrow_down),
                                    contentDescription = "정렬 옵션",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            "즐겨찾기" -> {
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter_star),
                                    contentDescription = "즐겨찾기",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 오른쪽 버튼들(기존 그대로)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple100.copy(alpha = 0.3f),
                    contentColor = PurpleMain500
                ),
                border = BorderStroke(0.5.dp, PurpleMain500),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter_add),
                    contentDescription = "섹션 추가",
                    tint = PurpleMain500
                )
            }

            Button(
                onClick = onSelectClick,
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple100.copy(alpha = 0.3f),
                    contentColor = PurpleMain500
                ),
                border = BorderStroke(0.5.dp, PurpleMain500),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "선택", style = label_medium_12, color = PurpleMain500)
            }
        }
    }
}


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


fun CardItemDto.toCardItem(): CardItem {
    return CardItem(
        id = this.id,
        height = randomCardHeight(), // 기존 randomCardHeight() 함수 사용
        title = this.title ?: "No Title", // 서버 데이터 없을 경우 기본값
        category = this.category ?: "No Category", // 마찬가지
        description = this.description ?: "No Description",
        imageUrl = this.imageUrl ?: ""
    )
}

// 섹션 이름 입력 다이얼로그
@Composable
fun NuboInputDialog(
    // 다이얼로그 표시 여부
    visible: Boolean,
    // 상단 가운데 타이틀
    title: String,
    // 우측 텍스트 버튼 라벨
    confirmText: String,
    // 입력창 플레이스홀더
    placeholder: String,
    // 최초 값 (이름 변경 시 기존 이름)
    initialValue: String = "",
    // 확인 클릭 시 콜백 (서버 연동은 추후 이곳에서)
    onConfirm: (String) -> Unit,
    // X 또는 백드롭 클릭 시 닫기
    onDismiss: () -> Unit,
) {
    if (!visible) return

    // Compose Dialog는 배경을 자동으로 어둡게 처리함
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        // 카드 형태의 컨테이너
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            horizontalAlignment = Alignment.Start
        ) {
            // 내부 상태 보관
            var text by remember { mutableStateOf(initialValue) }
            val confirmEnabled = text.isNotBlank()

            // 헤더 영역: X 버튼 + 타이틀 + 우측 확인 텍스트 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close), // X 아이콘 필요
                    contentDescription = "닫기",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() } // 왼쪽 X 클릭 시 닫기
                )

                // 가운데 타이틀
                Text(
                    text = title,
                    style = b1_semibold_18,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier
                        .weight(1f),
                    textAlign = TextAlign.Center
                )

                // 우측 텍스트 버튼
                Text(
                    text = confirmText,
                    style = b1_semibold_18,
                    color = if (confirmEnabled) PurpleMain500 else Grey500,
                    modifier = Modifier
                        .clickable(enabled = confirmEnabled) {
                            onConfirm(text.trim())
                            onDismiss()
                        }
                        .padding(end = 4.dp)
                )
            }

            Spacer(Modifier.height(30.dp))

            // ▶ 입력 필드 바깥 여백 16dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)   // 컨테이너와의 가로 여백
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = b3_medium_14.copy(color = Grey1000),
                    cursorBrush = SolidColor(PurpleMain500),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (confirmEnabled) {
                                onConfirm(text.trim())
                                onDismiss()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFFF3F3F3), RoundedCornerShape(40.dp))
                        .border(1.dp, Color(0xFFBCBCBC), RoundedCornerShape(40.dp))
                        .padding(horizontal = 16.dp),             // 입력 내부 좌우 여백
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isBlank()) {
                                Text(
                                    text = placeholder,
                                    style = b3_medium_14,
                                    color = Color(0xFFBDBDBD)
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
