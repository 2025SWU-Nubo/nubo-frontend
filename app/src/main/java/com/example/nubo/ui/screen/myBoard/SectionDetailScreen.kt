package com.example.nubo.ui.screen.myBoard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.nubo.model.myBoard.MyCardItem
import com.example.nubo.ui.component.randomCardHeight
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun SectionDetailScreen(
    sectionId: Int,
    sectionTitle: String,
    navController: NavController,
    // BoardDetailViewModel을 재사용
    viewModel: BoardDetailViewModel = hiltViewModel(),
) {
    var dialogMode by remember { mutableStateOf < InputDialogMode ? > (null) }

    // [변경] viewModel.init() 함수에 sectionId를 전달
    LaunchedEffect(sectionId) {
        viewModel.init(sectionId)
    }

    // [변경] ui.board 상태를 사용하여 섹션 이름과 카드 목록을 표시
    val ui by viewModel.ui.collectAsState()
    val detailState = ui.board

    Column(modifier = Modifier.fillMaxSize()) {
        // DetailTopBar는 BoardDetailScreen의 것을 재사용
        DetailTopBar(onBack = {
            // 현재 섹션의 최신 이름 전달
            val latestName = ui.board?.name ?: sectionTitle
            // 결과를 이전 화면(BoardDetailScreen)으로 전달
            navController.previousBackStackEntry?.savedStateHandle?.set(
                "renamed_section_id",
                sectionId
            )
            navController.previousBackStackEntry?.savedStateHandle?.set(
                "renamed_section_name",
                latestName
            )
            navController.popBackStack()
        })
        // BoardTitleBar는 BoardDetailScreen의 것을 재사용
        BoardTitleBar(
            title = detailState?.name ?: sectionTitle,
            onClick = {
                dialogMode = InputDialogMode.Rename(
                    sectionId = sectionId,
                    currentName = detailState?.name ?: sectionTitle
                )
            })

        // '+' 버튼이 없는 필터 버튼 UI
        SectionFilterButton(
            favoriteSelected = ui.favoriteOnly,
            onToggleFavorite = { enabled -> viewModel.setFavoriteFilter(enabled) },
            onSelectClick = {},
            onRequestSort = {sortKey -> viewModel.setSort(sortKey)}
        )

        if (ui.isLoading && detailState == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...")
            }
        } else if (detailState != null) {
            // API 응답에서 카드 목록만 추출하여 표시
            val cardItems = detailState.cards.content.map { it.toMyCardItem() } // [변경] toMyCardItem() 호출
            val cardHeights by remember(sectionId, cardItems.size) {
                mutableStateOf(cardItems.map { randomCardHeight() })
            }

            // MyBoardScreen의 ScrollableCardContent를 재사용
            ScrollableCardContent(
                cards = cardItems,
                cardHeights = cardHeights,
                onCardClick = { cardId ->
                    navController.navigate("card_detail/$cardId")
                }
            )
        }

        // 이름 변경 다이얼로그
        when (val m = dialogMode) {
            is InputDialogMode.Rename -> NuboInputDialog(
                visible = true,
                title = "섹션 이름 변경",
                confirmText = "완료",
                placeholder = "새 이름",
                initialValue = m.currentName,
                // [변경] 이름 변경 시 viewModel.renameCurrentBoard(newName)를 호출합니다.
                onConfirm = { newName -> viewModel.renameCurrentBoard(newName = newName) },
                onDismiss = { dialogMode = null }
            )

            else -> Unit // 섹션 생성 다이얼로그는 없음
        }
    }
}


// '+' 버튼이 없는 버전의 필터 버튼 (BoardDetailScreen.kt에서 복사하여 수정)
@Composable
fun SectionFilterButton(
    favoriteSelected: Boolean,
    onToggleFavorite: (Boolean) -> Unit,
    onSelectClick: () -> Unit,
    onRequestSort: (String) -> Unit
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
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFavoriteSelected) Purple50 else Color.Transparent,
                    contentColor = if (isFavoriteSelected) PurpleMain500 else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (isFavoriteSelected) PurpleMain500 else Grey200),
                modifier = Modifier.height(35.dp),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "즐겨찾기", style = AppTextStyles.label_medium_12)
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_star),
                        contentDescription = "즐겨찾기",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // 오른쪽: '선택' 버튼만 있음
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
            Text(text = "선택", style = AppTextStyles.label_medium_12, color = PurpleMain500)
        }
    }
}

// CardItemDto를 MyCardItem으로 변환하는 확장 함수
fun CardItemDto.toMyCardItem(): MyCardItem {
    return MyCardItem(
        id = this.id,
        imageUrl = this.imageUrl ?: ""
    )
}
